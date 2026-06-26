package com.looky.question.application;

import com.looky.question.domain.TraitCode;

import java.util.List;

public record QuestionRecord(
        Long questionId,
        TraitCode traitCode,
        String content,
        String contentTemplate,
        List<AnswerOptionRecord> options
) {
    public QuestionRecord(Long questionId, TraitCode traitCode, String content, List<AnswerOptionRecord> options) {
        this(questionId, traitCode, content, null, options);
    }

    public record AnswerOptionRecord(Long answerOptionId, int sequence, String content) {
    }
}
