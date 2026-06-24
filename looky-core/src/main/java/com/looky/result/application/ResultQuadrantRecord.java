package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;
import com.looky.result.domain.QuadrantWorkStatus;

import java.util.List;

public record ResultQuadrantRecord(
        ResultQuadrantType quadrantType,
        String imageUrl,
        String interpretation,
        String imagePrompt,
        String s3ObjectKey,
        QuadrantWorkStatus workStatus,
        int attemptCount,
        String definitionKeyword,
        List<String> adjectiveKeywords
) {
    public ResultQuadrantRecord(
            ResultQuadrantType quadrantType,
            String imageUrl,
            String interpretation,
            String imagePrompt,
            String s3ObjectKey,
            QuadrantWorkStatus workStatus,
            int attemptCount
    ) {
        this(quadrantType, imageUrl, interpretation, imagePrompt, s3ObjectKey, workStatus, attemptCount, null, List.of());
    }

    public ResultQuadrantRecord(ResultQuadrantType quadrantType, String imageUrl) {
        this(quadrantType, imageUrl, null, null, null, QuadrantWorkStatus.IMAGE_READY, 0, null, List.of());
    }

    public ResultQuadrantRecord(ResultQuadrantType quadrantType, String imageUrl, String interpretation, String s3ObjectKey) {
        this(quadrantType, imageUrl, interpretation, null, s3ObjectKey, QuadrantWorkStatus.IMAGE_READY, 0, null, List.of());
    }
}
