package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;

import java.util.List;
import java.util.Map;

public record ResultNarrative(
        Overview overview,
        Map<Long, List<String>> adjectivesBySubmissionAnswerId,
        Map<ResultQuadrantType, QuadrantNarrative> quadrants
) {
    public record Overview(
            String keyword,
            String analysisTitle,
            String analysisBody,
            String tip
    ) {
    }

    public record QuadrantNarrative(
            String definitionKeyword,
            List<String> adjectiveKeywords,
            String interpretation,
            String imagePrompt
    ) {
    }
}
