package com.fourme.profile.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fourme.profile.domain.Profile;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryProfileRepositoryTest {

    @Test
    void saveAndFindProfile() {
        InMemoryProfileRepository repository = new InMemoryProfileRepository();
        Profile profile = new Profile(
                UUID.randomUUID(),
                "Connor",
                "owner-token-hash",
                Instant.parse("2026-06-19T12:00:00Z")
        );

        repository.save(profile);

        Optional<Profile> foundProfile = repository.findById(profile.id());
        assertThat(foundProfile).contains(profile);
    }
}
