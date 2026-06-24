package com.looky.api.support.spring;

import com.looky.api.LookyApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LookyApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiAndHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsCommonWrapper() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("헬스 체크에 성공했습니다."))
                .andExpect(jsonPath("$.payload.healthStatus").value("UP"));
    }

    @Test
    void swaggerUiIndexIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
    }

    @Test
    void apiDocsExposeHealthAndSurveyPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/health']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/surveys/{surveyCode}/status']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/surveys/{surveyCode}/result']").exists());
    }
}
