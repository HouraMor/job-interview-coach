package com.houra.jobinterviewcoach.controller;

import com.houra.jobinterviewcoach.model.FeedbackResult;
import com.houra.jobinterviewcoach.model.InterviewFeedbackResult;
import com.houra.jobinterviewcoach.model.InterviewForm;
import com.houra.jobinterviewcoach.model.InterviewQuestionView;
import com.houra.jobinterviewcoach.persistence.entity.InterviewQuestion;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import com.houra.jobinterviewcoach.service.AiService;
import com.houra.jobinterviewcoach.service.AiServiceException;
import com.houra.jobinterviewcoach.service.InterviewFeedbackService;
import com.houra.jobinterviewcoach.service.InterviewInputValidator;
import com.houra.jobinterviewcoach.service.InterviewSessionService;
import com.houra.jobinterviewcoach.service.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageControllerTests {

    @Test
    void uploadedPdfTextReplacesPastedCvText() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("Extracted PDF text");
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        RecordingInterviewFeedbackService feedbackService = new RecordingInterviewFeedbackService();
        PageController pageController = new PageController(
                aiService, pdfTextExtractor, sessionService, feedbackService, defaultInputValidator()
        );
        InterviewForm form = new InterviewForm();
        form.setCvText("Pasted CV text");
        form.setJobText("Job description");
        MockMultipartFile file = new MockMultipartFile(
                "cvFile",
                "cv.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[]{1}
        );
        Model model = new ExtendedModelMap();

        String viewName = pageController.generateQuestions(form, file, null, model);

        assertEquals("questions", viewName);
        assertEquals(
                List.of(new InterviewQuestionView(101L, "Question one", 1)),
                model.getAttribute("questions")
        );
        assertEquals(42L, model.getAttribute("sessionId"));
        assertEquals(file, pdfTextExtractor.receivedFile);
        assertEquals("Extracted PDF text", aiService.receivedCvText);
        assertEquals("Job description", aiService.receivedJobText);
        assertEquals("Extracted PDF text", sessionService.receivedCvText);
        assertEquals("Job description", sessionService.receivedJobText);
        assertEquals(List.of("Question one"), sessionService.receivedQuestions);
    }

    @Test
    void pastedTextIsUsedWhenNoPdfIsSelected() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("Extracted PDF text");
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        RecordingInterviewFeedbackService feedbackService = new RecordingInterviewFeedbackService();
        PageController pageController = new PageController(
                aiService, pdfTextExtractor, sessionService, feedbackService, defaultInputValidator()
        );
        InterviewForm form = new InterviewForm();
        form.setCvText("Pasted CV text");
        form.setJobText("Job description");
        MockMultipartFile noFile = new MockMultipartFile(
                "cvFile",
                "",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[0]
        );
        Model model = new ExtendedModelMap();

        String viewName = pageController.generateQuestions(form, noFile, null, model);

        assertEquals("questions", viewName);
        assertEquals(
                List.of(new InterviewQuestionView(101L, "Question one", 1)),
                model.getAttribute("questions")
        );
        assertEquals(42L, model.getAttribute("sessionId"));
        assertNull(model.getAttribute("cvText"));
        assertNull(model.getAttribute("jobText"));
        assertNull(pdfTextExtractor.receivedFile);
        assertEquals("Pasted CV text", aiService.receivedCvText);
        assertEquals("Job description", aiService.receivedJobText);
        assertEquals("Pasted CV text", sessionService.receivedCvText);
        assertEquals("Job description", sessionService.receivedJobText);
        assertEquals(List.of("Question one"), sessionService.receivedQuestions);
    }

    @Test
    void uploadedJobPdfTextReplacesPastedJobText() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("Extracted job PDF text");
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        RecordingInterviewFeedbackService feedbackService = new RecordingInterviewFeedbackService();
        PageController pageController = new PageController(
                aiService, pdfTextExtractor, sessionService, feedbackService, defaultInputValidator()
        );
        InterviewForm form = new InterviewForm();
        form.setCvText("Pasted CV text");
        form.setJobText("Pasted job description");
        MockMultipartFile jobFile = new MockMultipartFile(
                "jobFile",
                "job.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[]{1}
        );
        Model model = new ExtendedModelMap();

        String viewName = pageController.generateQuestions(form, null, jobFile, model);

        assertEquals("questions", viewName);
        assertEquals(42L, model.getAttribute("sessionId"));
        assertEquals(
                List.of(new InterviewQuestionView(101L, "Question one", 1)),
                model.getAttribute("questions")
        );
        assertEquals(jobFile, pdfTextExtractor.receivedFile);
        assertEquals("Pasted CV text", aiService.receivedCvText);
        assertEquals("Extracted job PDF text", aiService.receivedJobText);
        assertEquals("Pasted CV text", sessionService.receivedCvText);
        assertEquals("Extracted job PDF text", sessionService.receivedJobText);
        assertEquals(List.of("Question one"), sessionService.receivedQuestions);
    }

    @Test
    void pdfExtractionErrorReturnsToTheForm() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        PdfTextExtractor pdfTextExtractor = new PdfTextExtractor() {
            @Override
            public String extractText(MultipartFile file) {
                throw new IllegalArgumentException("Only PDF files are supported.");
            }
        };
        RecordingInterviewFeedbackService feedbackService = new RecordingInterviewFeedbackService();
        PageController pageController = new PageController(
                aiService, pdfTextExtractor, sessionService, feedbackService, defaultInputValidator()
        );
        InterviewForm form = new InterviewForm();
        form.setJobText("Job description");
        MockMultipartFile file = new MockMultipartFile(
                "cvFile",
                "cv.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Not a PDF".getBytes()
        );
        Model model = new ExtendedModelMap();

        String viewName = pageController.generateQuestions(form, file, null, model);

        assertEquals("index", viewName);
        assertEquals("Only PDF files are supported.", model.getAttribute("errorMessage"));
        assertNull(aiService.receivedCvText);
        assertNull(sessionService.receivedCvText);
    }

    @Test
    void feedbackUsesPersistedContextThroughSessionAndQuestionIds() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("Unused PDF text");
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        FeedbackResult feedback = new FeedbackResult();
        feedback.setScore("8/10");
        feedback.setStrengths("Clear example");
        feedback.setMissingPoints("More detail");
        feedback.setImprovementTips("Explain the trade-off");
        RecordingInterviewFeedbackService feedbackService = new RecordingInterviewFeedbackService(
                new InterviewFeedbackResult("Persisted question", feedback, 501L)
        );
        PageController pageController = new PageController(
                aiService,
                pdfTextExtractor,
                sessionService,
                feedbackService,
                defaultInputValidator()
        );
        Model model = new ExtendedModelMap();

        String viewName = pageController.analyzeAnswer(42L, 101L, "My answer", model);

        assertEquals("feedback", viewName);
        assertEquals(42L, feedbackService.receivedSessionId);
        assertEquals(101L, feedbackService.receivedQuestionId);
        assertEquals("My answer", feedbackService.receivedUserAnswer);
        assertEquals(42L, model.getAttribute("sessionId"));
        assertEquals(101L, model.getAttribute("questionId"));
        assertEquals(501L, model.getAttribute("answerAttemptId"));
        assertEquals("Persisted question", model.getAttribute("selectedQuestion"));
        assertEquals("My answer", model.getAttribute("userAnswer"));
        assertEquals(feedback, model.getAttribute("feedback"));
        assertNull(model.getAttribute("cvText"));
        assertNull(model.getAttribute("jobText"));
    }

    @Test
    void oversizedCvTextIsRejectedBeforeAiOrPersistence() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        PageController pageController = new PageController(
                aiService,
                new RecordingPdfTextExtractor("Unused"),
                sessionService,
                new RecordingInterviewFeedbackService(),
                new InterviewInputValidator(5, 100, 100, 100)
        );
        InterviewForm form = new InterviewForm();
        form.setCvText("123456");
        form.setJobText("Job");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pageController.generateQuestions(form, null, null, new ExtendedModelMap())
        );

        assertEquals("CV text is too long. Maximum length is 5 characters.", exception.getMessage());
        assertNull(aiService.receivedCvText);
        assertNull(sessionService.receivedCvText);
    }

    @Test
    void oversizedJobTextIsRejectedBeforeAiOrPersistence() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        PageController pageController = new PageController(
                aiService,
                new RecordingPdfTextExtractor("Unused"),
                sessionService,
                new RecordingInterviewFeedbackService(),
                new InterviewInputValidator(100, 5, 100, 100)
        );
        InterviewForm form = new InterviewForm();
        form.setCvText("CV");
        form.setJobText("123456");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pageController.generateQuestions(form, null, null, new ExtendedModelMap())
        );

        assertEquals("Job description is too long. Maximum length is 5 characters.", exception.getMessage());
        assertNull(aiService.receivedJobText);
        assertNull(sessionService.receivedJobText);
    }

    @Test
    void oversizedPdfIsRejectedBeforeExtractionOrAi() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("Unused");
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        PageController pageController = new PageController(
                aiService,
                pdfTextExtractor,
                sessionService,
                new RecordingInterviewFeedbackService(),
                new InterviewInputValidator(100, 100, 100, 1)
        );
        InterviewForm form = new InterviewForm();
        form.setJobText("Job");
        MockMultipartFile file = new MockMultipartFile(
                "cvFile",
                "cv.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[]{1, 2}
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pageController.generateQuestions(form, file, null, new ExtendedModelMap())
        );

        assertEquals("The uploaded PDF is too large. Maximum file size is 1 byte.", exception.getMessage());
        assertNull(pdfTextExtractor.receivedFile);
        assertNull(aiService.receivedCvText);
        assertNull(sessionService.receivedCvText);
    }

    @Test
    void oversizedExtractedPdfTextIsRejectedBeforeAiOrPersistence() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("123456");
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        PageController pageController = new PageController(
                aiService,
                pdfTextExtractor,
                sessionService,
                new RecordingInterviewFeedbackService(),
                new InterviewInputValidator(5, 100, 100, 100)
        );
        InterviewForm form = new InterviewForm();
        form.setJobText("Job");
        MockMultipartFile file = new MockMultipartFile(
                "cvFile",
                "cv.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[]{1}
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pageController.generateQuestions(form, file, null, new ExtendedModelMap())
        );

        assertEquals("CV text is too long. Maximum length is 5 characters.", exception.getMessage());
        assertEquals(file, pdfTextExtractor.receivedFile);
        assertNull(aiService.receivedCvText);
        assertNull(sessionService.receivedCvText);
    }

    @Test
    void aiFailureDoesNotCreateAnInterviewSession() {
        AiService failingAiService = new AiService("http://localhost") {
            @Override
            public List<String> generateQuestions(String cvText, String jobText) {
                throw new AiServiceException("AI question generation is currently unavailable. Please try again later.");
            }
        };
        RecordingInterviewSessionService sessionService = new RecordingInterviewSessionService();
        PageController pageController = new PageController(
                failingAiService,
                new RecordingPdfTextExtractor("Unused"),
                sessionService,
                new RecordingInterviewFeedbackService(),
                defaultInputValidator()
        );
        InterviewForm form = new InterviewForm();
        form.setCvText("CV");
        form.setJobText("Job");

        assertThrows(
                AiServiceException.class,
                () -> pageController.generateQuestions(form, null, null, new ExtendedModelMap())
        );

        assertNull(sessionService.receivedCvText);
        assertNull(sessionService.receivedQuestions);
    }

    private static InterviewInputValidator defaultInputValidator() {
        return new InterviewInputValidator(20_000, 20_000, 10_000, 5L * 1024L * 1024L);
    }

    private static class RecordingAiService extends AiService {

        private String receivedCvText;
        private String receivedJobText;

        RecordingAiService() {
            super("http://localhost");
        }

        @Override
        public List<String> generateQuestions(String cvText, String jobText) {
            receivedCvText = cvText;
            receivedJobText = jobText;
            return List.of("Question one");
        }
    }

    private static class RecordingPdfTextExtractor extends PdfTextExtractor {

        private final String extractedText;
        private MultipartFile receivedFile;

        RecordingPdfTextExtractor(String extractedText) {
            this.extractedText = extractedText;
        }

        @Override
        public String extractText(MultipartFile file) {
            receivedFile = file;
            return extractedText;
        }
    }

    private static class RecordingInterviewSessionService extends InterviewSessionService {

        private final InterviewSession savedSession;
        private String receivedCvText;
        private String receivedJobText;
        private List<String> receivedQuestions;

        RecordingInterviewSessionService() {
            super(null);
            savedSession = new SavedInterviewSession();
        }

        @Override
        public InterviewSession createSession(String cvText, String jobText, List<String> generatedQuestions) {
            receivedCvText = cvText;
            receivedJobText = jobText;
            receivedQuestions = List.copyOf(generatedQuestions);
            return savedSession;
        }
    }

    private static class RecordingInterviewFeedbackService extends InterviewFeedbackService {

        private final InterviewFeedbackResult result;
        private Long receivedSessionId;
        private Long receivedQuestionId;
        private String receivedUserAnswer;

        RecordingInterviewFeedbackService() {
            this(null);
        }

        RecordingInterviewFeedbackService(InterviewFeedbackResult result) {
            super(null, null, null, null, null, defaultInputValidator());
            this.result = result;
        }

        @Override
        public InterviewFeedbackResult evaluateAndSave(
                Long sessionId,
                Long questionId,
                String userAnswer
        ) {
            receivedSessionId = sessionId;
            receivedQuestionId = questionId;
            receivedUserAnswer = userAnswer;
            return result;
        }
    }

    private static class SavedInterviewSession extends InterviewSession {

        private final List<InterviewQuestion> savedQuestions = List.of(new SavedInterviewQuestion());

        SavedInterviewSession() {
            super("Saved CV", "Saved job description");
        }

        @Override
        public Long getId() {
            return 42L;
        }

        @Override
        public List<InterviewQuestion> getQuestions() {
            return savedQuestions;
        }
    }

    private static class SavedInterviewQuestion extends InterviewQuestion {

        SavedInterviewQuestion() {
            super("Question one", 1);
        }

        @Override
        public Long getId() {
            return 101L;
        }
    }
}
