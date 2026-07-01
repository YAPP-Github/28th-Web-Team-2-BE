package com.looky.survey.persistence;

import com.looky.result.domain.ResultGenerationPhase;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.domain.ResultStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class SurveyRepositoryImpl implements SurveyRepository {

    private static final List<ResultStatus> RESULT_GENERATION_CANDIDATE_STATUSES = List.of(
            ResultStatus.WAITING_SELF_RESPONSE,
            ResultStatus.COLLECTING_PEER_RESPONSES,
            ResultStatus.WAITING_RESULT_OPEN_TIME,
            ResultStatus.GENERATING
    );

    private final SurveyJpaRepository surveyJpaRepository;

    public SurveyRepositoryImpl(SurveyJpaRepository surveyJpaRepository) {
        this.surveyJpaRepository = surveyJpaRepository;
    }

    @Override
    public SurveyRecord saveNewSurvey(
            String userNickname,
            String surveyCode,
            int requiredPeerSubmissionCount,
            OffsetDateTime now,
            OffsetDateTime resultAvailableAt,
            String characterPackKey,
            String characterPackVersion
    ) {
        SurveyJpaEntity entity = new SurveyJpaEntity(
                userNickname,
                surveyCode,
                requiredPeerSubmissionCount,
                now,
                resultAvailableAt,
                characterPackKey,
                characterPackVersion
        );
        return toRecord(surveyJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SurveyRecord> findById(Long surveyId) {
        return surveyJpaRepository.findById(surveyId).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SurveyRecord> findBySurveyCode(String surveyCode) {
        return surveyJpaRepository.findBySurveyCode(surveyCode).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyRecord> findResultGenerationCandidates(OffsetDateTime now) {
        return surveyJpaRepository.findByResultStatusIn(RESULT_GENERATION_CANDIDATE_STATUSES)
                .stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    public void markCollecting(Long surveyId) {
        SurveyJpaEntity entity = surveyJpaRepository.findById(surveyId).orElseThrow();
        entity.markCollecting();
    }

    @Override
    public boolean markGenerating(Long surveyId, int maxAttempts) {
        return surveyJpaRepository.updateResultStatusWhenCurrentStatusIn(
                surveyId,
                ResultStatus.GENERATING,
                RESULT_GENERATION_CANDIDATE_STATUSES,
                maxAttempts
        ) == 1;
    }

    @Override
    public void updateResultStatus(Long surveyId, ResultStatus resultStatus) {
        SurveyJpaEntity entity = surveyJpaRepository.findById(surveyId).orElseThrow();
        entity.updateResultStatus(resultStatus);
    }

    @Override
    public void syncResultStatus(Long surveyId, ResultStatus resultStatus) {
        surveyJpaRepository.syncResultStatusWhenCurrentStatusIn(
                surveyId,
                resultStatus,
                RESULT_GENERATION_CANDIDATE_STATUSES
        );
    }

    @Override
    public void updateGenerationPhase(Long surveyId, ResultGenerationPhase generationPhase) {
        SurveyJpaEntity entity = surveyJpaRepository.findById(surveyId).orElseThrow();
        entity.updateGenerationPhase(generationPhase);
    }

    private SurveyRecord toRecord(SurveyJpaEntity entity) {
        return new SurveyRecord(
                entity.getId(),
                entity.getUserNickname(),
                entity.getSurveyCode(),
                entity.getSurveyStatus(),
                entity.getResultStatus(),
                entity.getResultGenerationAttemptCount(),
                entity.getRequiredPeerSubmissionCount(),
                entity.getResultAvailableAt(),
                entity.getCreatedAt(),
                entity.getGenerationPhase(),
                entity.getCharacterPackKey(),
                entity.getCharacterPackVersion()
        );
    }
}
