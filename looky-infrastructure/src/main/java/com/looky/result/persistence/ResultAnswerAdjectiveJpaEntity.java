package com.looky.result.persistence;

import com.looky.question.domain.TraitCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.List;

@Entity
@Table(name = "result_answer_adjectives", uniqueConstraints = {
        @UniqueConstraint(name = "uk_result_answer_adjectives_answer", columnNames = {"result_id", "submission_answer_id"})
})
public class ResultAnswerAdjectiveJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private ResultJpaEntity result;

    @Column(name = "submission_answer_id", nullable = false)
    private Long submissionAnswerId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trait_code", nullable = false, length = 40)
    private TraitCode traitCode;

    @Column(name = "question_snapshot", nullable = false, columnDefinition = "text")
    private String questionSnapshot;

    @Column(name = "answer_snapshot", nullable = false, columnDefinition = "text")
    private String answerSnapshot;

    @Column(name = "adjectives_json", nullable = false, columnDefinition = "text")
    private String adjectivesJson;

    protected ResultAnswerAdjectiveJpaEntity() {
    }

    public ResultAnswerAdjectiveJpaEntity(ResultJpaEntity result, com.looky.result.application.ResultAnswerAdjectiveRecord answer, List<String> adjectives) {
        this.result = result;
        this.submissionAnswerId = answer.submissionAnswerId();
        this.questionId = answer.questionId();
        this.traitCode = answer.traitCode();
        this.questionSnapshot = answer.questionSnapshot();
        this.answerSnapshot = answer.answerSnapshot();
        this.adjectivesJson = "[\"" + String.join("\",\"", adjectives) + "\"]";
    }
}
