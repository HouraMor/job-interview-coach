package com.houra.jobinterviewcoach.controller;

import com.houra.jobinterviewcoach.model.FeedbackResult;
import com.houra.jobinterviewcoach.model.InterviewForm;
import com.houra.jobinterviewcoach.service.AiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class PageController {

    private final AiService aiService;

    public PageController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/")
    public String showHomePage(Model model) {
        model.addAttribute("interviewForm", new InterviewForm());
        return "index";
    }

    @PostMapping("/questions")
    public String generateQuestions(@ModelAttribute InterviewForm interviewForm, Model model) {
        List<String> questions = aiService.generateQuestions(
                interviewForm.getCvText(),
                interviewForm.getJobText()
        );

        model.addAttribute("cvText", interviewForm.getCvText());
        model.addAttribute("jobText", interviewForm.getJobText());
        model.addAttribute("questions", questions);
        model.addAttribute("interviewForm", interviewForm);

        return "questions";
    }

    @PostMapping("/feedback")
    public String analyzeAnswer(
            @RequestParam String cvText,
            @RequestParam String jobText,
            @RequestParam String selectedQuestion,
            @RequestParam String userAnswer,
            Model model
    ) {
        FeedbackResult feedback = aiService.analyzeAnswer(cvText, jobText, selectedQuestion, userAnswer);

        model.addAttribute("cvText", cvText);
        model.addAttribute("jobText", jobText);
        model.addAttribute("selectedQuestion", selectedQuestion);
        model.addAttribute("userAnswer", userAnswer);
        model.addAttribute("feedback", feedback);

        return "feedback";
    }
}