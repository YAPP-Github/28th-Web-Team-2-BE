package com.looky.submission.application;

import java.util.Set;

public record SubmissionQuestionRecord(
        Long submissionQuestionId,
        Long questionId,
        Set<Long> answerOptionIds
) {
}
