package com.looky.common.exception;

public class LookyException extends RuntimeException {

    private final ErrorCode errorCode;

    public LookyException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
