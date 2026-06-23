package com.looky.question.application;

import com.looky.question.domain.TraitCode;

import java.util.List;

public record QuestionRecord(
        Long questionId,
        TraitCode traitCode,
        String content,
        List<AnswerOptionRecord> options
) {
    public record AnswerOptionRecord(Long answerOptionId, int sequence, String content) {
    }
}
