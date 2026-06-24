package com.looky.result.storage;

import com.looky.result.application.ResultImageStorage;
import com.looky.result.domain.ResultQuadrantType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local"})
public class TestResultImageStorage implements ResultImageStorage {
    @Override
    public String upload(String surveyCode, ResultQuadrantType quadrantType, byte[] imageBytes) {
        return "surveys/" + surveyCode + "/results/" + quadrantType.name() + ".png";
    }
}
