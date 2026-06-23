package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;

import java.util.List;
import java.util.Map;

public record ResultNarrative(
        Map<Long, List<String>> adjectivesBySubmissionAnswerId,
        Map<ResultQuadrantType, QuadrantNarrative> quadrants
) {
    public record QuadrantNarrative(String interpretation, String imagePrompt) {
    }
}
