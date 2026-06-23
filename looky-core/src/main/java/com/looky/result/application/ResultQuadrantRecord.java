package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;
import com.looky.result.domain.QuadrantWorkStatus;

public record ResultQuadrantRecord(
        ResultQuadrantType quadrantType,
        String imageUrl,
        String interpretation,
        String imagePrompt,
        String s3ObjectKey,
        QuadrantWorkStatus workStatus,
        int attemptCount
) {
    public ResultQuadrantRecord(ResultQuadrantType quadrantType, String imageUrl) {
        this(quadrantType, imageUrl, null, null, null, QuadrantWorkStatus.IMAGE_READY, 0);
    }

    public ResultQuadrantRecord(ResultQuadrantType quadrantType, String imageUrl, String interpretation, String s3ObjectKey) {
        this(quadrantType, imageUrl, interpretation, null, s3ObjectKey, QuadrantWorkStatus.IMAGE_READY, 0);
    }
}
