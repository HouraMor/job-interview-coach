package com.houra.jobinterviewcoach.service;

public class InputLimitExceededException extends IllegalArgumentException {

    public InputLimitExceededException(String message) {
        super(message);
    }
}
