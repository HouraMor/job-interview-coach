package com.houra.jobinterviewcoach.controller;

import com.houra.jobinterviewcoach.model.AnswerAttemptHistoryView;
import com.houra.jobinterviewcoach.model.InterviewQuestionHistoryView;
import com.houra.jobinterviewcoach.model.InterviewSessionHistoryView;
import com.houra.jobinterviewcoach.model.InterviewSessionSummaryView;
import com.houra.jobinterviewcoach.service.InterviewHistoryService;
import com.houra.jobinterviewcoach.service.InterviewSessionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterviewHistoryControllerTests {

    @Test
    void historyPageAddsSessionSummariesToTheModel() {
        List<InterviewSessionSummaryView> sessions = List.of(
                new InterviewSessionSummaryView(
                        42L,
                        Instant.parse("2026-09-02T09:00:00Z"),
                        10,
                        4,
                        6
                )
        );
        RecordingInterviewHistoryService historyService = new RecordingInterviewHistoryService(sessions, null);
        InterviewHistoryController controller = new InterviewHistoryController(historyService);
        Model model = new ExtendedModelMap();

        String viewName = controller.showHistory(model);

        assertEquals("history", viewName);
        assertEquals(sessions, model.getAttribute("sessions"));
        assertEquals(1, historyService.findAllCalls);
    }

    @Test
    void detailPageAddsTheCompleteSessionToTheModel() {
        InterviewSessionHistoryView session = sessionDetail();
        RecordingInterviewHistoryService historyService = new RecordingInterviewHistoryService(List.of(), session);
        InterviewHistoryController controller = new InterviewHistoryController(historyService);
        Model model = new ExtendedModelMap();

        String viewName = controller.showSession(42L, model);

        assertEquals("history-detail", viewName);
        assertEquals(session, model.getAttribute("session"));
        assertEquals(42L, historyService.receivedSessionId);
    }

    @Test
    void missingSessionPropagatesTheNotFoundResponse() {
        RecordingInterviewHistoryService historyService = new RecordingInterviewHistoryService(List.of(), null);
        InterviewHistoryController controller = new InterviewHistoryController(historyService);
        Model model = new ExtendedModelMap();

        InterviewSessionNotFoundException exception = assertThrows(
                InterviewSessionNotFoundException.class,
                () -> controller.showSession(999L, model)
        );

        assertEquals("Interview session not found: 999", exception.getMessage());
        assertEquals(999L, historyService.receivedSessionId);
        ResponseStatus responseStatus = InterviewSessionNotFoundException.class.getAnnotation(ResponseStatus.class);
        assertEquals(HttpStatus.NOT_FOUND, responseStatus.value());
    }

    private InterviewSessionHistoryView sessionDetail() {
        AnswerAttemptHistoryView attempt = new AnswerAttemptHistoryView(
                301L,
                "My answer",
                "8/10",
                "Clear example",
                "More technical detail",
                "Explain the trade-off",
                Instant.parse("2026-09-02T09:10:00Z")
        );
        InterviewQuestionHistoryView question = new InterviewQuestionHistoryView(
                101L,
                1,
                "Tell me about your project.",
                List.of(attempt)
        );
        return new InterviewSessionHistoryView(
                42L,
                Instant.parse("2026-09-02T09:00:00Z"),
                "CV text",
                "Job description",
                List.of(question)
        );
    }

    private static class RecordingInterviewHistoryService extends InterviewHistoryService {

        private final List<InterviewSessionSummaryView> sessions;
        private final InterviewSessionHistoryView session;
        private int findAllCalls;
        private Long receivedSessionId;

        RecordingInterviewHistoryService(
                List<InterviewSessionSummaryView> sessions,
                InterviewSessionHistoryView session
        ) {
            super(null, null);
            this.sessions = sessions;
            this.session = session;
        }

        @Override
        public List<InterviewSessionSummaryView> findAllSessions() {
            findAllCalls++;
            return sessions;
        }

        @Override
        public InterviewSessionHistoryView getSession(Long sessionId) {
            receivedSessionId = sessionId;
            if (session == null) {
                throw new InterviewSessionNotFoundException(sessionId);
            }
            return session;
        }
    }
}
