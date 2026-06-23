package com.looky.result.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuadrantWorkStatusTest {

    @Test
    void definesAllQuadrantWorkStates() throws Exception {
        Class<?> statusType = Class.forName("com.looky.result.domain.QuadrantWorkStatus");

        assertNotNull(Enum.valueOf((Class) statusType, "PENDING"));
        assertNotNull(Enum.valueOf((Class) statusType, "NARRATIVE_READY"));
        assertNotNull(Enum.valueOf((Class) statusType, "IMAGE_READY"));
        assertNotNull(Enum.valueOf((Class) statusType, "FAILED"));
    }
}
