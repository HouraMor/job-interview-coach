package com.houra.jobinterviewcoach.persistence.repository;

import com.houra.jobinterviewcoach.persistence.entity.AnswerAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerAttemptRepository extends JpaRepository<AnswerAttempt, Long> {

    @Query("""
            SELECT attempt
            FROM AnswerAttempt attempt
            JOIN FETCH attempt.question question
            WHERE question.session.id = :sessionId
            ORDER BY attempt.createdAt ASC, attempt.id ASC
            """)
    List<AnswerAttempt> findHistoryAttemptsBySessionId(@Param("sessionId") Long sessionId);
}
