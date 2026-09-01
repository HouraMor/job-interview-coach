package com.houra.jobinterviewcoach.service;

import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import com.houra.jobinterviewcoach.persistence.repository.InterviewSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InterviewSessionService {

    private final InterviewSessionRepository sessionRepository;

    public InterviewSessionService(InterviewSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public InterviewSession createSession(
            String cvText,
            String jobText,
            List<String> generatedQuestions
    ) {
        if (cvText == null || cvText.isBlank()) {
            throw new IllegalArgumentException("CV text must not be blank.");
        }
        if (jobText == null || jobText.isBlank()) {
            throw new IllegalArgumentException("Job description must not be blank.");
        }
        if (generatedQuestions == null || generatedQuestions.isEmpty()) {
            throw new IllegalArgumentException("Generated questions must not be null or empty.");
        }

        InterviewSession session = new InterviewSession(cvText, jobText);

        for (int index = 0; index < generatedQuestions.size(); index++) {
            InterviewQuestion question = new InterviewQuestion(generatedQuestions.get(index), index + 1);
            session.addQuestion(question);
        }

        return sessionRepository.save(session);
    }
}
