package com.fourme.profile.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fourme.profile.domain.OwnerTokenGenerator;
import com.fourme.profile.domain.OwnerTokenHasher;
import com.fourme.profile.domain.Profile;
import com.fourme.profile.port.ProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileCommandServiceTest {

    @Test
    void createProfileStoresOwnerTokenHashAndReturnsRawTokenOnce() {
        CapturingProfileRepository profileRepository = new CapturingProfileRepository();
        OwnerTokenHasher ownerTokenHasher = new OwnerTokenHasher("test-secret");
        ProfileCommandService commandService = new ProfileCommandService(
                profileRepository,
                new FixedOwnerTokenGenerator(),
                ownerTokenHasher,
                Clock.fixed(Instant.parse("2026-06-19T12:00:00Z"), ZoneOffset.UTC)
        );

        CreateProfileResult result = commandService.createProfile(new CreateProfileCommand("Connor"));

        assertThat(result.profileId()).isEqualTo(profileRepository.savedProfile.id());
        assertThat(result.ownerToken()).isEqualTo("raw-owner-token");
        assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-06-19T12:00:00Z"));
        assertThat(profileRepository.savedProfile.displayName()).isEqualTo("Connor");
        assertThat(profileRepository.savedProfile.ownerTokenHash()).isEqualTo(ownerTokenHasher.hash("raw-owner-token"));
        assertThat(profileRepository.savedProfile.ownerTokenHash()).isNotEqualTo("raw-owner-token");
    }
}

class FixedOwnerTokenGenerator extends OwnerTokenGenerator {

    @Override
    public String generate() {
        return "raw-owner-token";
    }
}

class CapturingProfileRepository implements ProfileRepository {

    Profile savedProfile;

    @Override
    public Profile save(Profile profile) {
        savedProfile = profile;
        return profile;
    }

    @Override
    public Optional<Profile> findById(UUID profileId) {
        return Optional.ofNullable(savedProfile)
                .filter(profile -> profile.id().equals(profileId));
    }
}
