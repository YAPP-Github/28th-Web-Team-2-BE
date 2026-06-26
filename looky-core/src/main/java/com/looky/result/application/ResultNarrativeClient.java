package com.looky.result.application;

import java.util.List;

public interface ResultNarrativeClient {
    ResultNarrative generate(List<ResultAnswerAdjectiveRecord> answers);

    default String modelName() {
        return getClass().getSimpleName();
    }
}
