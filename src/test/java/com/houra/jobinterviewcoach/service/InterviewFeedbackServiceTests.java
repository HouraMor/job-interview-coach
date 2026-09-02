package com.houra.jobinterviewcoach.service;

import com.houra.jobinterviewcoach.model.FeedbackResult;
import com.houra.jobinterviewcoach.model.InterviewFeedbackResult;
import com.houra.jobinterviewcoach.persistence.entity.AnswerAttempt;
import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import com.houra.jobinterviewcoach.persistence.repository.AnswerAttemptRepository;
import com.houra.jobinterviewcoach.persistence.repository.InterviewQuestionRepository;
import com.houra.jobinterviewcoach.persistence.repository.InterviewSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class InterviewFeedbackServiceTests {

    @Autowired
    private InterviewSessionService sessionService;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private InterviewQuestionRepository questionRepository;

    @Autowired
    private AnswerAttemptRepository answerAttemptRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private RecordingAiService aiService;
    private InterviewFeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        aiService = new RecordingAiService(feedbackResult());
        feedbackService = new InterviewFeedbackService(
                aiService,
                sessionRepository,
                questionRepository,
                answerAttemptRepository,
                transactionTemplate
        );
    }

    @Test
    void usesPersistedContextAndSavesLinkedAnswerAttempt() {
        InterviewSession session = createSession(
                "Persisted CV",
                "Persisted job description",
                "Persisted interview question"
        );
        InterviewQuestion question = session.getQuestions().get(0);

        InterviewFeedbackResult result = feedbackService.evaluateAndSave(
                session.getId(),
                question.getId(),
                "My interview answer"
        );

        assertEquals("Persisted CV", aiService.receivedCvText);
        assertEquals("Persisted job description", aiService.receivedJobText);
        assertEquals("Persisted interview question", aiService.receivedQuestionText);
        assertEquals("My interview answer", aiService.receivedUserAnswer);
        assertFalse(aiService.transactionActiveDuringCall);
        assertEquals("Persisted interview question", result.questionText());
        assertSame(aiService.feedback, result.feedback());
        assertNotNull(result.answerAttemptId());

        transactionTemplate.executeWithoutResult(status -> {
            AnswerAttempt loadedAttempt = answerAttemptRepository
                    .findById(result.answerAttemptId())
                    .orElseThrow();

            assertEquals("My interview answer", loadedAttempt.getUserAnswer());
            assertEquals("8/10", loadedAttempt.getScore());
            assertEquals("Clear and relevant", loadedAttempt.getStrengths());
            assertEquals("More implementation detail", loadedAttempt.getMissingPoints());
            assertEquals("Add a concrete example", loadedAttempt.getImprovementTips());
            assertNotNull(loadedAttempt.getCreatedAt());
            assertEquals(question.getId(), loadedAttempt.getQuestion().getId());
            assertEquals(session.getId(), loadedAttempt.getQuestion().getSession().getId());
        });
    }

    @Test
    void rejectsUnknownSession() {
        InterviewSession session = createSession("CV", "Job", "Question");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> feedbackService.evaluateAndSave(
                        Long.MAX_VALUE,
                        session.getQuestions().get(0).getId(),
                        "Answer"
                )
        );

        assertEquals("Interview session not found: " + Long.MAX_VALUE, exception.getMessage());
        assertEquals(0, aiService.callCount);
    }

    @Test
    void rejectsUnknownQuestion() {
        InterviewSession session = createSession("CV", "Job", "Question");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> feedbackService.evaluateAndSave(
                        session.getId(),
                        Long.MAX_VALUE,
                        "Answer"
                )
        );

        assertEquals("Interview question not found: " + Long.MAX_VALUE, exception.getMessage());
        assertEquals(0, aiService.callCount);
    }

    @Test
    void rejectsQuestionFromAnotherSession() {
        InterviewSession firstSession = createSession("First CV", "First job", "First question");
        InterviewSession secondSession = createSession("Second CV", "Second job", "Second question");
        Long secondQuestionId = secondSession.getQuestions().get(0).getId();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> feedbackService.evaluateAndSave(
                        firstSession.getId(),
                        secondQuestionId,
                        "Answer"
                )
        );

        assertEquals(
                "Interview question " + secondQuestionId
                        + " does not belong to interview session " + firstSession.getId() + ".",
                exception.getMessage()
        );
        assertEquals(0, aiService.callCount);
    }

    @Test
    void rejectsBlankUserAnswer() {
        InterviewSession session = createSession("CV", "Job", "Question");
        Long questionId = session.getQuestions().get(0).getId();

        IllegalArgumentException nullAnswerException = assertThrows(
                IllegalArgumentException.class,
                () -> feedbackService.evaluateAndSave(session.getId(), questionId, null)
        );
        IllegalArgumentException blankAnswerException = assertThrows(
                IllegalArgumentException.class,
                () -> feedbackService.evaluateAndSave(session.getId(), questionId, "  \t")
        );

        assertEquals("Answer must not be blank.", nullAnswerException.getMessage());
        assertEquals("Answer must not be blank.", blankAnswerException.getMessage());
        assertEquals(0, aiService.callCount);
    }

    private InterviewSession createSession(String cvText, String jobText, String questionText) {
        return sessionService.createSession(cvText, jobText, List.of(questionText));
    }

    private static FeedbackResult feedbackResult() {
        FeedbackResult feedback = new FeedbackResult();
        feedback.setScore("8/10");
        feedback.setStrengths("Clear and relevant");
        feedback.setMissingPoints("More implementation detail");
        feedback.setImprovementTips("Add a concrete example");
        return feedback;
    }

    private static class RecordingAiService extends AiService {

        private final FeedbackResult feedback;
        private String receivedCvText;
        private String receivedJobText;
        private String receivedQuestionText;
        private String receivedUserAnswer;
        private boolean transactionActiveDuringCall;
        private int callCount;

        RecordingAiService(FeedbackResult feedback) {
            super("http://localhost");
            this.feedback = feedback;
        }

        @Override
        public FeedbackResult analyzeAnswer(
                String cvText,
                String jobText,
                String question,
                String userAnswer
        ) {
            callCount++;
            receivedCvText = cvText;
            receivedJobText = jobText;
            receivedQuestionText = question;
            receivedUserAnswer = userAnswer;
            transactionActiveDuringCall = TransactionSynchronizationManager.isActualTransactionActive();
            return feedback;
        }
    }
}
