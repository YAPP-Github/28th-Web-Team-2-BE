package com.looky.survey.persistence;

import com.looky.result.domain.ResultGenerationPhase;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "surveys", uniqueConstraints = {
        @UniqueConstraint(name = "uk_surveys_survey_code", columnNames = "survey_code")
})
public class SurveyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_nickname", nullable = false, length = 40)
    private String userNickname;

    @Column(name = "survey_code", nullable = false, length = 12)
    private String surveyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "survey_status", nullable = false, length = 32)
    private SurveyStatus surveyStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 64)
    private ResultStatus resultStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_phase", length = 40)
    private ResultGenerationPhase generationPhase;

    @Column(name = "result_generation_attempt_count", nullable = false)
    private int resultGenerationAttemptCount;

    @Column(name = "required_peer_submission_count", nullable = false)
    private int requiredPeerSubmissionCount;

    @Column(name = "result_available_at", nullable = false)
    private OffsetDateTime resultAvailableAt;

    @Column(name = "character_pack_key", length = 80)
    private String characterPackKey;

    @Column(name = "character_pack_version", length = 40)
    private String characterPackVersion;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SurveyJpaEntity() {
    }

    public SurveyJpaEntity(
            String userNickname,
            String surveyCode,
            int requiredPeerSubmissionCount,
            OffsetDateTime now,
            OffsetDateTime resultAvailableAt,
            String characterPackKey,
            String characterPackVersion
    ) {
        this.userNickname = userNickname;
        this.surveyCode = surveyCode;
        this.surveyStatus = SurveyStatus.DRAFT;
        this.resultStatus = ResultStatus.WAITING_SELF_RESPONSE;
        this.resultGenerationAttemptCount = 0;
        this.requiredPeerSubmissionCount = requiredPeerSubmissionCount;
        this.resultAvailableAt = resultAvailableAt;
        this.characterPackKey = characterPackKey;
        this.characterPackVersion = characterPackVersion;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void markCollecting() {
        this.surveyStatus = SurveyStatus.COLLECTING;
    }

    public void updateResultStatus(ResultStatus resultStatus) {
        this.resultStatus = resultStatus;
        if (resultStatus != ResultStatus.GENERATING) {
            this.generationPhase = null;
        }
    }

    public void updateGenerationPhase(ResultGenerationPhase generationPhase) {
        this.generationPhase = generationPhase;
    }

    public Long getId() {
        return id;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public String getSurveyCode() {
        return surveyCode;
    }

    public SurveyStatus getSurveyStatus() {
        return surveyStatus;
    }

    public ResultStatus getResultStatus() {
        return resultStatus;
    }

    public ResultGenerationPhase getGenerationPhase() {
        return generationPhase;
    }

    public int getResultGenerationAttemptCount() {
        return resultGenerationAttemptCount;
    }

    public int getRequiredPeerSubmissionCount() {
        return requiredPeerSubmissionCount;
    }

    public OffsetDateTime getResultAvailableAt() {
        return resultAvailableAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCharacterPackKey() {
        return characterPackKey;
    }

    public String getCharacterPackVersion() {
        return characterPackVersion;
    }
}
