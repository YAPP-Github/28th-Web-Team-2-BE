package com.looky.question.application;

import com.looky.submission.domain.SubmitterType;

import java.util.List;

public interface QuestionRepository {
    List<QuestionRecord> findRandomActiveQuestions(int count, SubmitterType submitterType);
}
