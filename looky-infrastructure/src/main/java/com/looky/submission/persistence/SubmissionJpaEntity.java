package com.looky.submission.persistence;

import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.persistence.SurveyJpaEntity;
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

import java.time.OffsetDateTime;

@Entity
@Table(name = "submissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submissions_submitter_key", columnNames = {"survey_id", "submitter_key"})
})
public class SubmissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private SurveyJpaEntity survey;

    @Enumerated(EnumType.STRING)
    @Column(name = "submitter_type", nullable = false, length = 20)
    private SubmitterType submitterType;

    @Column(name = "submitter_key", nullable = false, length = 80)
    private String submitterKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false, length = 20)
    private SubmissionStatus submissionStatus;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SubmissionJpaEntity() {
    }

    public SubmissionJpaEntity(SurveyJpaEntity survey, SubmitterType submitterType, String submitterKey, OffsetDateTime now) {
        this.survey = survey;
        this.submitterType = submitterType;
        this.submitterKey = submitterKey;
        this.submissionStatus = SubmissionStatus.IN_PROGRESS;
        this.startedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void complete(OffsetDateTime now) {
        this.submissionStatus = SubmissionStatus.COMPLETED;
        this.submittedAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public SurveyJpaEntity getSurvey() {
        return survey;
    }

    public SubmitterType getSubmitterType() {
        return submitterType;
    }

    public SubmissionStatus getSubmissionStatus() {
        return submissionStatus;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }
}
