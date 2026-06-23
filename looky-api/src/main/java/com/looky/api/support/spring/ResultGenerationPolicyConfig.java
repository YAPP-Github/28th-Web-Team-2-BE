package com.looky.api.support.spring;

import com.looky.result.application.ResultGenerationPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResultGenerationProperties.class)
public class ResultGenerationPolicyConfig {

    @Bean
    public ResultGenerationPolicy resultGenerationPolicy(
            @Value("${looky.result-generation.max-attempts:3}") int maxAttempts
    ) {
        return new ResultGenerationPolicy(maxAttempts);
    }
}
