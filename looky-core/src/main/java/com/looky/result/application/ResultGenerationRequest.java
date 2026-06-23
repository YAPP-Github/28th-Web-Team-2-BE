package com.looky.result.application;

import com.looky.survey.application.SurveyRecord;

/**
 * Stable input boundary for a future AI prompt builder and image-storage client.
 * Additional answer summaries and storage options can be added here without
 * coupling the generator client to persistence entities.
 */
public record ResultGenerationRequest(
        SurveyRecord survey,
        long completedPeerSubmissionCount
) {
}
