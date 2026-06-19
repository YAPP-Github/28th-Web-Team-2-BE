package com.fourme.profile.adapter;

import com.fourme.profile.domain.Profile;
import com.fourme.profile.port.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepository {

    private final ProfileJpaRepository profileJpaRepository;

    @Override
    public Profile save(Profile profile) {
        return profileJpaRepository.save(profile);
    }

    @Override
    public Optional<Profile> findById(UUID profileId) {
        return profileJpaRepository.findById(profileId);
    }
}
