package com.houra.jobinterviewcoach.persistence.repository;

import java.time.Instant;

public interface InterviewSessionSummaryProjection {

    Long getId();

    Instant getCreatedAt();

    long getQuestionCount();

    long getAnsweredQuestionCount();

    long getAnswerAttemptCount();
}
