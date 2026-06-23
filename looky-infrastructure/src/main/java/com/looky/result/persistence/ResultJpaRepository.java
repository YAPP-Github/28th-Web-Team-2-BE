package com.looky.result.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultJpaRepository extends JpaRepository<ResultJpaEntity, Long> {
    Optional<ResultJpaEntity> findBySurvey_Id(Long surveyId);
}
