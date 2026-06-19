package com.fourme.profile.adapter;

import com.fourme.profile.domain.Profile;
import com.fourme.profile.port.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProfileRepository implements ProfileRepository {

    private final ConcurrentMap<UUID, Profile> profiles = new ConcurrentHashMap<>();

    @Override
    public Profile save(Profile profile) {
        profiles.put(profile.id(), profile);
        return profile;
    }

    @Override
    public Optional<Profile> findById(UUID profileId) {
        return Optional.ofNullable(profiles.get(profileId));
    }
}
