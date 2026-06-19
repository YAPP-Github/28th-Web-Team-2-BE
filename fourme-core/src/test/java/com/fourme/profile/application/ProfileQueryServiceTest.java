package com.fourme.profile.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fourme.profile.domain.OwnerTokenHasher;
import com.fourme.profile.domain.Profile;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileQueryServiceTest {

    private final OwnerTokenHasher ownerTokenHasher = new OwnerTokenHasher("test-secret");
    private final CapturingProfileRepository profileRepository = new CapturingProfileRepository();
    private final ProfileQueryService queryService = new ProfileQueryService(
            profileRepository,
            ownerTokenHasher
    );

    @Test
    void verifyOwnerTokenReturnsTrueWhenHashMatches() {
        Profile profile = saveProfile("raw-owner-token");

        boolean verified = queryService.verifyOwnerToken(profile.id(), "raw-owner-token");

        assertThat(verified).isTrue();
    }

    @Test
    void verifyOwnerTokenReturnsFalseWhenTokenDoesNotMatch() {
        Profile profile = saveProfile("raw-owner-token");

        boolean verified = queryService.verifyOwnerToken(profile.id(), "other-token");

        assertThat(verified).isFalse();
    }

    @Test
    void verifyOwnerTokenReturnsFalseWhenProfileDoesNotExist() {
        boolean verified = queryService.verifyOwnerToken(UUID.randomUUID(), "raw-owner-token");

        assertThat(verified).isFalse();
    }

    private Profile saveProfile(String ownerToken) {
        Profile profile = new Profile(
                UUID.randomUUID(),
                "Connor",
                ownerTokenHasher.hash(ownerToken),
                Instant.parse("2026-06-19T12:00:00Z")
        );
        profileRepository.save(profile);
        return profile;
    }
}
