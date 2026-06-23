package com.looky.api.support.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ErrorResponse(
        String errorCode,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldErrorPayload> errors
) {
    public static ErrorResponse of(String errorCode) {
        return new ErrorResponse(errorCode, List.of());
    }

    public static ErrorResponse of(String errorCode, List<FieldErrorPayload> errors) {
        return new ErrorResponse(errorCode, errors);
    }

    public record FieldErrorPayload(String field, String reason) {
    }
}
