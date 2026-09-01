package com.houra.jobinterviewcoach.persistence.repository;

import com.houra.jobinterviewcoach.persistence.entity.AnswerAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerAttemptRepository extends JpaRepository<AnswerAttempt, Long> {
}
