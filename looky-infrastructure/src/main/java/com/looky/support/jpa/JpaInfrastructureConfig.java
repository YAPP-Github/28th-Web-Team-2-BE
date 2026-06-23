package com.looky.support.jpa;

import com.looky.question.persistence.QuestionJpaEntity;
import com.looky.question.persistence.QuestionJpaRepository;
import com.looky.result.persistence.ResultJpaEntity;
import com.looky.result.persistence.ResultJpaRepository;
import com.looky.submission.persistence.SubmissionJpaEntity;
import com.looky.submission.persistence.SubmissionJpaRepository;
import com.looky.survey.persistence.SurveyJpaEntity;
import com.looky.survey.persistence.SurveyJpaRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackageClasses = {QuestionJpaEntity.class, SurveyJpaEntity.class, SubmissionJpaEntity.class, ResultJpaEntity.class})
@EnableJpaRepositories(basePackageClasses = {QuestionJpaRepository.class, SurveyJpaRepository.class, SubmissionJpaRepository.class, ResultJpaRepository.class})
public class JpaInfrastructureConfig {
}
