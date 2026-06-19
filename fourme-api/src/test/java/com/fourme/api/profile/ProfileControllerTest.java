package com.fourme.api.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fourme.profile.application.CreateProfileCommand;
import com.fourme.profile.application.CreateProfileResult;
import com.fourme.profile.application.ProfileApplicationService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileApplicationService profileApplicationService;

    @Test
    void createProfileReturnsCreatedProfile() throws Exception {
        UUID profileId = UUID.fromString("4f00f296-27b6-42e9-8d89-c47f4c4998f2");
        Instant createdAt = Instant.parse("2026-06-19T12:00:00Z");
        given(profileApplicationService.createProfile(any(CreateProfileCommand.class)))
                .willReturn(new CreateProfileResult(profileId, "owner-token", createdAt));

        mockMvc.perform(post("/api/v1/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Connor"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/profiles/" + profileId))
                .andExpect(jsonPath("$.profileId").value(profileId.toString()))
                .andExpect(jsonPath("$.ownerToken").value("owner-token"))
                .andExpect(jsonPath("$.createdAt").value("2026-06-19T12:00:00Z"));

        verify(profileApplicationService).createProfile(new CreateProfileCommand("Connor"));
    }

    @Test
    void createProfileRejectsBlankDisplayName() throws Exception {
        mockMvc.perform(post("/api/v1/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.displayName").exists());
    }
}
