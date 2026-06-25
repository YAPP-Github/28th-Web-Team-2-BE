package com.looky.api.survey.dto;

import com.looky.survey.application.dto.SurveyStatusResult;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;

import java.time.OffsetDateTime;

public record SurveyStatusResponse(
        String surveyCode,
        String userNickname,
        SurveyStatus surveyStatus,
        ResultStatus resultStatus,
        boolean selfSubmitted,
        long peerSubmissionCount,
        int requiredPeerSubmissionCount,
        OffsetDateTime resultAvailableAt,
        long remainingSecondsToResultOpen,
        String shareUrl,
        String resultUrl
) {
    public static SurveyStatusResponse from(SurveyStatusResult result) {
        String resultUrl = result.resultStatus() == ResultStatus.READY
                ? "https://looky.my/" + result.surveyCode() + "/result"
                : null;
        return new SurveyStatusResponse(
                result.surveyCode(),
                result.userNickname(),
                result.surveyStatus(),
                result.resultStatus(),
                result.selfSubmitted(),
                result.peerSubmissionCount(),
                result.requiredPeerSubmissionCount(),
                result.resultAvailableAt(),
                result.remainingSecondsToResultOpen(),
                "https://looky.my/" + result.surveyCode(),
                resultUrl
        );
    }
}
