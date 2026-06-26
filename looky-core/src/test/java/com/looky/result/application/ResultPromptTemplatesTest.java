package com.looky.result.application;

import com.looky.question.domain.TraitCode;
import com.looky.submission.domain.SubmitterType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultPromptTemplatesTest {

    @Test
    void narrativePromptIncludesRequiredOverallAndQuadrantContracts() {
        String instructions = ResultPromptTemplates.NARRATIVE_INSTRUCTIONS;
        String input = ResultNarrativePromptComposer.compose(List.of(
                new ResultAnswerAdjectiveRecord(
                        101L,
                        11L,
                        SubmitterType.SELF,
                        "본인",
                        TraitCode.OPENNESS,
                        "질문",
                        "답변",
                        List.of()
                )
        ));

        assertTrue(instructions.contains("`analysisTitle`"));
        assertTrue(instructions.contains("`analysisBody`"));
        assertTrue(instructions.contains("`tip`"));
        assertTrue(instructions.contains("`definitionKeyword`"));
        assertTrue(instructions.contains("`adjectiveKeywords`"));
        assertTrue(instructions.contains("`OPEN`, `BLIND`, `HIDDEN`, `UNKNOWN`"));
        assertTrue(instructions.contains("절대 합치지"));
        assertTrue(instructions.contains("개수는 반드시 같아야"));
        assertTrue(instructions.contains("일단 저지르고 봐"));
        assertTrue(instructions.contains("이름, 원문 답변, 민감 정보"));
        assertTrue(input.contains("expectedSubmissionAnswerIds"));
        assertTrue(input.contains("각 입력 행은 서로 독립이다"));
        assertTrue(input.contains("같은 개수, 같은 순서로 1:1 대응"));
        assertTrue(input.contains("101"));
        assertTrue(input.contains("submissionAnswerId: 101"));
        assertTrue(input.contains("respondentLabel: 본인"));
    }
}
