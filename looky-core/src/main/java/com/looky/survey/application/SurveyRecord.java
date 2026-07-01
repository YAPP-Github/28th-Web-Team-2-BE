package com.looky.survey.application;

import com.looky.result.domain.ResultGenerationPhase;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;

import java.time.OffsetDateTime;

public record SurveyRecord(
        Long id,
        String userNickname,
        String surveyCode,
        SurveyStatus surveyStatus,
        ResultStatus resultStatus,
        int resultGenerationAttemptCount,
        int requiredPeerSubmissionCount,
        OffsetDateTime resultAvailableAt,
        OffsetDateTime createdAt,
        ResultGenerationPhase generationPhase,
        String characterPackKey,
        String characterPackVersion
) {
    public SurveyRecord(
            Long id,
            String userNickname,
            String surveyCode,
            SurveyStatus surveyStatus,
            ResultStatus resultStatus,
            int resultGenerationAttemptCount,
            int requiredPeerSubmissionCount,
            OffsetDateTime resultAvailableAt,
            OffsetDateTime createdAt,
            String characterPackKey,
            String characterPackVersion
    ) {
        this(
                id,
                userNickname,
                surveyCode,
                surveyStatus,
                resultStatus,
                resultGenerationAttemptCount,
                requiredPeerSubmissionCount,
                resultAvailableAt,
                createdAt,
                null,
                characterPackKey,
                characterPackVersion
        );
    }
}
