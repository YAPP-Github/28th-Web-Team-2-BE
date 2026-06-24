package com.looky.survey.application;

import com.looky.submission.application.SubmissionRepository;
import com.looky.survey.domain.ResultStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class ResultStatusResolver {

    private final SubmissionRepository submissionRepository;
    private final Clock clock;

    public ResultStatus resolve(SurveyRecord survey) {
        if (isTerminalStatus(survey.resultStatus())) {
            return survey.resultStatus();
        }

        if (!submissionRepository.existsCompletedSelfSubmission(survey.id())) {
            return ResultStatus.WAITING_SELF_RESPONSE;
        }
        if (submissionRepository.countCompletedPeerSubmissions(survey.id()) < survey.requiredPeerSubmissionCount()) {
            return ResultStatus.COLLECTING_PEER_RESPONSES;
        }
        if (OffsetDateTime.now(clock).isBefore(survey.resultAvailableAt())) {
            return ResultStatus.WAITING_RESULT_OPEN_TIME;
        }
        return ResultStatus.GENERATING;
    }

    private boolean isTerminalStatus(ResultStatus resultStatus) {
        return resultStatus == ResultStatus.GENERATING
                || resultStatus == ResultStatus.READY
                || resultStatus == ResultStatus.FAILED
                || resultStatus == ResultStatus.EXPIRED;
    }
}
