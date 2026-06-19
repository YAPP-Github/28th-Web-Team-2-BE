package com.fourme.profile.adapter;

import com.fourme.profile.domain.Profile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProfileJpaRepository extends JpaRepository<Profile, UUID> {
}
