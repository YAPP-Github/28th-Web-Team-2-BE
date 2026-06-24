package com.looky.api.survey.dto;

import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.domain.ResultStatus;

import java.util.Map;

public record SurveyResultResponse(
        String surveyCode,
        ResultStatus resultStatus,
        Map<String, String> quadrantImageUrls,
        Map<String, String> quadrantInterpretations,
        String overallKeyword,
        String overallAnalysis,
        String actionTip,
        Map<String, QuadrantResultResponse> quadrants
) {
    public static SurveyResultResponse from(SurveyResultResult result) {
        return new SurveyResultResponse(
                result.surveyCode(),
                result.resultStatus(),
                result.quadrantImageUrls(),
                result.quadrantInterpretations(),
                result.overallKeyword(), result.overallAnalysis(), result.actionTip(),
                result.quadrants() == null ? null : result.quadrants().entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> QuadrantResultResponse.from(entry.getValue())))
        );
    }
}
