package com.looky.survey.application.dto;

import com.looky.result.application.ResultOverviewRecord;
import com.looky.survey.domain.ResultStatus;

import java.util.Map;

public record SurveyResultResult(
        String surveyCode,
        ResultStatus resultStatus,
        Map<String, String> quadrantImageUrls,
        Map<String, String> quadrantInterpretations,
        ResultOverviewRecord overall,
        Map<String, SurveyResultQuadrant> quadrants
) {
    public SurveyResultResult(String surveyCode, ResultStatus resultStatus, Map<String, String> quadrantImageUrls) {
        this(surveyCode, resultStatus, quadrantImageUrls, null, null, null);
    }

    public SurveyResultResult(String surveyCode, ResultStatus resultStatus, Map<String, String> quadrantImageUrls, Map<String, String> quadrantInterpretations) {
        this(surveyCode, resultStatus, quadrantImageUrls, quadrantInterpretations, null, null);
    }
}
