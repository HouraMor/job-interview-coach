package com.houra.jobinterviewcoach.model;

import java.util.List;

public record InterviewQuestionHistoryView(
        Long id,
        int position,
        String questionText,
        List<AnswerAttemptHistoryView> attempts
) {
    public InterviewQuestionHistoryView {
        attempts = List.copyOf(attempts);
    }
}
