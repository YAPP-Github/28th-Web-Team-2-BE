package com.looky.api.survey.dto;

import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.SubmitAnswersCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAnswersRequest(
        @NotEmpty(message = "답변은 필수입니다.")
        List<@Valid AnswerRequest> answers
) {
    public SubmitAnswersCommand toCommand() {
        return new SubmitAnswersCommand(answers.stream()
                .map(answer -> new AnswerCommand(answer.questionId(), answer.answerOptionId()))
                .toList());
    }

    public record AnswerRequest(
            @NotNull(message = "질문 ID는 필수입니다.")
            Long questionId,
            @NotNull(message = "선택지 ID는 필수입니다.")
            Long answerOptionId
    ) {
    }
}
