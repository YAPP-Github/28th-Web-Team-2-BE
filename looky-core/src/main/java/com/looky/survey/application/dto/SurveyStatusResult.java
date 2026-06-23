package com.looky.survey.application.dto;

import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;

import java.time.OffsetDateTime;

public record SurveyStatusResult(
        Long surveyId,
        String userNickname,
        SurveyStatus surveyStatus,
        ResultStatus resultStatus,
        boolean selfSubmitted,
        long peerSubmissionCount,
        int requiredPeerSubmissionCount,
        OffsetDateTime resultAvailableAt,
        long remainingSecondsToResultOpen,
        String surveyCode
) {
}
