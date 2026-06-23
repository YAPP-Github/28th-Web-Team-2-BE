package com.looky.submission.persistence;

import com.looky.question.persistence.QuestionJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "submission_questions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_questions_question", columnNames = {"submission_id", "question_id"}),
        @UniqueConstraint(name = "uk_submission_questions_sequence", columnNames = {"submission_id", "sequence"})
})
public class SubmissionQuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private SubmissionJpaEntity submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionJpaEntity question;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "question_content_snapshot", nullable = false, columnDefinition = "text")
    private String questionContentSnapshot;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SubmissionQuestionJpaEntity() {
    }

    public SubmissionQuestionJpaEntity(SubmissionJpaEntity submission, QuestionJpaEntity question, int sequence, String questionContentSnapshot, OffsetDateTime createdAt) {
        this.submission = submission;
        this.question = question;
        this.sequence = sequence;
        this.questionContentSnapshot = questionContentSnapshot;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public SubmissionJpaEntity getSubmission() {
        return submission;
    }

    public Long getQuestionId() {
        return question.getId();
    }

    public int getSequence() {
        return sequence;
    }

    public String getQuestionContentSnapshot() {
        return questionContentSnapshot;
    }
}
