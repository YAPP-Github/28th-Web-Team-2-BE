package com.looky.result.persistence;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "result_quadrants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_result_quadrants_result_type", columnNames = {"result_id", "quadrant_type"})
})
public class ResultQuadrantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private ResultJpaEntity result;

    @Enumerated(EnumType.STRING)
    @Column(name = "quadrant_type", nullable = false, length = 32)
    private ResultQuadrantType quadrantType;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    protected ResultQuadrantJpaEntity() {
    }

    public ResultQuadrantJpaEntity(ResultJpaEntity result, ResultQuadrantType quadrantType, String imageUrl) {
        this.result = result;
        this.quadrantType = quadrantType;
        this.imageUrl = imageUrl;
    }

    public ResultQuadrantType getQuadrantType() {
        return quadrantType;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
