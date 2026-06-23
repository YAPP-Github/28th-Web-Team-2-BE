package com.looky.result.persistence;

import com.looky.result.application.ResultQuadrantRecord;
import com.looky.result.application.ResultAnswerAdjectiveRecord;
import com.looky.result.application.ResultNarrative;
import com.looky.result.application.ResultRecord;
import com.looky.result.application.ResultRepository;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.persistence.SurveyJpaEntity;
import com.looky.survey.persistence.SurveyJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.looky.result.domain.ResultQuadrantType;

@Repository
@Transactional(readOnly = true)
public class ResultRepositoryImpl implements ResultRepository {

    private final ResultJpaRepository resultJpaRepository;
    private final ResultQuadrantJpaRepository resultQuadrantJpaRepository;
    private final ResultAnswerAdjectiveJpaRepository resultAnswerAdjectiveJpaRepository;
    private final SurveyJpaRepository surveyJpaRepository;

    public ResultRepositoryImpl(
            ResultJpaRepository resultJpaRepository,
            ResultQuadrantJpaRepository resultQuadrantJpaRepository,
            ResultAnswerAdjectiveJpaRepository resultAnswerAdjectiveJpaRepository,
            SurveyJpaRepository surveyJpaRepository
    ) {
        this.resultJpaRepository = resultJpaRepository;
        this.resultQuadrantJpaRepository = resultQuadrantJpaRepository;
        this.resultAnswerAdjectiveJpaRepository = resultAnswerAdjectiveJpaRepository;
        this.surveyJpaRepository = surveyJpaRepository;
    }

    @Override
    public Optional<ResultRecord> findBySurveyId(Long surveyId) {
        return resultJpaRepository.findBySurvey_Id(surveyId)
                .map(this::toRecord);
    }

    @Override
    public boolean existsBySurveyId(Long surveyId) {
        return resultJpaRepository.existsBySurvey_Id(surveyId);
    }

    @Override
    public boolean hasNarrative(Long surveyId) {
        return resultJpaRepository.findBySurvey_Id(surveyId)
                .map(result -> !resultQuadrantJpaRepository.findByResult_Id(result.getId()).isEmpty())
                .orElse(false);
    }

    @Override
    @Transactional
    public void markQuadrantImageReady(Long surveyId, ResultQuadrantType quadrantType, String s3ObjectKey) {
        ResultQuadrantJpaEntity quadrant = findQuadrant(surveyId, quadrantType);
        quadrant.completeImage(null, s3ObjectKey);
    }

    @Override
    @Transactional
    public void markQuadrantImageFailed(Long surveyId, ResultQuadrantType quadrantType, String failureReason) {
        findQuadrant(surveyId, quadrantType).failImage(failureReason);
    }

    @Override
    @Transactional
    public void saveNarrative(Long surveyId, List<ResultAnswerAdjectiveRecord> answers, ResultNarrative narrative, OffsetDateTime now) {
        SurveyJpaEntity survey = surveyJpaRepository.findById(surveyId).orElseThrow();
        ResultJpaEntity result = resultJpaRepository.findBySurvey_Id(surveyId)
                .orElseGet(() -> resultJpaRepository.save(new ResultJpaEntity(survey, now)));
        resultAnswerAdjectiveJpaRepository.saveAll(answers.stream()
                .map(answer -> new ResultAnswerAdjectiveJpaEntity(result, answer, narrative.adjectivesBySubmissionAnswerId().get(answer.submissionAnswerId())))
                .toList());
        if (resultQuadrantJpaRepository.findByResult_Id(result.getId()).isEmpty()) {
            resultQuadrantJpaRepository.saveAll(narrative.quadrants().entrySet().stream()
                    .map(entry -> new ResultQuadrantJpaEntity(result, entry.getKey(), entry.getValue().interpretation(), entry.getValue().imagePrompt()))
                    .toList());
        }
    }

    @Override
    @Transactional
    public void saveReadyResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now) {
        SurveyJpaEntity survey = surveyJpaRepository.findById(surveyId).orElseThrow();
        ResultJpaEntity result = resultJpaRepository.findBySurvey_Id(surveyId)
                .orElseGet(() -> resultJpaRepository.save(new ResultJpaEntity(survey, now)));
        Map<com.looky.result.domain.ResultQuadrantType, ResultQuadrantJpaEntity> existing = resultQuadrantJpaRepository.findByResult_Id(result.getId()).stream()
                .collect(Collectors.toMap(ResultQuadrantJpaEntity::getQuadrantType, Function.identity()));
        for (ResultQuadrantRecord quadrant : quadrants) {
            ResultQuadrantJpaEntity entity = existing.get(quadrant.quadrantType());
            if (entity == null) {
                resultQuadrantJpaRepository.save(new ResultQuadrantJpaEntity(result, quadrant));
            } else {
                entity.completeImage(quadrant.imageUrl(), quadrant.s3ObjectKey());
            }
        }
        survey.updateResultStatus(ResultStatus.READY);
    }

    private ResultRecord toRecord(ResultJpaEntity entity) {
        return new ResultRecord(
                entity.getId(),
                entity.getSurvey().getId(),
                resultQuadrantJpaRepository.findByResult_Id(entity.getId()).stream()
                        .map(quadrant -> new ResultQuadrantRecord(
                                quadrant.getQuadrantType(),
                                quadrant.getImageUrl(),
                                quadrant.getInterpretation(),
                                quadrant.getImagePrompt(),
                                quadrant.getS3ObjectKey(),
                                quadrant.getWorkStatus(),
                                quadrant.getAttemptCount()
                        ))
                        .toList()
        );
    }

    private ResultQuadrantJpaEntity findQuadrant(Long surveyId, ResultQuadrantType quadrantType) {
        ResultJpaEntity result = resultJpaRepository.findBySurvey_Id(surveyId).orElseThrow();
        return resultQuadrantJpaRepository.findByResult_Id(result.getId()).stream()
                .filter(quadrant -> quadrant.getQuadrantType() == quadrantType)
                .findFirst()
                .orElseThrow();
    }
}
