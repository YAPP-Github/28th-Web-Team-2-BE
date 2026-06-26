package com.looky.survey.persistence;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurveyJpaEntityTest {

    @Test
    void keepsKstWallClockTimeWhenMappingOffsetDateTimeFields() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-26T23:44:51.116211+09:00");
        OffsetDateTime resultAvailableAt = OffsetDateTime.parse("2026-06-27T00:04:51.116211+09:00");

        SurveyJpaEntity entity = new SurveyJpaEntity(
                "만두",
                "ABC123",
                3,
                now,
                resultAvailableAt,
                "pomang",
                "v1"
        );

        assertEquals(now, entity.getCreatedAt());
        assertEquals(resultAvailableAt, entity.getResultAvailableAt());
    }
}
