package com.fourme.profile.application;

import java.util.UUID;

public record VerifyProfileOwnerTokenQuery(
        UUID profileId,
        String ownerToken
) {
}
