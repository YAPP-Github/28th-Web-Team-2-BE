package com.looky.survey.application;

import com.looky.survey.domain.ResultStatus;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
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

    /**
     * Claim a survey for generation by moving it to GENERATING when still eligible.
     */
    boolean markGenerating(Long surveyId, int maxAttempts);

    void updateResultStatus(Long surveyId, ResultStatus resultStatus);
}
