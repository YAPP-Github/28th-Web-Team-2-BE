package com.looky.result.persistence;

import com.looky.result.application.ResultAnswerAdjectiveRecord;
import com.looky.result.application.ResultGenerationSourceReader;
import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;
import com.looky.submission.persistence.SubmissionAnswerJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional(readOnly = true)
public class ResultGenerationSourceReaderImpl implements ResultGenerationSourceReader {

    private final SubmissionAnswerJpaRepository submissionAnswerJpaRepository;

    public ResultGenerationSourceReaderImpl(SubmissionAnswerJpaRepository submissionAnswerJpaRepository) {
        this.submissionAnswerJpaRepository = submissionAnswerJpaRepository;
    }

    @Override
    public List<ResultAnswerAdjectiveRecord> readCompletedAnswers(Long surveyId) {
        Map<Long, String> labelsBySubmissionId = new LinkedHashMap<>();
        return submissionAnswerJpaRepository.findCompletedAnswersBySurveyId(surveyId, SubmissionStatus.COMPLETED).stream()
                .map(answer -> new ResultAnswerAdjectiveRecord(
                        answer.getId(),
                        answer.getSubmissionQuestion().getQuestionId(),
                        answer.getSubmissionQuestion().getSubmission().getSubmitterType(),
                        labelsBySubmissionId.computeIfAbsent(
                                answer.getSubmissionQuestion().getSubmission().getId(),
                                ignored -> labelFor(answer.getSubmissionQuestion().getSubmission().getSubmitterType(), labelsBySubmissionId)
                        ),
                        answer.getSubmissionQuestion().getQuestion().getTraitCode(),
                        answer.getSubmissionQuestion().getQuestionContentSnapshot(),
                        answer.getAnswerContentSnapshot(),
                        List.of()
                ))
                .toList();
    }

    private static String labelFor(SubmitterType submitterType, Map<Long, String> labelsBySubmissionId) {
        if (submitterType == SubmitterType.SELF) {
            return "본인";
        }
        long peerCount = labelsBySubmissionId.values().stream().filter(label -> label.startsWith("친구 ")).count();
        return "친구 " + (peerCount + 1);
    }
}
