package com.looky.survey.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyJpaRepository extends JpaRepository<SurveyJpaEntity, Long> {
    Optional<SurveyJpaEntity> findBySurveyCode(String surveyCode);
}
