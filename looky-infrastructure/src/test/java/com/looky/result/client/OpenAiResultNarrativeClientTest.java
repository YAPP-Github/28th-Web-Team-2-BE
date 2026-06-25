package com.looky.result.client;

import com.looky.result.domain.ResultQuadrantType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiResultNarrativeClientTest {

    @Test
    void convertsStructuredModelOutputToNarrativeForEveryAnswerAndQuadrant() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(
                answer(11L, List.of("호기심 많은", "유연한")),
                answer(12L, List.of("성실한"))
        );
        output.overall = overview();
        output.quadrants = quadrants();

        var narrative = OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L, 12L));

        assertEquals(List.of("호기심 많은", "유연한"), narrative.adjectivesBySubmissionAnswerId().get(11L));
        assertEquals("타인이 먼저 발견하는 강점", narrative.quadrants().get(ResultQuadrantType.BLIND).interpretation());
        assertEquals("UNKNOWN image", narrative.quadrants().get(ResultQuadrantType.UNKNOWN).imagePrompt());
        assertEquals("마음을 잘 여는 사람", narrative.overview().keyword());
        assertEquals("대화를 여는 다정한 기운", narrative.overview().analysisTitle());
        assertEquals("낯선 자리에서도 \"먼저 같이 해볼까요?\" 하고 말을 건넵니다. 주변도 금세 편하게 반응하고 흐름이 부드러워집니다. 끝나고 나면 따뜻한 여운이 오래 남습니다.", narrative.overview().analysisBody());
        assertEquals("새로운 대화를 시작해보세요.", narrative.overview().tip());
        assertEquals(List.of("탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지"), narrative.quadrants().get(ResultQuadrantType.OPEN).adjectiveKeywords());
    }

    @Test
    void keepsOnlyTheFirstNarrativeWhenModelReturnsTheSameQuadrantMoreThanOnce() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(answer(11L, List.of("호기심 많은")));
        output.overall = overview();
        output.quadrants = quadrants();
        output.quadrants.open.adjectiveKeywords = List.of("태그 하나");

        assertThrows(IllegalArgumentException.class, () -> OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L)));
    }

    @Test
    void throwsWhenAnswerCoverageMissesSubmissionAnswerId() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(answer(11L, List.of("호기심 많은")));
        output.overall = overview();
        output.quadrants = quadrants();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L, 12L))
        );

        assertTrue(exception.getMessage().contains("missingAnswerIds=[12]"));
        assertTrue(exception.getMessage().contains("unexpectedAnswerIds=[]"));
    }

    @Test
    void throwsWhenAnswerCoverageContainsUnexpectedSubmissionAnswerId() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(
                answer(11L, List.of("호기심 많은")),
                answer(99L, List.of("유연한"))
        );
        output.overall = overview();
        output.quadrants = quadrants();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L, 12L))
        );

        assertTrue(exception.getMessage().contains("missingAnswerIds=[12]"));
        assertTrue(exception.getMessage().contains("unexpectedAnswerIds=[99]"));
    }

    @Test
    void throwsWhenSubmissionAnswerIdAppearsMoreThanOnce() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(
                answer(11L, List.of("호기심 많은")),
                answer(11L, List.of("유연한"))
        );
        output.overall = overview();
        output.quadrants = quadrants();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L))
        );

        assertEquals("OpenAI narrative contains duplicate answer adjectives", exception.getMessage());
    }

    private static OpenAiResultNarrativeClient.AnswerAdjectives answer(Long submissionAnswerId, List<String> adjectives) {
        var answer = new OpenAiResultNarrativeClient.AnswerAdjectives();
        answer.submissionAnswerId = submissionAnswerId;
        answer.adjectives = adjectives;
        return answer;
    }

    private static OpenAiResultNarrativeClient.OverallNarrative overview() {
        var overview = new OpenAiResultNarrativeClient.OverallNarrative();
        overview.keyword = "마음을 잘 여는 사람";
        overview.analysisTitle = "대화를 여는 다정한 기운";
        overview.analysisBody = "낯선 자리에서도 \"먼저 같이 해볼까요?\" 하고 말을 건넵니다. 주변도 금세 편하게 반응하고 흐름이 부드러워집니다. 끝나고 나면 따뜻한 여운이 오래 남습니다.";
        overview.tip = "새로운 대화를 시작해보세요.";
        return overview;
    }

    private static OpenAiResultNarrativeClient.Quadrants quadrants() {
        var quadrants = new OpenAiResultNarrativeClient.Quadrants();
        quadrants.open = quadrant("탐험가", "서로 알고 있는 강점", "OPEN image");
        quadrants.blind = quadrant("관찰자", "타인이 먼저 발견하는 강점", "BLIND image");
        quadrants.hidden = quadrant("사색가", "나만 알고 있는 내면", "HIDDEN image");
        quadrants.unknown = quadrant("개척자", "아직 발견되지 않은 가능성", "UNKNOWN image");
        return quadrants;
    }

    private static OpenAiResultNarrativeClient.QuadrantNarrative quadrant(String definitionKeyword, String interpretation, String imagePrompt) {
        var quadrant = new OpenAiResultNarrativeClient.QuadrantNarrative();
        quadrant.definitionKeyword = definitionKeyword;
        quadrant.adjectiveKeywords = List.of("탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지");
        quadrant.interpretation = interpretation;
        quadrant.imagePrompt = imagePrompt;
        return quadrant;
    }
}
