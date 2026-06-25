package com.looky.api.survey.dto;

import com.looky.result.application.ResultOverviewRecord;

public record OverallResultResponse(
        String keyword,
        String analysisTitle,
        String analysisBody,
        String tip
) {
    static OverallResultResponse from(ResultOverviewRecord overview) {
        return overview == null ? null : new OverallResultResponse(
                overview.keyword(),
                overview.analysisTitle(),
                overview.analysisBody(),
                overview.tip()
        );
    }
}
