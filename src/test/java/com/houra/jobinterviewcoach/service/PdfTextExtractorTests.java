package com.houra.jobinterviewcoach.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractorTests {

    private final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();

    @Test
    void extractsTextFromValidPdf() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "cvFile",
                "cv.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                createPdf("Java and Spring Boot experience")
        );

        String text = pdfTextExtractor.extractText(file);

        assertTrue(text.contains("Java and Spring Boot experience"));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "cvFile",
                "cv.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[0]
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pdfTextExtractor.extractText(file)
        );

        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void rejectsNonPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "cvFile",
                "cv.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Not a PDF".getBytes()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pdfTextExtractor.extractText(file)
        );

        assertTrue(exception.getMessage().contains("Only PDF"));
    }

    @Test
    void rejectsPdfWithoutReadableText() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "cvFile",
                "blank.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                createBlankPdf()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pdfTextExtractor.extractText(file)
        );

        assertTrue(exception.getMessage().contains("No readable text"));
    }

    private byte[] createPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(72, 720);
                stream.showText(text);
                stream.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createBlankPdf() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}
