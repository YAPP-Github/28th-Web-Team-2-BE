package com.looky.submission.persistence;

import com.looky.question.persistence.AnswerOptionJpaEntity;
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
@Table(name = "submission_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_answers_question", columnNames = "submission_question_id")
})
public class SubmissionAnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_question_id", nullable = false)
    private SubmissionQuestionJpaEntity submissionQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_option_id", nullable = false)
    private AnswerOptionJpaEntity answerOption;

    @Column(name = "answer_content_snapshot", nullable = false, columnDefinition = "text")
    private String answerContentSnapshot;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SubmissionAnswerJpaEntity() {
    }

    public SubmissionAnswerJpaEntity(SubmissionQuestionJpaEntity submissionQuestion, AnswerOptionJpaEntity answerOption, String answerContentSnapshot, OffsetDateTime createdAt) {
        this.submissionQuestion = submissionQuestion;
        this.answerOption = answerOption;
        this.answerContentSnapshot = answerContentSnapshot;
        this.createdAt = createdAt;
    }
}
