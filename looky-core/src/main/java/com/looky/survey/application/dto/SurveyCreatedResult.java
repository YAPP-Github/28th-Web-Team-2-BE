package com.looky.survey.application.dto;

import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;

import java.time.OffsetDateTime;

public record SurveyCreatedResult(
        Long surveyId,
        String userNickname,
        String surveyCode,
        SurveyStatus surveyStatus,
        ResultStatus resultStatus,
        int requiredPeerSubmissionCount,
        OffsetDateTime resultAvailableAt,
        OffsetDateTime createdAt
) {
}
