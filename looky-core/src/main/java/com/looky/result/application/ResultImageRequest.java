package com.looky.result.application;

import java.util.List;

public record ResultImageRequest(
        String imagePrompt,
        List<String> referenceAssetKeys
) {
    public ResultImageRequest {
        referenceAssetKeys = List.copyOf(referenceAssetKeys);
    }
}
