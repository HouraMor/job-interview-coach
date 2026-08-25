package com.houra.jobinterviewcoach.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Service
public class PdfTextExtractor {

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded PDF is empty.");
        }

        if (!isPdf(file)) {
            throw new IllegalArgumentException("Only PDF files are supported.");
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document).trim();

            if (text.isBlank()) {
                throw new IllegalArgumentException(
                        "No readable text was found in the PDF. Scanned PDFs requiring OCR are not supported."
                );
            }

            return text;
        } catch (IOException e) {
            throw new IllegalArgumentException("The uploaded file could not be read as a valid PDF.", e);
        }
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        return MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType)
                || (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"));
    }
}
