package com.looky.characterpack.persistence;

import com.looky.result.domain.ResultQuadrantType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CharacterPackVariantJpaRepository extends JpaRepository<CharacterPackVariantJpaEntity, Long> {
    Optional<CharacterPackVariantJpaEntity> findFirstByVersion_Pack_PackKeyAndVersion_VersionAndQuadrantTypeOrderBySortOrderAsc(
            String packKey,
            String version,
            ResultQuadrantType quadrantType
    );

    List<CharacterPackVariantJpaEntity> findByVersion_Pack_PackKeyAndVersion_VersionOrderBySortOrderAsc(
            String packKey,
            String version
    );
}
