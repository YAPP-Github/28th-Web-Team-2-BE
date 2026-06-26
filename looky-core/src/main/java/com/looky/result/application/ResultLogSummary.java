package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;

import java.util.EnumMap;
import java.util.Map;

public final class ResultLogSummary {

    private ResultLogSummary() {
    }

    public static Map<ResultQuadrantType, String> quadrantStatuses(ResultRecord result) {
        Map<ResultQuadrantType, String> statuses = new EnumMap<>(ResultQuadrantType.class);
        for (ResultQuadrantRecord quadrant : result.quadrants()) {
            statuses.put(quadrant.quadrantType(), quadrant.workStatus().name());
        }
        return Map.copyOf(statuses);
    }

    public static Map<ResultQuadrantType, String> selectedVariantKeys(ResultRecord result) {
        Map<ResultQuadrantType, String> keys = new EnumMap<>(ResultQuadrantType.class);
        for (ResultQuadrantRecord quadrant : result.quadrants()) {
            if (quadrant.selectedVariantKey() != null && !quadrant.selectedVariantKey().isBlank()) {
                keys.put(quadrant.quadrantType(), quadrant.selectedVariantKey());
            }
        }
        return Map.copyOf(keys);
    }
}
