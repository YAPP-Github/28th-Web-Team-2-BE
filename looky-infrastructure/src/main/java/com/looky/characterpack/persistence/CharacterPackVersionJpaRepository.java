package com.looky.characterpack.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacterPackVersionJpaRepository extends JpaRepository<CharacterPackVersionJpaEntity, Long> {
    Optional<CharacterPackVersionJpaEntity> findFirstByActiveTrue();
}
