package com.looky.result.persistence;

import com.looky.result.domain.ResultQuadrantType;
import com.looky.result.domain.QuadrantWorkStatus;
import com.looky.result.application.ResultQuadrantRecord;
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

@Entity
@Table(name = "result_quadrants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_result_quadrants_result_type", columnNames = {"result_id", "quadrant_type"})
})
public class ResultQuadrantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private ResultJpaEntity result;

    @Enumerated(EnumType.STRING)
    @Column(name = "quadrant_type", nullable = false, length = 32)
    private ResultQuadrantType quadrantType;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "interpretation", columnDefinition = "text")
    private String interpretation;

    @Column(name = "image_prompt", columnDefinition = "text")
    private String imagePrompt;

    @Column(name = "s3_object_key", length = 1024)
    private String s3ObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_status", nullable = false, length = 32)
    private QuadrantWorkStatus workStatus;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    protected ResultQuadrantJpaEntity() {
    }

    public ResultQuadrantJpaEntity(ResultJpaEntity result, ResultQuadrantType quadrantType, String imageUrl) {
        this.result = result;
        this.quadrantType = quadrantType;
        this.imageUrl = imageUrl;
        this.workStatus = QuadrantWorkStatus.IMAGE_READY;
    }

    public ResultQuadrantJpaEntity(
            ResultJpaEntity result,
            ResultQuadrantType quadrantType,
            String interpretation,
            String imagePrompt
    ) {
        this.result = result;
        this.quadrantType = quadrantType;
        this.interpretation = interpretation;
        this.imagePrompt = imagePrompt;
        this.workStatus = QuadrantWorkStatus.NARRATIVE_READY;
    }

    public ResultQuadrantJpaEntity(ResultJpaEntity result, ResultQuadrantRecord quadrant) {
        this(result, quadrant.quadrantType(), quadrant.imageUrl());
        this.interpretation = quadrant.interpretation();
        this.s3ObjectKey = quadrant.s3ObjectKey();
    }

    public ResultQuadrantType getQuadrantType() {
        return quadrantType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getInterpretation() { return interpretation; }
    public String getImagePrompt() { return imagePrompt; }
    public String getS3ObjectKey() { return s3ObjectKey; }
    public QuadrantWorkStatus getWorkStatus() { return workStatus; }
    public int getAttemptCount() { return attemptCount; }
    public String getFailureReason() { return failureReason; }

    public void completeImage(String imageUrl, String s3ObjectKey) {
        this.imageUrl = imageUrl;
        this.s3ObjectKey = s3ObjectKey;
        this.workStatus = QuadrantWorkStatus.IMAGE_READY;
        this.failureReason = null;
    }

    public void failImage(String failureReason) {
        this.workStatus = QuadrantWorkStatus.FAILED;
        this.attemptCount++;
        this.failureReason = failureReason;
    }
}
