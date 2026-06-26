package com.looky.characterpack.persistence;

import com.looky.characterpack.application.CharacterPackRepository;
import com.looky.characterpack.application.CharacterPackSnapshot;
import com.looky.characterpack.application.CharacterPackVariantRecord;
import com.looky.result.domain.ResultQuadrantType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class CharacterPackRepositoryImpl implements CharacterPackRepository {

    private final CharacterPackVersionJpaRepository characterPackVersionJpaRepository;
    private final CharacterPackVariantJpaRepository characterPackVariantJpaRepository;

    public CharacterPackRepositoryImpl(
            CharacterPackVersionJpaRepository characterPackVersionJpaRepository,
            CharacterPackVariantJpaRepository characterPackVariantJpaRepository
    ) {
        this.characterPackVersionJpaRepository = characterPackVersionJpaRepository;
        this.characterPackVariantJpaRepository = characterPackVariantJpaRepository;
    }

    @Override
    public Optional<CharacterPackSnapshot> findActiveSnapshot() {
        return characterPackVersionJpaRepository.findFirstByActiveTrue()
                .map(version -> new CharacterPackSnapshot(version.getPack().getPackKey(), version.getVersion()));
    }

    @Override
    public Optional<CharacterPackVariantRecord> findPrimaryVariant(String packKey, String packVersion, ResultQuadrantType quadrantType) {
        return characterPackVariantJpaRepository.findFirstByVersion_Pack_PackKeyAndVersion_VersionAndQuadrantTypeOrderBySortOrderAsc(
                        packKey,
                        packVersion,
                        quadrantType
                )
                .map(variant -> new CharacterPackVariantRecord(
                        variant.getVariantKey(),
                        variant.getQuadrantType(),
                        variant.getVersion().getBaseAssetKey(),
                        variant.getAssetKey()
                ));
    }

    @Override
    public List<CharacterPackVariantRecord> findReferenceVariants(String packKey, String packVersion) {
        return characterPackVariantJpaRepository.findByVersion_Pack_PackKeyAndVersion_VersionOrderBySortOrderAsc(packKey, packVersion)
                .stream()
                .map(variant -> new CharacterPackVariantRecord(
                        variant.getVariantKey(),
                        variant.getQuadrantType(),
                        variant.getVersion().getBaseAssetKey(),
                        variant.getAssetKey()
                ))
                .toList();
    }
}
