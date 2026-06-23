package com.looky.result.application;

import java.util.List;

public interface ResultNarrativeClient {
    ResultNarrative generate(List<ResultAnswerAdjectiveRecord> answers);
}
