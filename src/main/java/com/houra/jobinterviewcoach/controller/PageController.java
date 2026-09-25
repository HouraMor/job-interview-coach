package com.houra.jobinterviewcoach.controller;

import com.houra.jobinterviewcoach.model.InterviewFeedbackResult;
import com.houra.jobinterviewcoach.model.InterviewForm;
import com.houra.jobinterviewcoach.model.InterviewQuestionView;
import com.houra.jobinterviewcoach.persistence.entity.InterviewSession;
import com.houra.jobinterviewcoach.service.AiService;
import com.houra.jobinterviewcoach.service.InputLimitExceededException;
import com.houra.jobinterviewcoach.service.InterviewFeedbackService;
import com.houra.jobinterviewcoach.service.InterviewInputValidator;
import com.houra.jobinterviewcoach.service.InterviewSessionService;
import com.houra.jobinterviewcoach.service.PdfTextExtractor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class PageController {

    private final AiService aiService;
    private final PdfTextExtractor pdfTextExtractor;
    private final InterviewSessionService interviewSessionService;
    private final InterviewFeedbackService interviewFeedbackService;
    private final InterviewInputValidator inputValidator;

    public PageController(
            AiService aiService,
            PdfTextExtractor pdfTextExtractor,
            InterviewSessionService interviewSessionService,
            InterviewFeedbackService interviewFeedbackService,
            InterviewInputValidator inputValidator
    ) {
        this.aiService = aiService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.interviewSessionService = interviewSessionService;
        this.interviewFeedbackService = interviewFeedbackService;
        this.inputValidator = inputValidator;
    }

    @GetMapping("/")
    public String showHomePage(Model model) {
        model.addAttribute("interviewForm", new InterviewForm());
        return "index";
    }

    @PostMapping("/questions")
    public String generateQuestions(
            @ModelAttribute InterviewForm interviewForm,
            @RequestParam(name = "cvFile", required = false) MultipartFile cvFile,
            @RequestParam(name = "jobFile", required = false) MultipartFile jobFile,
            Model model
    ) {
        String cvText = interviewForm.getCvText();
        String jobText = interviewForm.getJobText();

        try {
            if (wasFileSelected(cvFile)) {
                inputValidator.validatePdf(cvFile);
                cvText = pdfTextExtractor.extractText(cvFile);
            }
            if (wasFileSelected(jobFile)) {
                inputValidator.validatePdf(jobFile);
                jobText = pdfTextExtractor.extractText(jobFile);
            }
        } catch (InputLimitExceededException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("interviewForm", interviewForm);
            return "index";
        }

        if (cvText == null || cvText.isBlank()) {
            model.addAttribute("errorMessage", "Paste your CV text or upload a PDF CV.");
            model.addAttribute("interviewForm", interviewForm);
            return "index";
        }

        if (jobText == null || jobText.isBlank()) {
            model.addAttribute("errorMessage", "Paste the job description or upload a PDF job description.");
            model.addAttribute("interviewForm", interviewForm);
            return "index";
        }

        inputValidator.validateCvText(cvText);
        inputValidator.validateJobText(jobText);

        List<String> questions = aiService.generateQuestions(
                cvText,
                jobText
        );
        InterviewSession session = interviewSessionService.createSession(cvText, jobText, questions);
        List<InterviewQuestionView> questionViews = session.getQuestions().stream()
                .map(question -> new InterviewQuestionView(
                        question.getId(),
                        question.getQuestionText(),
                        question.getPosition()
                ))
                .toList();

        model.addAttribute("sessionId", session.getId());
        model.addAttribute("questions", questionViews);
        model.addAttribute("interviewForm", interviewForm);

        return "questions";
    }

    private boolean wasFileSelected(MultipartFile file) {
        if (file == null) {
            return false;
        }

        String fileName = file.getOriginalFilename();
        return !file.isEmpty() || (fileName != null && !fileName.isBlank());
    }

    @PostMapping("/feedback")
    public String analyzeAnswer(
            @RequestParam Long sessionId,
            @RequestParam Long questionId,
            @RequestParam String userAnswer,
            Model model
    ) {
        InterviewFeedbackResult result = interviewFeedbackService.evaluateAndSave(
                sessionId,
                questionId,
                userAnswer
        );

        model.addAttribute("sessionId", sessionId);
        model.addAttribute("questionId", questionId);
        model.addAttribute("answerAttemptId", result.answerAttemptId());
        model.addAttribute("selectedQuestion", result.questionText());
        model.addAttribute("userAnswer", userAnswer);
        model.addAttribute("feedback", result.feedback());

        return "feedback";
    }
}
