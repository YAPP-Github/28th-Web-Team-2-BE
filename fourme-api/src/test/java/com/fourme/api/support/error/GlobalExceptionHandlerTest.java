package com.fourme.api.support.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestErrorController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void apiExceptionReturnsContractErrorResponse() throws Exception {
        mockMvc.perform(get("/test/errors/owner-token-required"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("OWNER_TOKEN_REQUIRED"))
                .andExpect(jsonPath("$.message").value("owner token이 필요합니다."))
                .andExpect(jsonPath("$.path").value("/test/errors/owner-token-required"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void apiExceptionCanReturnForbiddenErrorResponse() throws Exception {
        mockMvc.perform(get("/test/errors/owner-token-invalid"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("OWNER_TOKEN_INVALID"))
                .andExpect(jsonPath("$.message").value("owner token이 올바르지 않습니다."));
    }

    @RestController
    public static class TestErrorController {

        @GetMapping("/test/errors/owner-token-required")
        void ownerTokenRequired() {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.OWNER_TOKEN_REQUIRED);
        }

        @GetMapping("/test/errors/owner-token-invalid")
        void ownerTokenInvalid() {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.OWNER_TOKEN_INVALID);
        }
    }
}
