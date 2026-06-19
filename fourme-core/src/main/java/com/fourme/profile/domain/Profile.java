package com.fourme.profile.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
@Setter(AccessLevel.PRIVATE)
public class Profile {

    @Id
    private UUID id;

    private String displayName;

    private String ownerTokenHash;

    private Instant createdAt;

    public Profile(UUID id, String displayName, String ownerTokenHash, Instant createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.ownerTokenHash = ownerTokenHash;
        this.createdAt = createdAt;
    }
}
