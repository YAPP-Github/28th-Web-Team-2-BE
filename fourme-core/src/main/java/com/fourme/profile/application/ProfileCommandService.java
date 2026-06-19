package com.fourme.profile.application;

import com.fourme.profile.domain.OwnerTokenGenerator;
import com.fourme.profile.domain.OwnerTokenHasher;
import com.fourme.profile.domain.Profile;
import com.fourme.profile.port.ProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileCommandService {

    private final ProfileRepository profileRepository;
    private final OwnerTokenGenerator ownerTokenGenerator;
    private final OwnerTokenHasher ownerTokenHasher;
    private final Clock clock;

    @Transactional
    public CreateProfileResult createProfile(CreateProfileCommand command) {
        String ownerToken = ownerTokenGenerator.generate();
        Instant createdAt = Instant.now(clock);
        Profile profile = new Profile(
                UUID.randomUUID(),
                command.displayName(),
                ownerTokenHasher.hash(ownerToken),
                createdAt
        );

        Profile savedProfile = profileRepository.save(profile);
        return new CreateProfileResult(savedProfile.id(), ownerToken, savedProfile.createdAt());
    }
}
