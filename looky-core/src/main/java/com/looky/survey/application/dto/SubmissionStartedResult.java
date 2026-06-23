package com.looky.survey.application.dto;

import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;

import java.util.List;

public record SubmissionStartedResult(
        Long submissionId,
        SubmitterType submitterType,
        SubmissionStatus submissionStatus,
        String targetNickname,
        List<QuestionResult> questions
) {
}
