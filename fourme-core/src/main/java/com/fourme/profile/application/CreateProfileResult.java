package com.fourme.profile.application;

import java.time.Instant;
import java.util.UUID;

public record CreateProfileResult(
        UUID profileId,
        String ownerToken,
        Instant createdAt
) {
}
