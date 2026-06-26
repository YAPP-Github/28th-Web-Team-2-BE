package com.looky.survey.application;

import java.time.Duration;

public record SurveyPolicy(
        Duration resultOpenDelay,
        int requiredPeerSubmissionCount,
        int questionCountPerTrait
) {

    public SurveyPolicy {
        if (resultOpenDelay.isNegative()) {
            throw new IllegalArgumentException("resultOpenDelay must be zero or positive");
        }
        if (requiredPeerSubmissionCount < 1) {
            throw new IllegalArgumentException("requiredPeerSubmissionCount must be positive");
        }
        if (questionCountPerTrait < 1) {
            throw new IllegalArgumentException("questionCountPerTrait must be positive");
        }
    }
}
