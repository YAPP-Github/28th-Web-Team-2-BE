package com.looky.api.support.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "looky.result-generation")
public record ResultGenerationProperties(
        String narrativeModel,
        String imageModel,
        String imageQuality,
        Duration presignedUrlTtl,
        S3Properties s3
) {

    public record S3Properties(String bucket, String region) {
    }
}
