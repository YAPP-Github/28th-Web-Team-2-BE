package com.looky.survey.persistence;

import com.looky.survey.domain.ResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SurveyJpaRepository extends JpaRepository<SurveyJpaEntity, Long> {
    Optional<SurveyJpaEntity> findBySurveyCode(String surveyCode);

    List<SurveyJpaEntity> findByResultStatusIn(Collection<ResultStatus> resultStatuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SurveyJpaEntity survey
            set survey.resultStatus = :nextStatus,
                survey.resultGenerationAttemptCount = survey.resultGenerationAttemptCount + 1
            where survey.id = :surveyId
              and survey.resultStatus in (:currentStatuses)
              and survey.resultGenerationAttemptCount < :maxAttempts
            """)
    int updateResultStatusWhenCurrentStatusIn(
            @Param("surveyId") Long surveyId,
            @Param("nextStatus") ResultStatus nextStatus,
            @Param("currentStatuses") Collection<ResultStatus> currentStatuses,
            @Param("maxAttempts") int maxAttempts
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SurveyJpaEntity survey
            set survey.resultStatus = :nextStatus
            where survey.id = :surveyId
              and survey.resultStatus in (:currentStatuses)
            """)
    int syncResultStatusWhenCurrentStatusIn(
            @Param("surveyId") Long surveyId,
            @Param("nextStatus") ResultStatus nextStatus,
            @Param("currentStatuses") Collection<ResultStatus> currentStatuses
    );
}
