package com.looky.api.survey.dto;

import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.domain.ResultStatus;

import java.util.Map;

public record SurveyResultResponse(
        String surveyCode,
        ResultStatus resultStatus,
        Map<String, String> quadrantImageUrls
) {
    public static SurveyResultResponse from(SurveyResultResult result) {
        return new SurveyResultResponse(
                result.surveyCode(),
                result.resultStatus(),
                result.quadrantImageUrls()
        );
    }
}
