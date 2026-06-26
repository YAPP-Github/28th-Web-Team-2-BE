package com.looky.api.support.spring;

import com.looky.survey.application.SurveyPolicy;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SurveyPolicyConfig {

    @Bean
    public SurveyPolicy surveyPolicy(
            @Value("${looky.survey.result-open-delay:}") String resultOpenDelay,
            @Value("${looky.survey.result-open-delay-hours:24}") long resultOpenDelayHours,
            @Value("${looky.survey.required-peer-submission-count:3}") int requiredPeerSubmissionCount,
            @Value("${looky.survey.question-count-per-trait:2}") int questionCountPerTrait
    ) {
        Duration delay = resultOpenDelay.isBlank()
                ? Duration.ofHours(resultOpenDelayHours)
                : DurationStyle.detectAndParse(resultOpenDelay);
        return new SurveyPolicy(delay, requiredPeerSubmissionCount, questionCountPerTrait);
    }
}
