package com.looky.survey.application.dto;

import com.looky.survey.domain.ResultStatus;

import java.util.Map;

public record SurveyResultResult(
        String surveyCode,
        ResultStatus resultStatus,
        Map<String, String> quadrantImageUrls
) {
}
