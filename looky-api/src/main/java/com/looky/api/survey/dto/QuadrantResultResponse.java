package com.looky.api.survey.dto;

import com.looky.survey.application.dto.SurveyResultQuadrant;
import java.util.List;

public record QuadrantResultResponse(String definitionKeyword, List<String> adjectiveKeywords, String interpretation, String imageUrl) {
    static QuadrantResultResponse from(SurveyResultQuadrant result) {
        return new QuadrantResultResponse(result.definitionKeyword(), result.adjectiveKeywords(), result.interpretation(), result.imageUrl());
    }
}
