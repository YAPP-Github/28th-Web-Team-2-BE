package com.looky.survey.application.dto;

import java.util.List;

public record QuestionResult(
        Long questionId,
        int sequence,
        String content,
        List<AnswerOptionResult> options
) {
    public record AnswerOptionResult(Long answerOptionId, int sequence, String content) {
    }
}
