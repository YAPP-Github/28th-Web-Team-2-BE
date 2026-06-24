package com.looky.result.application;

import com.looky.question.domain.TraitCode;
import com.looky.submission.domain.SubmitterType;

import java.util.List;

public record ResultAnswerAdjectiveRecord(
        Long submissionAnswerId,
        Long questionId,
        SubmitterType submitterType,
        String respondentLabel,
        TraitCode traitCode,
        String questionSnapshot,
        String answerSnapshot,
        List<String> adjectives
) {
}
