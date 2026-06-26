package com.looky.api.survey.dto;

import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.domain.ResultStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record SurveyResultResponse(
        String surveyCode,
        ResultStatus resultStatus,
        Map<String, String> quadrantImageUrls,
        Map<String, String> quadrantInterpretations,
        OverallResultResponse overall,
        String overallKeyword,
        String overallAnalysis,
        String actionTip,
        Map<String, QuadrantResultResponse> quadrants
) {
    public static SurveyResultResponse from(SurveyResultResult result) {
        OverallResultResponse overall = OverallResultResponse.from(result.overall());
        return new SurveyResultResponse(
                result.surveyCode(),
                result.resultStatus(),
                result.quadrantImageUrls(),
                result.quadrantInterpretations(),
                overall,
                overall == null ? null : overall.keyword(),
                overall == null ? null : overall.analysisBody(),
                overall == null ? null : overall.tip(),
                result.quadrants() == null ? null : result.quadrants().entrySet().stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> QuadrantResultResponse.from(entry.getValue()),
                                        (left, right) -> left,
                                        LinkedHashMap::new
                                ),
                                Collections::unmodifiableMap
                        ))
        );
    }
}
