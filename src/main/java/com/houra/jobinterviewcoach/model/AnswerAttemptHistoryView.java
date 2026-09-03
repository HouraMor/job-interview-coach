package com.houra.jobinterviewcoach.model;

import java.time.Instant;

public record AnswerAttemptHistoryView(
        Long id,
        String userAnswer,
        String score,
        String strengths,
        String missingPoints,
        String improvementTips,
        Instant createdAt
) {
}
