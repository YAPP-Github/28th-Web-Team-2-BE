package com.looky.result.client;

import com.looky.result.application.ResultImageClient;
import com.looky.result.application.ResultImageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"test", "local"})
public class TestResultImageClient implements ResultImageClient {

    private final boolean failBlindOnce;
    private boolean blindFailed;
    private final List<String> generatedPrompts = new ArrayList<>();

    public TestResultImageClient(@Value("${looky.test.fail-blind-image-once:false}") boolean failBlindOnce) {
        this.failBlindOnce = failBlindOnce;
    }

    @Override
    public byte[] generate(ResultImageRequest request) {
        generatedPrompts.add(request.imagePrompt());
        if (failBlindOnce && !blindFailed && request.imagePrompt().contains("BLIND image prompt")) {
            blindFailed = true;
            throw new IllegalStateException("blind image failed once");
        }
        return ("test-png:" + request.imagePrompt()).getBytes(StandardCharsets.UTF_8);
    }

    public List<String> generatedPrompts() {
        return List.copyOf(generatedPrompts);
    }

    @Override
    public String modelName() {
        return "test-image-stub";
    }
}
