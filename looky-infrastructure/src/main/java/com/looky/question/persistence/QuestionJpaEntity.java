package com.looky.question.persistence;

import com.looky.question.domain.TraitCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "questions")
public class QuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_self", nullable = false, columnDefinition = "text")
    private String contentSelf;

    @Column(name = "content_peer", nullable = false, columnDefinition = "text")
    private String contentPeer;

    @Column(name = "content_self_template", columnDefinition = "text")
    private String contentSelfTemplate;

    @Column(name = "content_peer_template", columnDefinition = "text")
    private String contentPeerTemplate;

    @Column(name = "question_type", nullable = false, length = 40)
    private String questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trait_code", nullable = false, length = 40)
    private TraitCode traitCode;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected QuestionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getContentSelf() {
        return contentSelf;
    }

    public String getContentPeer() {
        return contentPeer;
    }

    public String getContentSelfTemplate() {
        return contentSelfTemplate;
    }

    public String getContentPeerTemplate() {
        return contentPeerTemplate;
    }

    public TraitCode getTraitCode() {
        return traitCode;
    }
}
