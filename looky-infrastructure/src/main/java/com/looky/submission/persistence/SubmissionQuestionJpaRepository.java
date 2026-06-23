package com.looky.submission.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SubmissionQuestionJpaRepository extends JpaRepository<SubmissionQuestionJpaEntity, Long> {
    List<SubmissionQuestionJpaEntity> findBySubmission_IdOrderBySequenceAsc(Long submissionId);

    List<SubmissionQuestionJpaEntity> findBySubmission_IdAndQuestion_IdIn(Long submissionId, Collection<Long> questionIds);
}
