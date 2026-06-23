package com.looky.question.application;

import java.util.List;

public record QuestionRecord(
        Long questionId,
        String content,
        List<AnswerOptionRecord> options
) {
    public record AnswerOptionRecord(Long answerOptionId, int sequence, String content) {
    }
}
