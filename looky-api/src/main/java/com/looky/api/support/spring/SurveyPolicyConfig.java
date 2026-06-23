package com.looky.api.support.spring;

import com.looky.survey.application.SurveyPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SurveyPolicyConfig {

    @Bean
    public SurveyPolicy surveyPolicy(@Value("${looky.survey.result-open-delay-hours:24}") long resultOpenDelayHours) {
        return new SurveyPolicy(Duration.ofHours(resultOpenDelayHours));
    }
}
