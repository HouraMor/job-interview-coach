package com.houra.jobinterviewcoach.model;

import java.time.Instant;

public record InterviewSessionSummaryView(
        Long id,
        Instant createdAt,
        int questionCount,
        int answeredQuestionCount,
        int answerAttemptCount
) {
}
