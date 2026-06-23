package com.looky.survey.domain;

public enum ResultStatus {
    WAITING_SELF_RESPONSE,
    COLLECTING_PEER_RESPONSES,
    WAITING_RESULT_OPEN_TIME,
    GENERATING,
    READY,
    FAILED,
    EXPIRED
}
