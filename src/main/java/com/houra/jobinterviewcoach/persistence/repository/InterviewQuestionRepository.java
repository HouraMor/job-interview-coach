package com.houra.jobinterviewcoach.persistence.repository;

import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
}
