package com.looky.result.persistence;

import com.looky.result.application.ResultAnswerAdjectiveRecord;
import com.looky.result.application.ResultGenerationSourceReader;
import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.persistence.SubmissionAnswerJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class ResultGenerationSourceReaderImpl implements ResultGenerationSourceReader {

    private final SubmissionAnswerJpaRepository submissionAnswerJpaRepository;

    public ResultGenerationSourceReaderImpl(SubmissionAnswerJpaRepository submissionAnswerJpaRepository) {
        this.submissionAnswerJpaRepository = submissionAnswerJpaRepository;
    }

    @Override
    public List<ResultAnswerAdjectiveRecord> readCompletedAnswers(Long surveyId) {
        return submissionAnswerJpaRepository.findCompletedAnswersBySurveyId(surveyId, SubmissionStatus.COMPLETED).stream()
                .map(answer -> new ResultAnswerAdjectiveRecord(
                        answer.getId(),
                        answer.getSubmissionQuestion().getQuestionId(),
                        answer.getSubmissionQuestion().getQuestion().getTraitCode(),
                        answer.getSubmissionQuestion().getQuestionContentSnapshot(),
                        answer.getAnswerContentSnapshot(),
                        List.of()
                ))
                .toList();
    }
}
