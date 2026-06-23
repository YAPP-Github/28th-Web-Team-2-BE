package com.looky.api.survey.dto;

import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.application.dto.SubmissionCompletedResult;

import java.time.OffsetDateTime;

public record SubmissionCompletedResponse(
        Long submissionId,
        SubmitterType submitterType,
        SubmissionStatus submissionStatus,
        OffsetDateTime submittedAt
) {
    public static SubmissionCompletedResponse from(SubmissionCompletedResult result) {
        return new SubmissionCompletedResponse(
                result.submissionId(),
                result.submitterType(),
                result.submissionStatus(),
                result.submittedAt()
        );
    }
}
