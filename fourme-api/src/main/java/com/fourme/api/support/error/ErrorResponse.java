package com.fourme.api.support.error;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> details
) {

    public static ErrorResponse of(int status, ApiErrorCode code, String message, String path) {
        return of(status, code, message, path, Map.of());
    }

    public static ErrorResponse of(
            int status,
            ApiErrorCode code,
            String message,
            String path,
            Map<String, String> details
    ) {
        return new ErrorResponse(
                Instant.now(),
                status,
                code.name(),
                message,
                path,
                Map.copyOf(details)
        );
    }
}
