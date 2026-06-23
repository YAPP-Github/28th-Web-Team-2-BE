package com.looky.result.application;

import java.util.List;

public interface ResultGenerationSourceReader {
    List<ResultAnswerAdjectiveRecord> readCompletedAnswers(Long surveyId);
}
