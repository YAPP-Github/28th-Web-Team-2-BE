package com.looky.characterpack.persistence;

import com.looky.result.domain.ResultQuadrantType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "character_pack_variants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_character_pack_variants_version_key", columnNames = {"version_id", "variant_key"}),
        @UniqueConstraint(name = "uk_character_pack_variants_quadrant_sort", columnNames = {"version_id", "quadrant_type", "sort_order"})
})
public class CharacterPackVariantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private CharacterPackVersionJpaEntity version;

    @Column(name = "variant_key", nullable = false, length = 120)
    private String variantKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "quadrant_type", nullable = false, length = 32)
    private ResultQuadrantType quadrantType;

    @Column(name = "asset_key", nullable = false, length = 255)
    private String assetKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CharacterPackVariantJpaEntity() {
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

    public CharacterPackVersionJpaEntity getVersion() {
        return version;
    }

    public String getVariantKey() {
        return variantKey;
    }

    public ResultQuadrantType getQuadrantType() {
        return quadrantType;
    }

    public String getAssetKey() {
        return assetKey;
    }
}
