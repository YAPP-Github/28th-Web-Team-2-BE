package com.looky.characterpack.application;

import com.looky.result.domain.ResultQuadrantType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterPackRepository {
    Optional<CharacterPackSnapshot> findActiveSnapshot();

    Optional<CharacterPackVariantRecord> findPrimaryVariant(String packKey, String packVersion, ResultQuadrantType quadrantType);

    default List<CharacterPackVariantRecord> findReferenceVariants(String packKey, String packVersion) {
        throw new UnsupportedOperationException("Reference variant lookup is not implemented");
    }
}
