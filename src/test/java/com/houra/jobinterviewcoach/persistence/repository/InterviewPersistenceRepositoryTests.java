package com.houra.jobinterviewcoach.persistence.repository;

import com.houra.jobinterviewcoach.persistence.entity.AnswerAttempt;
import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InterviewPersistenceRepositoryTests {

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private InterviewQuestionRepository questionRepository;

    @Autowired
    private AnswerAttemptRepository answerAttemptRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndLoadsInterviewSession() {
        InterviewSession session = new InterviewSession("CV text", "Job text");

        InterviewSession savedSession = sessionRepository.saveAndFlush(session);
        Long sessionId = savedSession.getId();
        entityManager.clear();

        InterviewSession loadedSession = sessionRepository.findById(sessionId).orElseThrow();

        assertNotNull(loadedSession.getId());
        assertNotNull(loadedSession.getCreatedAt());
        assertEquals("CV text", loadedSession.getCvText());
        assertEquals("Job text", loadedSession.getJobText());
    }

    @Test
    void savesAndLoadsQuestionsAndAnswerAttemptsInQuestionOrder() {
        InterviewSession session = new InterviewSession("CV text", "Job text");
        InterviewQuestion secondQuestion = new InterviewQuestion("Second question", 2);
        InterviewQuestion firstQuestion = new InterviewQuestion("First question", 1);
        AnswerAttempt answerAttempt = new AnswerAttempt(
                "My answer",
                "7/10",
                "Clear example",
                "More technical detail",
                "Explain the trade-off"
        );

        firstQuestion.addAnswerAttempt(answerAttempt);
        session.addQuestion(secondQuestion);
        session.addQuestion(firstQuestion);

        sessionRepository.saveAndFlush(session);
        Long sessionId = session.getId();
        Long firstQuestionId = firstQuestion.getId();
        Long answerAttemptId = answerAttempt.getId();
        entityManager.clear();

        InterviewSession loadedSession = sessionRepository.findById(sessionId).orElseThrow();
        List<InterviewQuestion> loadedQuestions = loadedSession.getQuestions();

        assertEquals(List.of(1, 2), loadedQuestions.stream().map(InterviewQuestion::getPosition).toList());
        assertTrue(loadedQuestions.stream().allMatch(question -> question.getSession().getId().equals(sessionId)));

        InterviewQuestion loadedFirstQuestion = loadedQuestions.get(0);
        assertEquals("First question", loadedFirstQuestion.getQuestionText());
        assertEquals(1, loadedFirstQuestion.getAnswerAttempts().size());

        AnswerAttempt loadedAttempt = loadedFirstQuestion.getAnswerAttempts().get(0);
        assertEquals(answerAttemptId, loadedAttempt.getId());
        assertEquals(firstQuestionId, loadedAttempt.getQuestion().getId());
        assertEquals("My answer", loadedAttempt.getUserAnswer());
        assertEquals("7/10", loadedAttempt.getScore());
        assertEquals("Clear example", loadedAttempt.getStrengths());
        assertEquals("More technical detail", loadedAttempt.getMissingPoints());
        assertEquals("Explain the trade-off", loadedAttempt.getImprovementTips());
        assertNotNull(loadedAttempt.getCreatedAt());

        assertTrue(questionRepository.findById(firstQuestionId).isPresent());
        assertTrue(answerAttemptRepository.findById(answerAttemptId).isPresent());
    }
}
