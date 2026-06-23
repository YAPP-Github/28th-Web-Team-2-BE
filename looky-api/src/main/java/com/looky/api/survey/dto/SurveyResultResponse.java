package com.looky.api.survey.dto;

import com.looky.survey.application.dto.SurveyResultResult;

import java.util.Map;

public record SurveyResultResponse(
        String surveyCode,
        Map<String, String> quadrantImageUrls
) {
    public static SurveyResultResponse from(SurveyResultResult result) {
        return new SurveyResultResponse(
                result.surveyCode(),
                result.quadrantImageUrls()
        );
    }
}
