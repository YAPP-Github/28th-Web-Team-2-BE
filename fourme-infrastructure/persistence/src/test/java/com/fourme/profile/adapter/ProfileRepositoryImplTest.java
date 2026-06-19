package com.fourme.profile.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fourme.profile.domain.Profile;
import com.fourme.profile.port.ProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@Import(ProfileRepositoryImpl.class)
@ContextConfiguration(classes = ProfileRepositoryImplTest.TestConfig.class)
class ProfileRepositoryImplTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    void saveAndFindProfile() {
        Profile profile = new Profile(
                UUID.randomUUID(),
                "Connor",
                "owner-token-hash",
                Instant.parse("2026-06-19T12:00:00Z")
        );

        profileRepository.save(profile);

        Optional<Profile> foundProfile = profileRepository.findById(profile.id());
        assertThat(foundProfile).isPresent();
        assertThat(foundProfile.get().id()).isEqualTo(profile.id());
        assertThat(foundProfile.get().displayName()).isEqualTo("Connor");
        assertThat(foundProfile.get().ownerTokenHash()).isEqualTo("owner-token-hash");
        assertThat(foundProfile.get().createdAt()).isEqualTo(Instant.parse("2026-06-19T12:00:00Z"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Profile.class)
    @EnableJpaRepositories(basePackageClasses = ProfileJpaRepository.class)
    static class TestConfig {
    }
}
