package com.looky.result.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultQuadrantJpaRepository extends JpaRepository<ResultQuadrantJpaEntity, Long> {
    List<ResultQuadrantJpaEntity> findByResult_Id(Long resultId);
}
