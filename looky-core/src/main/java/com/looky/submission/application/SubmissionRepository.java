package com.looky.submission.application;

import com.looky.question.application.QuestionRecord;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.SubmissionCompletedResult;
import com.looky.survey.application.dto.SubmissionStartedResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository {
    boolean existsSelfSubmission(Long surveyId);

    boolean existsCompletedSelfSubmission(Long surveyId);

    long countCompletedPeerSubmissions(Long surveyId);

    SubmissionStartedResult saveStartedSubmission(
            Long surveyId,
            String targetNickname,
            SubmitterType submitterType,
            String submitterKey,
            List<QuestionRecord> questions,
            OffsetDateTime now
    );

    Optional<SubmissionRecord> findInProgressSubmission(Long submissionId);

    SubmissionCompletedResult completeSubmission(Long submissionId, List<AnswerCommand> answers, OffsetDateTime now);
}
