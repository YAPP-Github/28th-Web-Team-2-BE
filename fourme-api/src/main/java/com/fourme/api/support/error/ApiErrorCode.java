package com.fourme.api.support.error;

public enum ApiErrorCode {

    INVALID_REQUEST("요청 형식이 올바르지 않습니다."),
    OWNER_TOKEN_REQUIRED("owner token이 필요합니다."),
    OWNER_TOKEN_INVALID("owner token이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND("요청한 리소스를 찾을 수 없습니다."),
    SELF_SURVEY_REQUIRED("self survey 완료 후 요청할 수 있습니다."),
    SELF_SURVEY_ALREADY_SUBMITTED("이미 제출된 self survey입니다."),
    NOT_ENOUGH_PEER_RESPONSES("peer response가 충분하지 않습니다."),
    RESULT_COOLDOWN_ACTIVE("결과 생성 cooldown이 진행 중입니다."),
    AI_EXTRACTION_FAILED("AI adjective extraction에 실패했습니다."),
    IMAGE_GENERATION_FAILED("이미지 생성에 실패했습니다."),
    STORAGE_FAILED("스토리지 처리에 실패했습니다."),
    COMPOSITION_FAILED("이미지 합성에 실패했습니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String defaultMessage;

    ApiErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
