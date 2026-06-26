package com.looky.api.support.spring;

import com.looky.result.application.ResultGenerationPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(ResultGenerationProperties.class)
public class ResultGenerationPolicyConfig {

    @Bean
    public ResultGenerationPolicy resultGenerationPolicy(
            @Value("${looky.result-generation.max-attempts:3}") int maxAttempts
    ) {
        return new ResultGenerationPolicy(maxAttempts);
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService resultImageGenerationExecutor(
            @Value("${looky.result-generation.image-concurrency:4}") int imageConcurrency
    ) {
        return Executors.newFixedThreadPool(Math.max(1, imageConcurrency));
    }
}
