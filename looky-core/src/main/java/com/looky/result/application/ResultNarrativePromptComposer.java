package com.looky.result.application;

import java.util.List;

public final class ResultNarrativePromptComposer {

    private ResultNarrativePromptComposer() {
    }

    public static String compose(List<ResultAnswerAdjectiveRecord> answers) {
        return ResultPromptTemplates.composeNarrativeInput(answers);
    }
}
