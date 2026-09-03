package com.houra.jobinterviewcoach.controller;

import com.houra.jobinterviewcoach.service.InterviewHistoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class InterviewHistoryController {

    private final InterviewHistoryService interviewHistoryService;

    public InterviewHistoryController(InterviewHistoryService interviewHistoryService) {
        this.interviewHistoryService = interviewHistoryService;
    }

    @GetMapping("/history")
    public String showHistory(Model model) {
        model.addAttribute("sessions", interviewHistoryService.findAllSessions());
        return "history";
    }

    @GetMapping("/history/{sessionId}")
    public String showSession(@PathVariable Long sessionId, Model model) {
        model.addAttribute("session", interviewHistoryService.getSession(sessionId));
        return "history-detail";
    }

    @PostMapping("/history/{sessionId}/delete")
    public String deleteSession(@PathVariable Long sessionId) {
        interviewHistoryService.deleteSession(sessionId);
        return "redirect:/history";
    }
}
