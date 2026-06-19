package com.fourme.api.profile.dto;

import java.time.Instant;

public record CreateProfileResponse(
        String profileId,
        String ownerToken,
        Instant createdAt
) {
}
