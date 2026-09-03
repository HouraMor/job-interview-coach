package com.houra.jobinterviewcoach.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InterviewSessionNotFoundException extends RuntimeException {

    public InterviewSessionNotFoundException(Long sessionId) {
        super("Interview session not found: " + sessionId);
    }
}
