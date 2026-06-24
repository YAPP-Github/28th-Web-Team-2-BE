package com.looky.survey.application.dto;

import java.util.List;

public record SurveyResultQuadrant(
        String definitionKeyword,
        List<String> adjectiveKeywords,
        String interpretation,
        String imageUrl
) {
}
