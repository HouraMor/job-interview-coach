package com.houra.jobinterviewcoach.controller;

import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import com.houra.jobinterviewcoach.persistence.repository.AnswerAttemptRepository;
import com.houra.jobinterviewcoach.persistence.repository.InterviewSessionRepository;
import com.houra.jobinterviewcoach.service.AiService;
import com.houra.jobinterviewcoach.service.AiServiceException;
import com.houra.jobinterviewcoach.service.InterviewSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicAiEndpointTests {

    private static final String QUESTIONS_UNAVAILABLE =
            "AI question generation is currently unavailable. Please try again later.";
    private static final String FEEDBACK_UNAVAILABLE =
            "AI feedback is currently unavailable. Please try again later.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InterviewSessionService sessionService;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private AnswerAttemptRepository answerAttemptRepository;

    @MockitoBean
    private AiService aiService;

    @Test
    void anonymousUserCanGenerateQuestionsAndPersistARealSession() throws Exception {
        given(aiService.generateQuestions(anyString(), anyString())).willReturn(tenQuestions());
        long sessionsBefore = sessionRepository.count();

        mockMvc.perform(multipart("/questions")
                        .param("cvText", "Java and Spring Boot experience")
                        .param("jobText", "Backend developer role")
                        .with(csrf())
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(view().name("questions"))
                .andExpect(model().attributeExists("sessionId", "questions"));

        assertEquals(sessionsBefore + 1, sessionRepository.count());
    }

    @Test
    void questionProviderFailureReturns503AndDoesNotPersistASession() throws Exception {
        given(aiService.generateQuestions(anyString(), anyString()))
                .willThrow(new AiServiceException(QUESTIONS_UNAVAILABLE));
        long sessionsBefore = sessionRepository.count();

        mockMvc.perform(multipart("/questions")
                        .param("cvText", "Java experience")
                        .param("jobText", "Backend role")
                        .with(csrf())
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.11");
                            return request;
                        }))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("operation-error"))
                .andExpect(model().attribute("errorMessage", QUESTIONS_UNAVAILABLE));

        assertEquals(sessionsBefore, sessionRepository.count());
    }

    @Test
    void feedbackProviderFailureReturns503AndDoesNotPersistAnAttempt() throws Exception {
        InterviewSession session = sessionService.createSession("CV", "Job", List.of("Question"));
        InterviewQuestion question = session.getQuestions().get(0);
        given(aiService.analyzeAnswer(anyString(), anyString(), anyString(), anyString()))
                .willThrow(new AiServiceException(FEEDBACK_UNAVAILABLE));
        long attemptsBefore = answerAttemptRepository.count();

        mockMvc.perform(post("/feedback")
                        .param("sessionId", session.getId().toString())
                        .param("questionId", question.getId().toString())
                        .param("userAnswer", "My answer")
                        .with(csrf())
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.12");
                            return request;
                        }))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("operation-error"))
                .andExpect(model().attribute("errorMessage", FEEDBACK_UNAVAILABLE));

        assertEquals(attemptsBefore, answerAttemptRepository.count());
    }

    private static List<String> tenQuestions() {
        return List.of(
                "Question 1", "Question 2", "Question 3", "Question 4", "Question 5",
                "Question 6", "Question 7", "Question 8", "Question 9", "Question 10"
        );
    }
}
