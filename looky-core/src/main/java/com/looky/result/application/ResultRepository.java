package com.looky.result.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.looky.result.domain.QuadrantWorkStatus;
import com.looky.result.domain.ResultQuadrantType;

public interface ResultRepository {
    Optional<ResultRecord> findBySurveyId(Long surveyId);

    boolean existsBySurveyId(Long surveyId);

    boolean hasNarrative(Long surveyId);

    default List<ResultQuadrantRecord> findImageWorkCandidates(Long surveyId, int maxAttempts) {
        return findBySurveyId(surveyId)
                .map(ResultRecord::quadrants)
                .orElseGet(List::of)
                .stream()
                .filter(quadrant -> quadrant.workStatus() == QuadrantWorkStatus.NARRATIVE_READY
                        || (quadrant.workStatus() == QuadrantWorkStatus.FAILED && quadrant.attemptCount() < maxAttempts))
                .toList();
    }

    void markQuadrantImageReady(Long surveyId, ResultQuadrantType quadrantType, String s3ObjectKey, String selectedVariantKey);

    void markQuadrantImageFailed(Long surveyId, ResultQuadrantType quadrantType, String failureReason);

    void saveNarrative(
            Long surveyId,
            List<ResultAnswerAdjectiveRecord> answers,
            ResultNarrative narrative,
            OffsetDateTime now
    );

    /**
     * Persist result quadrants and mark the survey READY atomically.
     */
    void saveReadyResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now);
}
