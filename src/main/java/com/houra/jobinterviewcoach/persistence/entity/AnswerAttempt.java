package com.houra.jobinterviewcoach.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "answer_attempt")
public class AnswerAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "question_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_answer_attempt_question")
    )
    private InterviewQuestion question;

    @Column(name = "user_answer", nullable = false, columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "score")
    private String score;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "missing_points", columnDefinition = "TEXT")
    private String missingPoints;

    @Column(name = "improvement_tips", columnDefinition = "TEXT")
    private String improvementTips;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AnswerAttempt() {
    }

    public AnswerAttempt(
            String userAnswer,
            String score,
            String strengths,
            String missingPoints,
            String improvementTips
    ) {
        this.userAnswer = Objects.requireNonNull(userAnswer, "userAnswer must not be null");
        this.score = score;
        this.strengths = strengths;
        this.missingPoints = missingPoints;
        this.improvementTips = improvementTips;
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public InterviewQuestion getQuestion() {
        return question;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public String getScore() {
        return score;
    }

    public String getStrengths() {
        return strengths;
    }

    public String getMissingPoints() {
        return missingPoints;
    }

    public String getImprovementTips() {
        return improvementTips;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void setQuestion(InterviewQuestion question) {
        this.question = question;
    }
}
