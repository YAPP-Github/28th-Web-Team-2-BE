package com.looky.survey.application;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SurveyRepository {
    SurveyRecord saveNewSurvey(
            String userNickname,
            String surveyCode,
            int requiredPeerSubmissionCount,
            OffsetDateTime now,
            OffsetDateTime resultAvailableAt
    );

    Optional<SurveyRecord> findBySurveyCode(String surveyCode);

    void markCollecting(Long surveyId);
}
