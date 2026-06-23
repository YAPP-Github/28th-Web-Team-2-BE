package com.looky.question.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "answer_options", uniqueConstraints = {
        @UniqueConstraint(name = "uk_answer_options_question_sequence", columnNames = {"question_id", "sequence"})
})
public class AnswerOptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionJpaEntity question;

    @Column(name = "content_self", nullable = false, columnDefinition = "text")
    private String contentSelf;

    @Column(name = "content_peer", nullable = false, columnDefinition = "text")
    private String contentPeer;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AnswerOptionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return question.getId();
    }

    public String getContentSelf() {
        return contentSelf;
    }

    public String getContentPeer() {
        return contentPeer;
    }

    public int getSequence() {
        return sequence;
    }
}
