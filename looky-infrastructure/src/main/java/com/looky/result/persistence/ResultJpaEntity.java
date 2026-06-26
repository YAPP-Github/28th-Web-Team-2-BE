package com.looky.result.persistence;

import com.looky.survey.persistence.SurveyJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "results", uniqueConstraints = {
        @UniqueConstraint(name = "uk_results_survey_id", columnNames = "survey_id")
})
public class ResultJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private SurveyJpaEntity survey;

    @Column(name = "overall_keyword", length = 120)
    private String overallKeyword;
    @Column(name = "overall_analysis_title", length = 120)
    private String overallAnalysisTitle;
    @Column(name = "overall_analysis", columnDefinition = "text")
    private String overallAnalysis;
    @Column(name = "action_tip", columnDefinition = "text")
    private String actionTip;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ResultJpaEntity() {
    }

    public ResultJpaEntity(SurveyJpaEntity survey, OffsetDateTime now) {
        this.survey = survey;
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

    public Long getId() {
        return id;
    }

    public SurveyJpaEntity getSurvey() {
        return survey;
    }
    public String getOverallKeyword() { return overallKeyword; }
    public String getOverallAnalysisTitle() { return overallAnalysisTitle; }
    public String getOverallAnalysis() { return overallAnalysis; }
    public String getActionTip() { return actionTip; }
    public void saveOverview(com.looky.result.application.ResultNarrative.Overview overview) {
        this.overallKeyword = overview.keyword();
        this.overallAnalysisTitle = overview.analysisTitle();
        this.overallAnalysis = overview.analysisBody();
        this.actionTip = overview.tip();
    }
}
