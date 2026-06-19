package com.fourme.profile.application;

import com.fourme.profile.domain.OwnerTokenHasher;
import com.fourme.profile.domain.Profile;
import com.fourme.profile.port.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final OwnerTokenHasher ownerTokenHasher;

    @Transactional(readOnly = true)
    public Optional<Profile> findProfile(UUID profileId) {
        return profileRepository.findById(profileId);
    }

    @Transactional(readOnly = true)
    public boolean verifyOwnerToken(UUID profileId, String ownerToken) {
        return profileRepository.findById(profileId)
                .map(profile -> ownerTokenHasher.matches(ownerToken, profile.ownerTokenHash()))
                .orElse(false);
    }
}
