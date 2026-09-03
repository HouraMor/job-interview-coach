package com.houra.jobinterviewcoach.persistence.repository;

import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    @Query("""
            SELECT session.id AS id,
                   session.createdAt AS createdAt,
                   COUNT(DISTINCT question.id) AS questionCount,
                   COUNT(DISTINCT CASE
                       WHEN attempt.id IS NOT NULL THEN question.id
                       ELSE NULL
                   END) AS answeredQuestionCount,
                   COUNT(attempt.id) AS answerAttemptCount
            FROM InterviewSession session
            LEFT JOIN session.questions question
            LEFT JOIN question.answerAttempts attempt
            GROUP BY session.id, session.createdAt
            ORDER BY session.createdAt DESC, session.id DESC
            """)
    List<InterviewSessionSummaryProjection> findHistorySummaries();

    @Query("""
            SELECT DISTINCT session
            FROM InterviewSession session
            LEFT JOIN FETCH session.questions question
            WHERE session.id = :sessionId
            """)
    Optional<InterviewSession> findByIdWithQuestions(@Param("sessionId") Long sessionId);
}
