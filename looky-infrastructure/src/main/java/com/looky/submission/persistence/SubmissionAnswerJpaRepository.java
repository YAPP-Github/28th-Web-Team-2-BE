package com.looky.submission.persistence;

import com.looky.submission.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionAnswerJpaRepository extends JpaRepository<SubmissionAnswerJpaEntity, Long> {

    @Query("""
            select answer
            from SubmissionAnswerJpaEntity answer
            join fetch answer.submissionQuestion submissionQuestion
            join fetch submissionQuestion.question question
            join fetch submissionQuestion.submission submission
            where submission.survey.id = :surveyId
              and submission.submissionStatus = :submissionStatus
            order by submission.id asc, submissionQuestion.sequence asc
            """)
    List<SubmissionAnswerJpaEntity> findCompletedAnswersBySurveyId(
            @Param("surveyId") Long surveyId,
            @Param("submissionStatus") SubmissionStatus submissionStatus
    );
}
