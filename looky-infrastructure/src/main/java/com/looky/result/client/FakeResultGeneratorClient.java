package com.looky.result.client;

import com.looky.result.application.GeneratedResult;
import com.looky.result.application.ResultGeneratorClient;
import com.looky.result.domain.ResultQuadrantType;
import com.looky.survey.application.SurveyRecord;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class FakeResultGeneratorClient implements ResultGeneratorClient {

    private static final String CDN_BASE_URL = "https://cdn.looky.my/results";

    @Override
    public GeneratedResult generate(SurveyRecord survey) {
        Map<ResultQuadrantType, String> imageUrls = new EnumMap<>(ResultQuadrantType.class);
        imageUrls.put(ResultQuadrantType.OPEN, imageUrl(survey.surveyCode(), "open"));
        imageUrls.put(ResultQuadrantType.BLIND, imageUrl(survey.surveyCode(), "blind"));
        imageUrls.put(ResultQuadrantType.HIDDEN, imageUrl(survey.surveyCode(), "hidden"));
        imageUrls.put(ResultQuadrantType.UNKNOWN, imageUrl(survey.surveyCode(), "unknown"));
        return new GeneratedResult(imageUrls);
    }

    private String imageUrl(String surveyCode, String quadrantName) {
        return CDN_BASE_URL + "/" + surveyCode + "/" + quadrantName + ".png";
    }
}
