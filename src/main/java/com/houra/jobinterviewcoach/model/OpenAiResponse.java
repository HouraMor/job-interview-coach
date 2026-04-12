package com.houra.jobinterviewcoach.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenAiResponse {
    @JsonProperty("output_text")
    private String outputText;

    public String getOutputText() {
        return outputText;
    }

    public void setOutputText(String outputText) {
        this.outputText = outputText;
    }
}