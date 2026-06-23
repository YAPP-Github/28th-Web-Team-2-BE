package com.looky.submission.persistence;

import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissionJpaRepository extends JpaRepository<SubmissionJpaEntity, Long> {
    boolean existsBySurvey_IdAndSubmitterType(Long surveyId, SubmitterType submitterType);

    boolean existsBySurvey_IdAndSubmitterTypeAndSubmissionStatus(Long surveyId, SubmitterType submitterType, SubmissionStatus submissionStatus);

    long countBySurvey_IdAndSubmitterTypeAndSubmissionStatus(Long surveyId, SubmitterType submitterType, SubmissionStatus submissionStatus);

    Optional<SubmissionJpaEntity> findByIdAndSubmissionStatus(Long id, SubmissionStatus submissionStatus);
}
