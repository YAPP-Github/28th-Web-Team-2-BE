package com.looky.result.persistence;

import com.looky.result.application.ResultQuadrantRecord;
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

@Repository
@Transactional(readOnly = true)
public class ResultRepositoryImpl implements ResultRepository {

    private final ResultJpaRepository resultJpaRepository;
    private final ResultQuadrantJpaRepository resultQuadrantJpaRepository;
    private final SurveyJpaRepository surveyJpaRepository;

    public ResultRepositoryImpl(
            ResultJpaRepository resultJpaRepository,
            ResultQuadrantJpaRepository resultQuadrantJpaRepository,
            SurveyJpaRepository surveyJpaRepository
    ) {
        this.resultJpaRepository = resultJpaRepository;
        this.resultQuadrantJpaRepository = resultQuadrantJpaRepository;
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
    @Transactional
    public void saveReadyResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now) {
        SurveyJpaEntity survey = surveyJpaRepository.findById(surveyId).orElseThrow();
        ResultJpaEntity result = resultJpaRepository.save(new ResultJpaEntity(survey, now));
        resultQuadrantJpaRepository.saveAll(quadrants.stream()
                .map(quadrant -> new ResultQuadrantJpaEntity(
                        result,
                        quadrant.quadrantType(),
                        quadrant.imageUrl()
                ))
                .toList());
        survey.updateResultStatus(ResultStatus.READY);
    }

    private ResultRecord toRecord(ResultJpaEntity entity) {
        return new ResultRecord(
                entity.getId(),
                entity.getSurvey().getId(),
                resultQuadrantJpaRepository.findByResult_Id(entity.getId()).stream()
                        .map(quadrant -> new ResultQuadrantRecord(
                                quadrant.getQuadrantType(),
                                quadrant.getImageUrl()
                        ))
                        .toList()
        );
    }
}
