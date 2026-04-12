package com.houra.jobinterviewcoach.model;

public class FeedbackResult {
    private String score;
    private String strengths;
    private String missingPoints;
    private String improvementTips;

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getMissingPoints() {
        return missingPoints;
    }

    public void setMissingPoints(String missingPoints) {
        this.missingPoints = missingPoints;
    }

    public String getImprovementTips() {
        return improvementTips;
    }

    public void setImprovementTips(String improvementTips) {
        this.improvementTips = improvementTips;
    }
}