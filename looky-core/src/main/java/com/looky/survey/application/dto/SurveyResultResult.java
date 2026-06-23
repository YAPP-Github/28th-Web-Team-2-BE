package com.looky.survey.application.dto;

import java.util.Map;

public record SurveyResultResult(
        String surveyCode,
        Map<String, String> quadrantImageUrls
) {
}
