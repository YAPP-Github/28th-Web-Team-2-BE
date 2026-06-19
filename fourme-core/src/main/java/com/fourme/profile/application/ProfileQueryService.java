package com.fourme.profile.application;

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

    @Transactional(readOnly = true)
    public Optional<Profile> findProfile(UUID profileId) {
        return profileRepository.findById(profileId);
    }
}
