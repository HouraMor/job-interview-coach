package com.houra.jobinterviewcoach.service;

import com.houra.jobinterviewcoach.model.AnswerAttemptHistoryView;
import com.houra.jobinterviewcoach.model.InterviewQuestionHistoryView;
import com.houra.jobinterviewcoach.model.InterviewSessionHistoryView;
import com.houra.jobinterviewcoach.model.InterviewSessionSummaryView;
import com.houra.jobinterviewcoach.persistence.entity.AnswerAttempt;
import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import com.houra.jobinterviewcoach.persistence.repository.AnswerAttemptRepository;
import com.houra.jobinterviewcoach.persistence.repository.InterviewSessionRepository;
import com.houra.jobinterviewcoach.persistence.repository.InterviewSessionSummaryProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InterviewHistoryService {

    private final InterviewSessionRepository sessionRepository;
    private final AnswerAttemptRepository answerAttemptRepository;

    public InterviewHistoryService(
            InterviewSessionRepository sessionRepository,
            AnswerAttemptRepository answerAttemptRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.answerAttemptRepository = answerAttemptRepository;
    }

    public List<InterviewSessionSummaryView> findAllSessions() {
        return sessionRepository.findHistorySummaries().stream()
                .map(this::toSummaryView)
                .toList();
    }

    public InterviewSessionHistoryView getSession(Long sessionId) {
        InterviewSession session = sessionRepository.findByIdWithQuestions(sessionId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(sessionId));

        Map<Long, List<AnswerAttemptHistoryView>> attemptsByQuestion = new HashMap<>();
        for (AnswerAttempt attempt : answerAttemptRepository.findHistoryAttemptsBySessionId(sessionId)) {
            attemptsByQuestion
                    .computeIfAbsent(attempt.getQuestion().getId(), ignored -> new ArrayList<>())
                    .add(toAttemptView(attempt));
        }

        List<InterviewQuestionHistoryView> questions = session.getQuestions().stream()
                .sorted(Comparator.comparingInt(InterviewQuestion::getPosition))
                .map(question -> new InterviewQuestionHistoryView(
                        question.getId(),
                        question.getPosition(),
                        question.getQuestionText(),
                        attemptsByQuestion.getOrDefault(question.getId(), List.of())
                ))
                .toList();

        return new InterviewSessionHistoryView(
                session.getId(),
                session.getCreatedAt(),
                session.getCvText(),
                session.getJobText(),
                questions
        );
    }

    private InterviewSessionSummaryView toSummaryView(InterviewSessionSummaryProjection summary) {
        return new InterviewSessionSummaryView(
                summary.getId(),
                summary.getCreatedAt(),
                Math.toIntExact(summary.getQuestionCount()),
                Math.toIntExact(summary.getAnsweredQuestionCount()),
                Math.toIntExact(summary.getAnswerAttemptCount())
        );
    }

    private AnswerAttemptHistoryView toAttemptView(AnswerAttempt attempt) {
        return new AnswerAttemptHistoryView(
                attempt.getId(),
                attempt.getUserAnswer(),
                attempt.getScore(),
                attempt.getStrengths(),
                attempt.getMissingPoints(),
                attempt.getImprovementTips(),
                attempt.getCreatedAt()
        );
    }
}
