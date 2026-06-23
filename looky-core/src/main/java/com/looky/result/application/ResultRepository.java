package com.looky.result.application;

import java.util.Optional;

public interface ResultRepository {
    Optional<ResultRecord> findBySurveyId(Long surveyId);
}
