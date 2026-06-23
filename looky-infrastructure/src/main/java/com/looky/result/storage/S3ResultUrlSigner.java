package com.looky.result.storage;

import com.looky.result.application.ResultUrlSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@Profile("!test")
public class S3ResultUrlSigner implements ResultUrlSigner {
    private final S3Presigner s3Presigner; private final String bucket; private final Duration ttl;
    public S3ResultUrlSigner(S3Presigner s3Presigner, @Value("${looky.result-generation.s3.bucket}") String bucket, @Value("${looky.result-generation.presigned-url-ttl}") Duration ttl) { this.s3Presigner = s3Presigner; this.bucket = bucket; this.ttl = ttl; }
    public String sign(String objectKey) {
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey).build()).build()).url().toString();
    }
}
