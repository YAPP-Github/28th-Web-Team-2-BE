package com.looky.result.client;

import com.looky.result.application.ResultImageClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("test")
public class TestResultImageClient implements ResultImageClient {

    private final boolean failBlindOnce;
    private boolean blindFailed;
    private final List<String> generatedPrompts = new ArrayList<>();

    public TestResultImageClient(@Value("${looky.test.fail-blind-image-once:false}") boolean failBlindOnce) {
        this.failBlindOnce = failBlindOnce;
    }

    @Override
    public byte[] generate(String imagePrompt) {
        generatedPrompts.add(imagePrompt);
        if (failBlindOnce && !blindFailed && imagePrompt.startsWith("BLIND")) {
            blindFailed = true;
            throw new IllegalStateException("blind image failed once");
        }
        return ("test-png:" + imagePrompt).getBytes(StandardCharsets.UTF_8);
    }

    public List<String> generatedPrompts() {
        return List.copyOf(generatedPrompts);
    }
}
