package com.looky.api.support.spring;

import com.looky.api.LookyApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = LookyApiApplication.class, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("local")
class ResultGenerationPropertiesTest {

    @Value("${looky.result-generation.narrative-model}")
    private String narrativeModel;

    @Value("${looky.result-generation.image-model}")
    private String imageModel;

    @Value("${looky.result-generation.image-quality}")
    private String imageQuality;

    @Value("${looky.result-generation.presigned-url-ttl}")
    private Duration presignedUrlTtl;

    @Value("${looky.result-generation.s3.bucket}")
    private String bucket;

    @Value("${looky.result-generation.s3.region}")
    private String region;

    @Test
    void resultGenerationUsesConfiguredModelsQualityAndSignedUrlTtl() {
        assertEquals("gpt-5.4-mini", narrativeModel);
        assertEquals("gpt-image-1.5", imageModel);
        assertEquals("low", imageQuality);
        assertEquals(Duration.ofHours(24), presignedUrlTtl);
        assertEquals("app-contents-dev", bucket);
        assertEquals("ap-northeast-2", region);
    }
}
