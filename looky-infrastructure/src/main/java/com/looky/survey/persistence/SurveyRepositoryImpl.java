package com.looky.survey.persistence;

import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@Transactional
public class SurveyRepositoryImpl implements SurveyRepository {

    private final SurveyJpaRepository surveyJpaRepository;

    public SurveyRepositoryImpl(SurveyJpaRepository surveyJpaRepository) {
        this.surveyJpaRepository = surveyJpaRepository;
    }

    @Override
    public SurveyRecord saveNewSurvey(String userNickname, String surveyCode, int requiredPeerSubmissionCount, OffsetDateTime now, OffsetDateTime resultAvailableAt) {
        SurveyJpaEntity entity = new SurveyJpaEntity(userNickname, surveyCode, requiredPeerSubmissionCount, now, resultAvailableAt);
        return toRecord(surveyJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SurveyRecord> findBySurveyCode(String surveyCode) {
        return surveyJpaRepository.findBySurveyCode(surveyCode).map(this::toRecord);
    }

    @Override
    public void markCollecting(Long surveyId) {
        SurveyJpaEntity entity = surveyJpaRepository.findById(surveyId).orElseThrow();
        entity.markCollecting();
    }

    private SurveyRecord toRecord(SurveyJpaEntity entity) {
        return new SurveyRecord(
                entity.getId(),
                entity.getUserNickname(),
                entity.getSurveyCode(),
                entity.getSurveyStatus(),
                entity.getResultStatus(),
                entity.getRequiredPeerSubmissionCount(),
                entity.getResultAvailableAt(),
                entity.getCreatedAt()
        );
    }
}
