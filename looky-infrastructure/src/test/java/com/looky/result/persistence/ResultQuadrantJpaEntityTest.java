package com.looky.result.persistence;

import com.looky.result.domain.QuadrantWorkStatus;
import com.looky.result.domain.ResultQuadrantType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultQuadrantJpaEntityTest {

    @Test
    void narrativeDraftStartsReadyForImageGenerationWithoutAnImage() {
        var quadrant = new ResultQuadrantJpaEntity(
                null,
                ResultQuadrantType.BLIND,
                "타인이 먼저 발견하는 강점",
                "abstract illustration of a hidden strength"
        );

        assertEquals(QuadrantWorkStatus.NARRATIVE_READY, quadrant.getWorkStatus());
        assertEquals("타인이 먼저 발견하는 강점", quadrant.getInterpretation());
        assertEquals("abstract illustration of a hidden strength", quadrant.getImagePrompt());
        assertNull(quadrant.getImageUrl());
        assertNull(quadrant.getS3ObjectKey());
    }

    @Test
    void failedImageWorkIncrementsOnlyThatQuadrantsAttemptCount() {
        var quadrant = new ResultQuadrantJpaEntity(null, ResultQuadrantType.BLIND, "해석", "prompt");

        quadrant.failImage("image generation failed");

        assertEquals(QuadrantWorkStatus.FAILED, quadrant.getWorkStatus());
        assertEquals(1, quadrant.getAttemptCount());
        assertEquals("image generation failed", quadrant.getFailureReason());
    }
}
