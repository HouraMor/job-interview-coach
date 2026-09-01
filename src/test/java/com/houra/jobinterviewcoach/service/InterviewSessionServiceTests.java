package com.houra.jobinterviewcoach.service;

import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import com.houra.jobinterviewcoach.persistence.repository.InterviewSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InterviewSessionServiceTests {

    @Autowired
    private InterviewSessionService sessionService;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void createsSessionWithQuestionsInOriginalOrder() {
        InterviewSession savedSession = sessionService.createSession(
                "CV text",
                "Job description",
                List.of("First question", "Second question", "Third question")
        );
        entityManager.flush();
        Long sessionId = savedSession.getId();
        entityManager.clear();

        InterviewSession loadedSession = sessionRepository.findById(sessionId).orElseThrow();
        List<InterviewQuestion> loadedQuestions = loadedSession.getQuestions();

        assertNotNull(loadedSession.getId());
        assertEquals("CV text", loadedSession.getCvText());
        assertEquals("Job description", loadedSession.getJobText());
        assertEquals(
                List.of("First question", "Second question", "Third question"),
                loadedQuestions.stream().map(InterviewQuestion::getQuestionText).toList()
        );
        assertEquals(
                List.of(1, 2, 3),
                loadedQuestions.stream().map(InterviewQuestion::getPosition).toList()
        );
        assertTrue(loadedQuestions.stream().allMatch(question -> question.getSession().getId().equals(sessionId)));
    }

    @Test
    void createsANewSessionForEveryCall() {
        InterviewSession firstSession = sessionService.createSession(
                "Same CV",
                "Same job",
                List.of("Same question")
        );
        InterviewSession secondSession = sessionService.createSession(
                "Same CV",
                "Same job",
                List.of("Same question")
        );
        entityManager.flush();

        assertNotEquals(firstSession.getId(), secondSession.getId());
    }

    @Test
    void rejectsBlankCvText() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> sessionService.createSession("  ", "Job description", List.of("Question"))
        );

        assertEquals("CV text must not be blank.", exception.getMessage());
    }

    @Test
    void rejectsBlankJobDescription() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> sessionService.createSession("CV text", "\t", List.of("Question"))
        );

        assertEquals("Job description must not be blank.", exception.getMessage());
    }

    @Test
    void rejectsNullOrEmptyGeneratedQuestions() {
        IllegalArgumentException nullQuestionsException = assertThrows(
                IllegalArgumentException.class,
                () -> sessionService.createSession("CV text", "Job description", null)
        );
        IllegalArgumentException emptyQuestionsException = assertThrows(
                IllegalArgumentException.class,
                () -> sessionService.createSession("CV text", "Job description", List.of())
        );

        assertEquals("Generated questions must not be null or empty.", nullQuestionsException.getMessage());
        assertEquals("Generated questions must not be null or empty.", emptyQuestionsException.getMessage());
    }
}
