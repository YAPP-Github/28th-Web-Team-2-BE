package com.looky.result.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ResultRepository {
    Optional<ResultRecord> findBySurveyId(Long surveyId);

    boolean existsBySurveyId(Long surveyId);

    void saveResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now);
}
