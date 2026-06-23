package com.looky.api.survey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "설문 생성 요청")
public record CreateSurveyRequest(
        @Schema(description = "개설자 닉네임. 앞뒤 공백은 제거되며 1자 이상 10자 이하입니다.", example = "만두")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
        String userNickname
) {
    public CreateSurveyRequest {
        if (userNickname != null) {
            userNickname = userNickname.trim();
        }
    }
}
