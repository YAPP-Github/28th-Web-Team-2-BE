package com.looky.survey.application;

import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;

import java.time.OffsetDateTime;

public record SurveyRecord(
        Long id,
        String userNickname,
        String surveyCode,
        SurveyStatus surveyStatus,
        ResultStatus resultStatus,
        int requiredPeerSubmissionCount,
        OffsetDateTime resultAvailableAt,
        OffsetDateTime createdAt
) {
}
