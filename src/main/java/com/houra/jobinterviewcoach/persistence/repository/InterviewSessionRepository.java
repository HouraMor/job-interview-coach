package com.houra.jobinterviewcoach.persistence.repository;

import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
}
