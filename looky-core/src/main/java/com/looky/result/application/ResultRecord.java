package com.looky.result.application;

import java.util.List;

public record ResultRecord(
        Long id,
        Long surveyId,
        ResultOverviewRecord overview,
        List<ResultQuadrantRecord> quadrants
) {
    public ResultRecord(Long id, Long surveyId, List<ResultQuadrantRecord> quadrants) {
        this(id, surveyId, null, quadrants);
    }
}
