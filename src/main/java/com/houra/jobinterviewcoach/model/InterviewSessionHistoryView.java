package com.houra.jobinterviewcoach.model;

import java.time.Instant;
import java.util.List;

public record InterviewSessionHistoryView(
        Long id,
        Instant createdAt,
        String cvText,
        String jobText,
        List<InterviewQuestionHistoryView> questions
) {
    public InterviewSessionHistoryView {
        questions = List.copyOf(questions);
    }
}
