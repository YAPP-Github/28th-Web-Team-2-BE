package com.looky.result.client;

import com.looky.result.application.ResultAnswerAdjectiveRecord;
import com.looky.result.application.ResultNarrative;
import com.looky.result.application.ResultNarrativeClient;
import com.looky.result.domain.ResultQuadrantType;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Profile("!test & !local")
public class OpenAiResultNarrativeClient implements ResultNarrativeClient {

    private final String narrativeModel;

    public OpenAiResultNarrativeClient(@Value("${looky.result-generation.narrative-model}") String narrativeModel) {
        this.narrativeModel = narrativeModel;
    }

    @Override
    public ResultNarrative generate(List<ResultAnswerAdjectiveRecord> answers) {
        OpenAiNarrativeOutput output = OpenAIOkHttpClient.fromEnv().responses().create(
                        StructuredResponseCreateParams.<OpenAiNarrativeOutput>builder()
                                .model(narrativeModel)
                                .instructions("""
                                        You are a Korean Johari-window analyst. Extract concise Korean adjectives for every survey answer.
                                        SELF answers are the survey owner's self-perception. PEER answers are other people's perception of the owner.
                                        Compare the two groups when classifying evidence: OPEN is shared by SELF and PEER, BLIND is supported by PEER but not SELF, HIDDEN is supported by SELF but not PEER, and UNKNOWN is a plausible potential not established by either group.
                                        Return one overall Korean keyword, one overall Korean analysis, and one actionable Korean tip titled "이렇게 해보는 건 어때요?".
                                        Then classify all evidence into exactly OPEN, BLIND, HIDDEN, and UNKNOWN.
                                        For each quadrant, return one character-like Korean definition keyword, exactly two short conversational Korean adjective keywords, one Korean interpretation, and one English abstract image prompt.
                                        For every quadrant, write a concise Korean interpretation and an English image prompt for an abstract, non-identifying illustration.
                                        Do not include names, raw survey text, or sensitive personal data in interpretations or image prompts.
                                        """)
                                .input(prompt(answers))
                                .text(OpenAiNarrativeOutput.class)
                                .build()
                ).output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("OpenAI narrative response is missing structured output"));
        return toResultNarrative(output, answers.stream().map(ResultAnswerAdjectiveRecord::submissionAnswerId).toList());
    }

    static ResultNarrative toResultNarrative(OpenAiNarrativeOutput output, List<Long> expectedAnswerIds) {
        if (output == null || output.overall == null || output.answerAdjectives == null || output.quadrants == null) {
            throw new IllegalArgumentException("OpenAI narrative response is incomplete");
        }
        if (isBlank(output.overall.keyword) || isBlank(output.overall.analysis) || isBlank(output.overall.tip)) {
            throw new IllegalArgumentException("OpenAI narrative contains an invalid overview");
        }

        Map<Long, List<String>> adjectivesByAnswerId = new LinkedHashMap<>();
        for (AnswerAdjectives answer : output.answerAdjectives) {
            if (answer == null || answer.submissionAnswerId == null || answer.adjectives == null
                    || answer.adjectives.isEmpty() || answer.adjectives.stream().anyMatch(OpenAiResultNarrativeClient::isBlank)) {
                throw new IllegalArgumentException("OpenAI narrative contains an invalid adjective record");
            }
            if (adjectivesByAnswerId.put(answer.submissionAnswerId, List.copyOf(answer.adjectives)) != null) {
                throw new IllegalArgumentException("OpenAI narrative contains duplicate answer adjectives");
            }
        }
        if (!adjectivesByAnswerId.keySet().equals(Set.copyOf(expectedAnswerIds))) {
            throw new IllegalArgumentException("OpenAI narrative does not cover every answer");
        }

        Map<ResultQuadrantType, ResultNarrative.QuadrantNarrative> quadrants = new EnumMap<>(ResultQuadrantType.class);
        putQuadrant(quadrants, ResultQuadrantType.OPEN, output.quadrants.open);
        putQuadrant(quadrants, ResultQuadrantType.BLIND, output.quadrants.blind);
        putQuadrant(quadrants, ResultQuadrantType.HIDDEN, output.quadrants.hidden);
        putQuadrant(quadrants, ResultQuadrantType.UNKNOWN, output.quadrants.unknown);
        return new ResultNarrative(
                new ResultNarrative.Overview(output.overall.keyword, output.overall.analysis, output.overall.tip),
                Map.copyOf(adjectivesByAnswerId),
                Map.copyOf(quadrants)
        );
    }

    private static void putQuadrant(Map<ResultQuadrantType, ResultNarrative.QuadrantNarrative> quadrants, ResultQuadrantType type, QuadrantNarrative quadrant) {
        if (quadrant == null || isBlank(quadrant.definitionKeyword) || quadrant.adjectiveKeywords == null || quadrant.adjectiveKeywords.size() != 2
                || quadrant.adjectiveKeywords.stream().anyMatch(OpenAiResultNarrativeClient::isBlank)
                || isBlank(quadrant.interpretation) || isBlank(quadrant.imagePrompt)) {
            throw new IllegalArgumentException("OpenAI narrative contains an invalid " + type + " quadrant");
        }
        quadrants.put(type, new ResultNarrative.QuadrantNarrative(
                quadrant.definitionKeyword, List.copyOf(quadrant.adjectiveKeywords), quadrant.interpretation, quadrant.imagePrompt));
    }

    private static String prompt(List<ResultAnswerAdjectiveRecord> answers) {
        return answers.stream()
                .map(answer -> """
                        submissionAnswerId: %d
                        respondentLabel: %s
                        submitterType: %s
                        traitCode: %s
                        question: %s
                        answer: %s
                        """.formatted(
                        answer.submissionAnswerId(),
                        answer.respondentLabel(),
                        answer.submitterType(),
                        answer.traitCode(),
                        answer.questionSnapshot(),
                        answer.answerSnapshot()
                ))
                .reduce("Analyze the following completed survey answers:\n", (left, right) -> left + "\n" + right);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class OpenAiNarrativeOutput {
        public OverallNarrative overall;
        public List<AnswerAdjectives> answerAdjectives;
        public Quadrants quadrants;
    }

    public static class OverallNarrative {
        public String keyword;
        public String analysis;
        public String tip;
    }

    public static class Quadrants {
        public QuadrantNarrative open;
        public QuadrantNarrative blind;
        public QuadrantNarrative hidden;
        public QuadrantNarrative unknown;
    }

    public static class AnswerAdjectives {
        public Long submissionAnswerId;
        public List<String> adjectives;
    }

    public static class QuadrantNarrative {
        public String definitionKeyword;
        public List<String> adjectiveKeywords;
        public String interpretation;
        public String imagePrompt;
    }
}
