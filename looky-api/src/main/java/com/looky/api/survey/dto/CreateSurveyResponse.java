package com.looky.api.survey.dto;

import com.looky.survey.application.dto.SurveyCreatedResult;
import com.looky.survey.domain.SurveyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "설문 생성 응답 payload")
public record CreateSurveyResponse(
        @Schema(description = "외부에서 사용하는 설문 코드", example = "b91k2p")
        String surveyCode,
        @Schema(description = "프론트 공유 URL", example = "https://looky.my/b91k2p")
        String shareUrl,
        @Schema(description = "trim된 개설자 닉네임", example = "만두")
        String userNickname,
        @Schema(description = "설문 상태", example = "DRAFT")
        SurveyStatus surveyStatus,
        @Schema(description = "결과 생성 가능 시각", example = "2026-06-24T03:00:00+09:00")
        OffsetDateTime resultAvailableAt,
        @Schema(description = "설문 생성 시각", example = "2026-06-23T03:00:00+09:00")
        OffsetDateTime createdAt
) {
    public static CreateSurveyResponse from(SurveyCreatedResult result) {
        String surveyCode = result.surveyCode();
        String userNickname = result.userNickname() == null ? null : result.userNickname().trim();
        return new CreateSurveyResponse(
                surveyCode,
                "https://looky.my/" + surveyCode,
                userNickname,
                result.surveyStatus(),
                result.resultAvailableAt(),
                result.createdAt()
        );
    }
}
