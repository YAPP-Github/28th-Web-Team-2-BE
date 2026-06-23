package com.looky.result.persistence;

import com.looky.result.application.ResultQuadrantRecord;
import com.looky.result.application.ResultRecord;
import com.looky.result.application.ResultRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ResultRepositoryImpl implements ResultRepository {

    private final ResultJpaRepository resultJpaRepository;
    private final ResultQuadrantJpaRepository resultQuadrantJpaRepository;

    public ResultRepositoryImpl(
            ResultJpaRepository resultJpaRepository,
            ResultQuadrantJpaRepository resultQuadrantJpaRepository
    ) {
        this.resultJpaRepository = resultJpaRepository;
        this.resultQuadrantJpaRepository = resultQuadrantJpaRepository;
    }

    @Override
    public Optional<ResultRecord> findBySurveyId(Long surveyId) {
        return resultJpaRepository.findBySurvey_Id(surveyId)
                .map(this::toRecord);
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
