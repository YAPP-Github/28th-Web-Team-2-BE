package com.looky.api.support.spring;

import com.looky.api.LookyApiApplication;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LookyApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightRequestAllowsConfiguredFrontendOrigins() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Matchers.containsString("GET")));
    }

    @Test
    void preflightRequestAllowsProductionFrontendOriginForSurveyCreation() throws Exception {
        mockMvc.perform(options("/api/v1/surveys")
                        .header(HttpHeaders.ORIGIN, "https://looky.my")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://looky.my"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Matchers.containsString("POST")));
    }

    @Test
    void apiResponseExposesConfiguredHeadersToAllowedOrigins() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header(HttpHeaders.ORIGIN, "http://192.168.45.187:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://192.168.45.187:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, Matchers.containsString("Location")));
    }

    @Test
    void apiResponseAllowsRequestedLocalNetworkFrontendOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header(HttpHeaders.ORIGIN, "http://192.168.0.100:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://192.168.0.100:3000"));
    }
}
