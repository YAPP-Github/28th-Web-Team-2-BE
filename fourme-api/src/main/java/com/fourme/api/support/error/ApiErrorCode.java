package com.fourme.api.support.error;

public enum ApiErrorCode {

    INVALID_REQUEST("요청 형식이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String defaultMessage;

    ApiErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
