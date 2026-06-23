package com.looky.result.application;

public record ResultGenerationPolicy(int maxAttempts) {

    public ResultGenerationPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
    }
}
