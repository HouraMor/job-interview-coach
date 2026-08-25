package com.houra.jobinterviewcoach.controller;

import com.houra.jobinterviewcoach.model.InterviewForm;
import com.houra.jobinterviewcoach.service.AiService;
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

class PageControllerTests {

    @Test
    void uploadedPdfTextReplacesPastedCvText() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("Extracted PDF text");
        PageController pageController = new PageController(aiService, pdfTextExtractor);
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
        assertEquals("Extracted PDF text", model.getAttribute("cvText"));
        assertEquals(List.of("Question one"), model.getAttribute("questions"));
        assertEquals(file, pdfTextExtractor.receivedFile);
        assertEquals("Extracted PDF text", aiService.receivedCvText);
        assertEquals("Job description", aiService.receivedJobText);
    }

    @Test
    void pastedTextIsUsedWhenNoPdfIsSelected() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("Extracted PDF text");
        PageController pageController = new PageController(aiService, pdfTextExtractor);
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
        assertEquals("Pasted CV text", model.getAttribute("cvText"));
        assertEquals(List.of("Question one"), model.getAttribute("questions"));
        assertNull(pdfTextExtractor.receivedFile);
        assertEquals("Pasted CV text", aiService.receivedCvText);
        assertEquals("Job description", aiService.receivedJobText);
    }

    @Test
    void uploadedJobPdfTextReplacesPastedJobText() {
        RecordingAiService aiService = new RecordingAiService();
        RecordingPdfTextExtractor pdfTextExtractor = new RecordingPdfTextExtractor("Extracted job PDF text");
        PageController pageController = new PageController(aiService, pdfTextExtractor);
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
        assertEquals("Pasted CV text", model.getAttribute("cvText"));
        assertEquals("Extracted job PDF text", model.getAttribute("jobText"));
        assertEquals(jobFile, pdfTextExtractor.receivedFile);
        assertEquals("Pasted CV text", aiService.receivedCvText);
        assertEquals("Extracted job PDF text", aiService.receivedJobText);
    }

    @Test
    void pdfExtractionErrorReturnsToTheForm() {
        RecordingAiService aiService = new RecordingAiService();
        PdfTextExtractor pdfTextExtractor = new PdfTextExtractor() {
            @Override
            public String extractText(MultipartFile file) {
                throw new IllegalArgumentException("Only PDF files are supported.");
            }
        };
        PageController pageController = new PageController(aiService, pdfTextExtractor);
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
}
