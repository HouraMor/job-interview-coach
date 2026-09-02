package com.houra.jobinterviewcoach.service;

import com.houra.jobinterviewcoach.model.FeedbackResult;
import com.houra.jobinterviewcoach.model.InterviewFeedbackResult;
import com.houra.jobinterviewcoach.persistence.entity.AnswerAttempt;
import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import com.houra.jobinterviewcoach.persistence.repository.AnswerAttemptRepository;
import com.houra.jobinterviewcoach.persistence.repository.InterviewQuestionRepository;
import com.houra.jobinterviewcoach.persistence.repository.InterviewSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
public class InterviewFeedbackService {

    private final AiService aiService;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final AnswerAttemptRepository answerAttemptRepository;
    private final TransactionTemplate transactionTemplate;

    public InterviewFeedbackService(
            AiService aiService,
            InterviewSessionRepository sessionRepository,
            InterviewQuestionRepository questionRepository,
            AnswerAttemptRepository answerAttemptRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.aiService = aiService;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.answerAttemptRepository = answerAttemptRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public InterviewFeedbackResult evaluateAndSave(
            Long sessionId,
            Long questionId,
            String userAnswer
    ) {
        validateInput(sessionId, questionId, userAnswer);

        FeedbackContext context = Objects.requireNonNull(
                transactionTemplate.execute(status -> loadContext(sessionId, questionId))
        );

        FeedbackResult feedback = aiService.analyzeAnswer(
                context.cvText(),
                context.jobText(),
                context.questionText(),
                userAnswer
        );

        Long answerAttemptId = Objects.requireNonNull(
                transactionTemplate.execute(
                        status -> saveAnswerAttempt(context, userAnswer, feedback)
                )
        );

        return new InterviewFeedbackResult(context.questionText(), feedback, answerAttemptId);
    }

    private void validateInput(Long sessionId, Long questionId, String userAnswer) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID must not be null.");
        }
        if (questionId == null) {
            throw new IllegalArgumentException("Question ID must not be null.");
        }
        if (userAnswer == null || userAnswer.isBlank()) {
            throw new IllegalArgumentException("Answer must not be blank.");
        }
    }

    private FeedbackContext loadContext(Long sessionId, Long questionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Interview session not found: " + sessionId
                ));
        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Interview question not found: " + questionId
                ));

        validateQuestionOwnership(sessionId, questionId, question);

        return new FeedbackContext(
                session.getId(),
                question.getId(),
                session.getCvText(),
                session.getJobText(),
                question.getQuestionText()
        );
    }

    private Long saveAnswerAttempt(
            FeedbackContext context,
            String userAnswer,
            FeedbackResult feedback
    ) {
        InterviewQuestion question = questionRepository.findById(context.questionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Interview question not found: " + context.questionId()
                ));
        validateQuestionOwnership(context.sessionId(), context.questionId(), question);

        AnswerAttempt answerAttempt = new AnswerAttempt(
                userAnswer,
                feedback.getScore(),
                feedback.getStrengths(),
                feedback.getMissingPoints(),
                feedback.getImprovementTips()
        );
        question.addAnswerAttempt(answerAttempt);

        return answerAttemptRepository.save(answerAttempt).getId();
    }

    private void validateQuestionOwnership(
            Long sessionId,
            Long questionId,
            InterviewQuestion question
    ) {
        if (!sessionId.equals(question.getSession().getId())) {
            throw new IllegalArgumentException(
                    "Interview question " + questionId
                            + " does not belong to interview session " + sessionId + "."
            );
        }
    }

    private record FeedbackContext(
            Long sessionId,
            Long questionId,
            String cvText,
            String jobText,
            String questionText
    ) {
    }
}
