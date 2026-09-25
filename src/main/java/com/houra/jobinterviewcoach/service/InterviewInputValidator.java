package com.houra.jobinterviewcoach.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class InterviewInputValidator {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private final int maxCvCharacters;
    private final int maxJobCharacters;
    private final int maxAnswerCharacters;
    private final long maxPdfBytes;

    public InterviewInputValidator(
            @Value("${app.input.max-cv-characters:20000}") int maxCvCharacters,
            @Value("${app.input.max-job-characters:20000}") int maxJobCharacters,
            @Value("${app.input.max-answer-characters:10000}") int maxAnswerCharacters,
            @Value("${app.input.max-pdf-bytes:5242880}") long maxPdfBytes
    ) {
        if (maxCvCharacters <= 0 || maxJobCharacters <= 0 || maxAnswerCharacters <= 0 || maxPdfBytes <= 0) {
            throw new IllegalStateException("Input limits must be greater than zero.");
        }

        this.maxCvCharacters = maxCvCharacters;
        this.maxJobCharacters = maxJobCharacters;
        this.maxAnswerCharacters = maxAnswerCharacters;
        this.maxPdfBytes = maxPdfBytes;
    }

    public void validateCvText(String cvText) {
        validateTextLength(cvText, maxCvCharacters, "CV text");
    }

    public void validateJobText(String jobText) {
        validateTextLength(jobText, maxJobCharacters, "Job description");
    }

    public void validateAnswer(String answer) {
        validateTextLength(answer, maxAnswerCharacters, "Answer");
    }

    public void validatePdf(MultipartFile file) {
        if (file != null && file.getSize() > maxPdfBytes) {
            throw new InputLimitExceededException(
                    "The uploaded PDF is too large. Maximum file size is " + formatBytes(maxPdfBytes) + "."
            );
        }
    }

    private void validateTextLength(String text, int maximumCharacters, String fieldName) {
        if (text != null && text.length() > maximumCharacters) {
            throw new InputLimitExceededException(
                    fieldName + " is too long. Maximum length is " + maximumCharacters + " characters."
            );
        }
    }

    private String formatBytes(long bytes) {
        if (bytes % BYTES_PER_MEGABYTE == 0) {
            return (bytes / BYTES_PER_MEGABYTE) + " MB";
        }
        if (bytes == 1) {
            return "1 byte";
        }
        return bytes + " bytes";
    }
}
