package com.looky.survey.application;

import java.time.Duration;

public record SurveyPolicy(Duration resultOpenDelay) {

    public SurveyPolicy {
        if (resultOpenDelay.isNegative()) {
            throw new IllegalArgumentException("resultOpenDelay must be zero or positive");
        }
    }
}
