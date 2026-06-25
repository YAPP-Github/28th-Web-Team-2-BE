package com.looky.characterpack.application;

import com.looky.result.domain.ResultQuadrantType;

public record CharacterPackVariantRecord(
        String variantKey,
        ResultQuadrantType quadrantType,
        String baseAssetKey,
        String assetKey
) {
}
