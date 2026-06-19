package com.fourme.profile.port;

import com.fourme.profile.domain.Profile;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {

    Profile save(Profile profile);

    Optional<Profile> findById(UUID profileId);
}
