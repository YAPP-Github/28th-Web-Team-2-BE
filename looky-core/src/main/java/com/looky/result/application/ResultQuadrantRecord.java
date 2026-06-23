package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;

public record ResultQuadrantRecord(
        ResultQuadrantType quadrantType,
        String imageUrl
) {
}
