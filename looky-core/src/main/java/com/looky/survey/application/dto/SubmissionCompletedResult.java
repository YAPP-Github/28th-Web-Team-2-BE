package com.looky.survey.application.dto;

import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;

import java.time.OffsetDateTime;

public record SubmissionCompletedResult(
        Long submissionId,
        SubmitterType submitterType,
        SubmissionStatus submissionStatus,
        OffsetDateTime submittedAt
) {
}
