package com.looky.survey.application;

import com.looky.survey.domain.ResultStatus;

import java.time.OffsetDateTime;
import java.util.List;
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

    List<SurveyRecord> findResultGenerationCandidates(OffsetDateTime now);

    void markCollecting(Long surveyId);

    void updateResultStatus(Long surveyId, ResultStatus resultStatus);
}
