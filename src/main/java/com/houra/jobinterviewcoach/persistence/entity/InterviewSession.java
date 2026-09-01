package com.houra.jobinterviewcoach.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "interview_session")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cv_text", nullable = false, columnDefinition = "TEXT")
    private String cvText;

    @Column(name = "job_text", nullable = false, columnDefinition = "TEXT")
    private String jobText;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<InterviewQuestion> questions = new ArrayList<>();

    protected InterviewSession() {
    }

    public InterviewSession(String cvText, String jobText) {
        this.cvText = Objects.requireNonNull(cvText, "cvText must not be null");
        this.jobText = Objects.requireNonNull(jobText, "jobText must not be null");
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCvText() {
        return cvText;
    }

    public String getJobText() {
        return jobText;
    }

    public List<InterviewQuestion> getQuestions() {
        return questions;
    }

    public void addQuestion(InterviewQuestion question) {
        InterviewQuestion questionToAdd = Objects.requireNonNull(question, "question must not be null");
        questions.add(questionToAdd);
        questionToAdd.setSession(this);
    }
}
