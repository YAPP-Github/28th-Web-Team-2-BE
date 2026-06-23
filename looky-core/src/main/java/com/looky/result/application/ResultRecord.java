package com.looky.result.application;

import java.util.List;

public record ResultRecord(
        Long id,
        Long surveyId,
        List<ResultQuadrantRecord> quadrants
) {
}
