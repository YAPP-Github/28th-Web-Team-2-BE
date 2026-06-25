package com.looky.result.client;

import com.looky.result.application.ResultImageClient;
import com.looky.result.application.ResultImageRequest;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.images.ImageEditParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@Profile("!test & !local")
public class OpenAiResultImageClient implements ResultImageClient {

    private final S3Client s3Client;
    private final String bucket;
    private final String imageModel;
    private final ImageEditParams.Quality imageQuality;

    public OpenAiResultImageClient(
            S3Client s3Client,
            @Value("${looky.result-generation.s3.bucket}") String bucket,
            @Value("${looky.result-generation.image-model}") String imageModel,
            @Value("${looky.result-generation.image-quality}") String imageQuality
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.imageModel = imageModel;
        this.imageQuality = ImageEditParams.Quality.of(imageQuality);
    }

    @Override
    public byte[] generate(ResultImageRequest request) {
        List<ResponseInputStream<GetObjectResponse>> referenceImages = new ArrayList<>();
        try {
            for (String assetKey : request.referenceAssetKeys()) {
                referenceImages.add(s3Client.getObject(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(assetKey)
                        .build()));
            }
            String image = OpenAIOkHttpClient.fromEnv().images().edit(ImageEditParams.builder()
                            .model(imageModel)
                            .quality(imageQuality)
                            .outputFormat(ImageEditParams.OutputFormat.PNG)
                            .n(1)
                            .prompt(request.imagePrompt())
                            .imageOfInputStreams(referenceImages.stream().map(InputStream.class::cast).toList())
                            .build())
                    .data().orElseThrow(() -> new IllegalArgumentException("OpenAI image response is missing data"))
                    .stream()
                    .findFirst()
                    .flatMap(response -> response.b64Json())
                    .orElseThrow(() -> new IllegalArgumentException("OpenAI image response is missing PNG data"));
            return Base64.getDecoder().decode(image);
        } finally {
            for (ResponseInputStream<GetObjectResponse> referenceImage : referenceImages) {
                try {
                    referenceImage.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
