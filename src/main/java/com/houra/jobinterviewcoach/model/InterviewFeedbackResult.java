package com.houra.jobinterviewcoach.model;

public record InterviewFeedbackResult(
        String questionText,
        FeedbackResult feedback,
        Long answerAttemptId
) {
}
