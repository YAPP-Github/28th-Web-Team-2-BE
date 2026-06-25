package com.looky.api.support.spring;

import com.looky.survey.application.SurveyPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = SurveyPolicyConfig.class,
        properties = "looky.survey.result-open-delay=20m"
)
class SurveyPolicyConfigTest {

    @Autowired
    private SurveyPolicy surveyPolicy;

    @Test
    void surveyPolicyUsesDurationStyleDelayProperty() {
        assertEquals(Duration.ofMinutes(20), surveyPolicy.resultOpenDelay());
    }
}
