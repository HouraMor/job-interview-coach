package com.houra.jobinterviewcoach.controller;

import com.houra.jobinterviewcoach.service.AiServiceException;
import com.houra.jobinterviewcoach.service.InputLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class PageExceptionHandler {

    @ExceptionHandler(AiServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleAiServiceFailure(AiServiceException exception, Model model) {
        model.addAttribute("errorMessage", exception.getMessage());
        return "operation-error";
    }

    @ExceptionHandler(InputLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInputLimit(InputLimitExceededException exception, Model model) {
        model.addAttribute("errorMessage", exception.getMessage());
        return "operation-error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String handleMultipartLimit(Model model) {
        model.addAttribute("errorMessage", "The uploaded PDF is too large.");
        return "operation-error";
    }
}
