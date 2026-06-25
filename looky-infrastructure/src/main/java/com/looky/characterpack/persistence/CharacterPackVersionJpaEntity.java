package com.looky.characterpack.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "character_pack_versions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_character_pack_versions_pack_version", columnNames = {"pack_id", "version"})
})
public class CharacterPackVersionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id", nullable = false)
    private CharacterPackJpaEntity pack;

    @Column(name = "version", nullable = false, length = 40)
    private String version;

    @Column(name = "base_asset_key", nullable = false, length = 255)
    private String baseAssetKey;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CharacterPackVersionJpaEntity() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public CharacterPackJpaEntity getPack() {
        return pack;
    }

    public String getVersion() {
        return version;
    }

    public String getBaseAssetKey() {
        return baseAssetKey;
    }
}
