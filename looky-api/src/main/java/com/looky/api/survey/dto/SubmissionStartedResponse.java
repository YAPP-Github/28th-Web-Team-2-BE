package com.looky.api.survey.dto;

import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.application.dto.QuestionResult;
import com.looky.survey.application.dto.SubmissionStartedResult;

import java.util.List;

public record SubmissionStartedResponse(
        Long submissionId,
        SubmitterType submitterType,
        SubmissionStatus submissionStatus,
        String targetNickname,
        List<QuestionResponse> questions
) {
    public static SubmissionStartedResponse from(SubmissionStartedResult result) {
        return new SubmissionStartedResponse(
                result.submissionId(),
                result.submitterType(),
                result.submissionStatus(),
                result.targetNickname(),
                result.questions().stream().map(QuestionResponse::from).toList()
        );
    }

    public record QuestionResponse(
            Long questionId,
            int sequence,
            String content,
            List<AnswerOptionResponse> options
    ) {
        private static QuestionResponse from(QuestionResult result) {
            return new QuestionResponse(
                    result.questionId(),
                    result.sequence(),
                    result.content(),
                    result.options().stream().map(AnswerOptionResponse::from).toList()
            );
        }
    }

    public record AnswerOptionResponse(Long answerOptionId, int sequence, String content) {
        private static AnswerOptionResponse from(QuestionResult.AnswerOptionResult result) {
            return new AnswerOptionResponse(result.answerOptionId(), result.sequence(), result.content());
        }
    }
}
