package com.looky.question.persistence;

import com.looky.question.domain.TraitCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, Long> {
    List<QuestionJpaEntity> findByActiveTrueAndTraitCode(TraitCode traitCode);
}
