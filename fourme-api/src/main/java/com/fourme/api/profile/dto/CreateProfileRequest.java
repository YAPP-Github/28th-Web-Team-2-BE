package com.fourme.api.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProfileRequest(
        @NotBlank
        @Size(max = 30)
        String displayName
) {
}
