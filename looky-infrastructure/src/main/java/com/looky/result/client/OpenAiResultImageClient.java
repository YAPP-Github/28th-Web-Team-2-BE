package com.looky.result.client;

import com.looky.result.application.ResultImageClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.images.ImageGenerateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@Profile("!test")
public class OpenAiResultImageClient implements ResultImageClient {

    private final String imageModel;
    private final ImageGenerateParams.Quality imageQuality;

    public OpenAiResultImageClient(
            @Value("${looky.result-generation.image-model}") String imageModel,
            @Value("${looky.result-generation.image-quality}") String imageQuality
    ) {
        this.imageModel = imageModel;
        this.imageQuality = ImageGenerateParams.Quality.of(imageQuality);
    }

    @Override
    public byte[] generate(String imagePrompt) {
        String image = OpenAIOkHttpClient.fromEnv().images().generate(ImageGenerateParams.builder()
                        .model(imageModel)
                        .quality(imageQuality)
                        .outputFormat(ImageGenerateParams.OutputFormat.PNG)
                        .n(1)
                        .prompt(imagePrompt)
                        .build())
                .data().orElseThrow(() -> new IllegalArgumentException("OpenAI image response is missing data"))
                .stream()
                .findFirst()
                .flatMap(response -> response.b64Json())
                .orElseThrow(() -> new IllegalArgumentException("OpenAI image response is missing PNG data"));
        return Base64.getDecoder().decode(image);
    }
}
