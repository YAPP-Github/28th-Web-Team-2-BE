package com.looky.result.application;

import com.looky.result.domain.QuadrantWorkStatus;
import com.looky.result.domain.ResultQuadrantType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultQuadrantRecordTest {

    @Test
    void representsNarrativeReadyQuadrantWork() {
        ResultQuadrantRecord quadrant = new ResultQuadrantRecord(
                ResultQuadrantType.BLIND,
                null,
                "타인이 먼저 발견하는 강점",
                "abstract illustration",
                null,
                QuadrantWorkStatus.NARRATIVE_READY,
                0
        );

        assertEquals("abstract illustration", quadrant.imagePrompt());
        assertEquals(QuadrantWorkStatus.NARRATIVE_READY, quadrant.workStatus());
        assertEquals(0, quadrant.attemptCount());
    }
}
