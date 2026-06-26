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
        assertEquals("대화를 자연스럽게 여는 온도", narrative.overview().analysisTitle());
        assertEquals("낯선 모임에서도 \"먼저 같이 해볼게요\" 하고 자연스럽게 말을 꺼냅니다. 주변 사람들도 금세 긴장을 풀고 반응이 부드러워집니다. 끝나고 나면 편안한 인상으로 기억됩니다.", narrative.overview().analysisBody());
        assertEquals("""
                분위기를 먼저 풀다 보면 내 속도를 놓칠 때가 있어요.
                한 박자 쉬며 "제가 먼저 정리해볼게요"라고 말해보세요.
                대화의 온도는 유지되면서도 흐름이 더 안정적으로 돌아올 거예요""", narrative.overview().tip());
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

    @Test
    void throwsWithAnswerIndexAndIdWhenAdjectiveRecordContainsBlankValue() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(
                answer(11L, List.of("호기심 많은")),
                answer(12L, List.of(" "))
        );
        output.overall = overview();
        output.quadrants = quadrants();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L, 12L))
        );

        assertEquals(
                "OpenAI narrative contains an invalid adjective record: index=1, submissionAnswerId=12, reason=adjectives contains blank value",
                exception.getMessage()
        );
    }

    @Test
    void acceptsNarrativeWhenOnlyCopyStyleConstraintsDeviate() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(answer(11L, List.of("호기심 많은")));
        output.overall = overview();
        output.overall.analysisTitle = "짧은 제목";
        output.overall.analysisBody = "낯선 모임에서도 \"먼저 같이 해볼게요\" 하고 자연스럽게 말을 꺼냅니다. 주변 사람들도 금세 긴장을 풀고 반응이 부드러워집니다. 끝나고 나면 편안한 인상으로 기억됩니다. 다시 찾게 되는 인상도 남습니다.";
        output.overall.tip = """
                분위기를 먼저 풀다 보면 내 속도를 놓칠 때가 있어요.
                한 박자 쉬며 "제가 먼저 정리해볼게요"라고 말해보세요.
                흐름이 조금 더 안정적으로 이어집니다.""";
        output.quadrants = quadrants();

        var narrative = OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L));

        assertEquals("짧은 제목", narrative.overview().analysisTitle());
        assertTrue(narrative.overview().analysisBody().length() > 105);
        assertTrue(narrative.overview().tip().endsWith("이어집니다."));
    }

    @Test
    void throwsWhenTipDoesNotHaveExactlyThreeLines() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(answer(11L, List.of("호기심 많은")));
        output.overall = overview();
        output.overall.tip = """
                첫 줄입니다.
                둘째 줄입니다.""";
        output.quadrants = quadrants();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L))
        );

        assertTrue(exception.getMessage().contains("overall.tip lineCount"));
    }

    @Test
    void throwsWhenQuadrantImagePromptContainsHangul() {
        var output = new OpenAiResultNarrativeClient.OpenAiNarrativeOutput();
        output.answerAdjectives = List.of(answer(11L, List.of("호기심 많은")));
        output.overall = overview();
        output.quadrants = quadrants();
        output.quadrants.open.imagePrompt = "korean 한글 prompt";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> OpenAiResultNarrativeClient.toResultNarrative(output, List.of(11L))
        );

        assertEquals("OpenAI narrative contains a non-English OPEN.imagePrompt", exception.getMessage());
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
        overview.analysisTitle = "대화를 자연스럽게 여는 온도";
        overview.analysisBody = "낯선 모임에서도 \"먼저 같이 해볼게요\" 하고 자연스럽게 말을 꺼냅니다. 주변 사람들도 금세 긴장을 풀고 반응이 부드러워집니다. 끝나고 나면 편안한 인상으로 기억됩니다.";
        overview.tip = """
                분위기를 먼저 풀다 보면 내 속도를 놓칠 때가 있어요.
                한 박자 쉬며 "제가 먼저 정리해볼게요"라고 말해보세요.
                대화의 온도는 유지되면서도 흐름이 더 안정적으로 돌아올 거예요""";
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
