package com.looky.submission.application;

import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;

import java.util.List;

public record SubmissionRecord(
        Long id,
        Long surveyId,
        SubmitterType submitterType,
        SubmissionStatus submissionStatus,
        List<SubmissionQuestionRecord> questions
) {
}
