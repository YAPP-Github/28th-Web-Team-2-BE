package com.looky.result.client;

import com.looky.result.domain.ResultQuadrantType;
import com.looky.result.application.ResultGenerationRequest;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FakeResultGeneratorClientTest {

    private final FakeResultGeneratorClient client = new FakeResultGeneratorClient();

    @Test
    void generateReturnsFixedCdnUrlsForFourQuadrants() {
        var result = client.generate(new ResultGenerationRequest(new SurveyRecord(
                1L,
                "만두",
                "b91k2p8xq4z2",
                SurveyStatus.COLLECTING,
                ResultStatus.GENERATING,
                1,
                3,
                OffsetDateTime.parse("2026-06-23T03:00:00+09:00"),
                OffsetDateTime.parse("2026-06-22T03:00:00+09:00"),
                "pomang",
                "v1"
        ), 3));

        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/open.png", result.quadrantImageUrls().get(ResultQuadrantType.OPEN));
        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/blind.png", result.quadrantImageUrls().get(ResultQuadrantType.BLIND));
        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/hidden.png", result.quadrantImageUrls().get(ResultQuadrantType.HIDDEN));
        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/unknown.png", result.quadrantImageUrls().get(ResultQuadrantType.UNKNOWN));
    }
}
