package com.houra.jobinterviewcoach.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "interview_question",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_interview_question_session_position",
                columnNames = {"session_id", "position"}
        )
)
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_interview_question_session")
    )
    private InterviewSession session;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "position", nullable = false)
    private int position;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AnswerAttempt> answerAttempts = new ArrayList<>();

    protected InterviewQuestion() {
    }

    public InterviewQuestion(String questionText, int position) {
        this.questionText = Objects.requireNonNull(questionText, "questionText must not be null");
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public InterviewSession getSession() {
        return session;
    }

    public String getQuestionText() {
        return questionText;
    }

    public int getPosition() {
        return position;
    }

    public List<AnswerAttempt> getAnswerAttempts() {
        return answerAttempts;
    }

    public void addAnswerAttempt(AnswerAttempt answerAttempt) {
        AnswerAttempt attemptToAdd = Objects.requireNonNull(answerAttempt, "answerAttempt must not be null");
        answerAttempts.add(attemptToAdd);
        attemptToAdd.setQuestion(this);
    }

    void setSession(InterviewSession session) {
        this.session = session;
    }
}
