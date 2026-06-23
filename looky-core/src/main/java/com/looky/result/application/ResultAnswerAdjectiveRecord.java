package com.looky.result.application;

import com.looky.question.domain.TraitCode;

import java.util.List;

public record ResultAnswerAdjectiveRecord(
        Long submissionAnswerId,
        Long questionId,
        TraitCode traitCode,
        String questionSnapshot,
        String answerSnapshot,
        List<String> adjectives
) {
}
