package com.looky.result.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3ResultStorageConfig {

    @Bean
    S3Client resultS3Client(@Value("${looky.result-generation.s3.region}") String region) {
        return S3Client.builder().region(Region.of(region)).credentialsProvider(DefaultCredentialsProvider.create()).build();
    }

    @Bean
    S3Presigner resultS3Presigner(@Value("${looky.result-generation.s3.region}") String region) {
        return S3Presigner.builder().region(Region.of(region)).credentialsProvider(DefaultCredentialsProvider.create()).build();
    }
}
