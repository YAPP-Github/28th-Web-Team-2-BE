package com.looky.result.storage;

import com.looky.result.application.ResultImageStorage;
import com.looky.result.domain.ResultQuadrantType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@Profile("!test & !local")
public class S3ResultImageStorage implements ResultImageStorage {
    private final S3Client s3Client; private final String bucket;
    public S3ResultImageStorage(S3Client s3Client, @Value("${looky.result-generation.s3.bucket}") String bucket) { this.s3Client = s3Client; this.bucket = bucket; }
    public String upload(String surveyCode, ResultQuadrantType quadrantType, byte[] imageBytes) {
        String key = "surveys/" + surveyCode + "/results/" + quadrantType.name() + ".png";
        s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType("image/png").build(), RequestBody.fromBytes(imageBytes));
        return key;
    }
}
