package com.houra.jobinterviewcoach.service;

import com.houra.jobinterviewcoach.model.AnswerAttemptHistoryView;
import com.houra.jobinterviewcoach.model.InterviewQuestionHistoryView;
import com.houra.jobinterviewcoach.model.InterviewSessionHistoryView;
import com.houra.jobinterviewcoach.model.InterviewSessionSummaryView;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class InterviewHistoryServiceTests {

    @Autowired
    private InterviewHistoryService historyService;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private InterviewQuestionRepository questionRepository;

    @Autowired
    private AnswerAttemptRepository answerAttemptRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteTestData() {
        transactionTemplate.executeWithoutResult(status -> {
            answerAttemptRepository.deleteAllInBatch();
            questionRepository.deleteAllInBatch();
            sessionRepository.deleteAllInBatch();
        });
    }

    @Test
    void returnsNewestSessionsFirst() {
        InterviewSession olderSession = persistSession(new InterviewSession("Older CV", "Older job"));
        InterviewSession newerSession = persistSession(new InterviewSession("Newer CV", "Newer job"));
        setSessionCreatedAt(olderSession.getId(), Instant.parse("2026-09-01T08:00:00Z"));
        setSessionCreatedAt(newerSession.getId(), Instant.parse("2026-09-02T08:00:00Z"));

        List<InterviewSessionSummaryView> summaries = historyService.findAllSessions();

        assertEquals(
                List.of(newerSession.getId(), olderSession.getId()),
                summaries.stream().map(InterviewSessionSummaryView::id).toList()
        );
    }

    @Test
    void calculatesOverviewCountsWithoutLoadingFullHistory() {
        InterviewSession session = new InterviewSession("CV", "Job");
        InterviewQuestion firstQuestion = new InterviewQuestion("First question", 1);
        InterviewQuestion secondQuestion = new InterviewQuestion("Second question", 2);
        InterviewQuestion unansweredQuestion = new InterviewQuestion("Unanswered question", 3);
        firstQuestion.addAnswerAttempt(answerAttempt("First answer"));
        firstQuestion.addAnswerAttempt(answerAttempt("Second answer"));
        secondQuestion.addAnswerAttempt(answerAttempt("Third answer"));
        session.addQuestion(firstQuestion);
        session.addQuestion(secondQuestion);
        session.addQuestion(unansweredQuestion);
        persistSession(session);

        InterviewSessionSummaryView summary = historyService.findAllSessions().get(0);

        assertEquals(3, summary.questionCount());
        assertEquals(2, summary.answeredQuestionCount());
        assertEquals(3, summary.answerAttemptCount());
    }

    @Test
    void mapsDetailWithOrderedQuestionsAndChronologicalAttempts() {
        InterviewSession session = new InterviewSession("Detailed CV", "Detailed job");
        InterviewQuestion secondQuestion = new InterviewQuestion("Unanswered second question", 2);
        InterviewQuestion firstQuestion = new InterviewQuestion("Answered first question", 1);
        AnswerAttempt newerAttempt = new AnswerAttempt(
                "Newer answer",
                "9/10",
                "Strong newer answer",
                "Small newer gap",
                "Newer tip"
        );
        AnswerAttempt olderAttempt = new AnswerAttempt(
                "Older answer",
                "6/10",
                "Strong older answer",
                "Large older gap",
                "Older tip"
        );
        firstQuestion.addAnswerAttempt(newerAttempt);
        firstQuestion.addAnswerAttempt(olderAttempt);
        session.addQuestion(secondQuestion);
        session.addQuestion(firstQuestion);
        persistSession(session);

        Instant olderTime = Instant.parse("2026-09-01T08:00:00Z");
        Instant newerTime = Instant.parse("2026-09-02T08:00:00Z");
        setAttemptCreatedAt(olderAttempt.getId(), olderTime);
        setAttemptCreatedAt(newerAttempt.getId(), newerTime);

        InterviewSessionHistoryView detail = historyService.getSession(session.getId());

        assertEquals(session.getId(), detail.id());
        assertEquals("Detailed CV", detail.cvText());
        assertEquals("Detailed job", detail.jobText());
        assertEquals(
                List.of(1, 2),
                detail.questions().stream().map(InterviewQuestionHistoryView::position).toList()
        );

        InterviewQuestionHistoryView answeredQuestion = detail.questions().get(0);
        assertEquals(firstQuestion.getId(), answeredQuestion.id());
        assertEquals("Answered first question", answeredQuestion.questionText());
        assertEquals(
                List.of(olderAttempt.getId(), newerAttempt.getId()),
                answeredQuestion.attempts().stream().map(AnswerAttemptHistoryView::id).toList()
        );

        AnswerAttemptHistoryView mappedOlderAttempt = answeredQuestion.attempts().get(0);
        assertEquals("Older answer", mappedOlderAttempt.userAnswer());
        assertEquals("6/10", mappedOlderAttempt.score());
        assertEquals("Strong older answer", mappedOlderAttempt.strengths());
        assertEquals("Large older gap", mappedOlderAttempt.missingPoints());
        assertEquals("Older tip", mappedOlderAttempt.improvementTips());
        assertEquals(olderTime, mappedOlderAttempt.createdAt());

        InterviewQuestionHistoryView unansweredQuestion = detail.questions().get(1);
        assertEquals(secondQuestion.getId(), unansweredQuestion.id());
        assertTrue(unansweredQuestion.attempts().isEmpty());
    }

    @Test
    void rejectsUnknownSessionId() {
        InterviewSessionNotFoundException exception = assertThrows(
                InterviewSessionNotFoundException.class,
                () -> historyService.getSession(Long.MAX_VALUE)
        );

        assertEquals("Interview session not found: " + Long.MAX_VALUE, exception.getMessage());
    }

    private InterviewSession persistSession(InterviewSession session) {
        return transactionTemplate.execute(status -> sessionRepository.saveAndFlush(session));
    }

    private AnswerAttempt answerAttempt(String userAnswer) {
        return new AnswerAttempt(
                userAnswer,
                "7/10",
                "Strengths",
                "Missing points",
                "Improvement tips"
        );
    }

    private void setSessionCreatedAt(Long sessionId, Instant createdAt) {
        jdbcTemplate.update(
                "UPDATE interview_session SET created_at = ? WHERE id = ?",
                Timestamp.from(createdAt),
                sessionId
        );
    }

    private void setAttemptCreatedAt(Long attemptId, Instant createdAt) {
        jdbcTemplate.update(
                "UPDATE answer_attempt SET created_at = ? WHERE id = ?",
                Timestamp.from(createdAt),
                attemptId
        );
    }
}
