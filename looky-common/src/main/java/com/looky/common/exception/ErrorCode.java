package com.looky.common.exception;

public enum ErrorCode {
    VALIDATION_ERROR(400, "요청값이 올바르지 않습니다."),
    INVALID_SURVEY_CODE(404, "유효하지 않은 설문 링크입니다."),
    SURVEY_NOT_FOUND(404, "설문을 찾을 수 없습니다."),
    SURVEY_NOT_COLLECTING(409, "현재 응답을 받을 수 없는 설문입니다."),
    RESULT_NOT_READY(409, "결과가 아직 준비되지 않았습니다."),
    RESULT_GENERATION_FAILED(409, "결과 생성에 실패했습니다."),
    SUBMISSION_NOT_FOUND(404, "제출 정보를 찾을 수 없습니다."),
    SUBMISSION_ALREADY_COMPLETED(409, "이미 완료된 응답입니다."),
    SELF_SUBMISSION_ALREADY_COMPLETED(409, "이미 본인 응답을 완료했습니다."),
    INVALID_ANSWER_COUNT(400, "답변 개수가 올바르지 않습니다."),
    QUESTION_NOT_IN_SUBMISSION(400, "해당 제출에 배정된 질문이 아닙니다."),
    INVALID_ANSWER_OPTION(400, "해당 질문의 선택지가 아닙니다."),
    DUPLICATED_QUESTION(400, "중복된 질문이 포함되어 있습니다."),
    ANSWER_REQUIRED(400, "모든 질문에 답변해야 합니다."),
    NOT_ENOUGH_ACTIVE_QUESTIONS(400, "활성화된 질문이 부족합니다."),
    INTERNAL_SERVER_ERROR(500, "서버에서 예상치 못한 문제가 발생했습니다.");

    private final int httpStatus;
    private final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
