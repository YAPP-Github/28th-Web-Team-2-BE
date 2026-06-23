package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record GeneratedResult(
        Map<ResultQuadrantType, String> quadrantImageUrls
) {

    public List<ResultQuadrantRecord> toQuadrants() {
        List<ResultQuadrantRecord> quadrants = new ArrayList<>(ResultQuadrantType.values().length);
        for (ResultQuadrantType type : ResultQuadrantType.values()) {
            String imageUrl = quadrantImageUrls == null ? null : quadrantImageUrls.get(type);
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new IllegalArgumentException("Result quadrant image url is required: " + type);
            }
            quadrants.add(new ResultQuadrantRecord(type, imageUrl));
        }
        return quadrants;
    }
}
