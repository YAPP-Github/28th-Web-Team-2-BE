package com.looky.result.client;

import com.looky.result.domain.ResultQuadrantType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiResultNarrativeClientTest {

    @Test
    void convertsStructuredModelOutputToNarrativeForEveryAnswerAndQuadrant() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(
                answer(11L, List.of("호기심 많은", "유연한")),
                answer(12L, List.of("성실한"))
        );
        output.quadrants = List.of(
                quadrant("OPEN", "서로 알고 있는 강점", "OPEN image"),
                quadrant("BLIND", "타인이 먼저 발견하는 강점", "BLIND image"),
                quadrant("HIDDEN", "나만 알고 있는 내면", "HIDDEN image"),
                quadrant("UNKNOWN", "아직 발견되지 않은 가능성", "UNKNOWN image")
        );

        var narrative = OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L, 12L));

        assertEquals(List.of("호기심 많은", "유연한"), narrative.adjectivesBySubmissionAnswerId().get(11L));
        assertEquals("타인이 먼저 발견하는 강점", narrative.quadrants().get(ResultQuadrantType.BLIND).interpretation());
        assertEquals("UNKNOWN image", narrative.quadrants().get(ResultQuadrantType.UNKNOWN).imagePrompt());
    }

    private static OpenAiResultNarrativeClient.AnswerAdjectives answer(Long submissionAnswerId, List<String> adjectives) {
        var answer = new OpenAiResultNarrativeClient.AnswerAdjectives();
        answer.submissionAnswerId = submissionAnswerId;
        answer.adjectives = adjectives;
        return answer;
    }

    private static OpenAiResultNarrativeClient.QuadrantNarrative quadrant(String quadrantType, String interpretation, String imagePrompt) {
        var quadrant = new OpenAiResultNarrativeClient.QuadrantNarrative();
        quadrant.quadrantType = quadrantType;
        quadrant.interpretation = interpretation;
        quadrant.imagePrompt = imagePrompt;
        return quadrant;
    }
}
