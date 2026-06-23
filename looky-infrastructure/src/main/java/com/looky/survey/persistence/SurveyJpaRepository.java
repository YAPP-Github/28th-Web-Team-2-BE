package com.looky.survey.persistence;

import com.looky.survey.domain.ResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SurveyJpaRepository extends JpaRepository<SurveyJpaEntity, Long> {
    Optional<SurveyJpaEntity> findBySurveyCode(String surveyCode);

    List<SurveyJpaEntity> findByResultStatusInAndResultAvailableAtLessThanEqual(
            Collection<ResultStatus> resultStatuses,
            OffsetDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SurveyJpaEntity survey
            set survey.resultStatus = :nextStatus
            where survey.id = :surveyId
              and survey.resultStatus in (:currentStatuses)
            """)
    int updateResultStatusWhenCurrentStatusIn(
            @Param("surveyId") Long surveyId,
            @Param("nextStatus") ResultStatus nextStatus,
            @Param("currentStatuses") Collection<ResultStatus> currentStatuses
    );
}
