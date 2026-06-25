package com.looky.result.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.looky.result.application.ResultAnswerAdjectiveRecord;
import com.looky.result.application.ResultNarrative;
import com.looky.result.application.ResultNarrativeClient;
import com.looky.result.application.ResultPromptTemplates;
import com.looky.result.domain.ResultQuadrantType;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
                .instructions(ResultPromptTemplates.NARRATIVE_INSTRUCTIONS)
                .input(ResultPromptTemplates.composeNarrativeInput(answers))
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

    @Override
    public String modelName() {
        return narrativeModel;
    }

    static ResultNarrative toResultNarrative(OpenAiNarrativeOutput output, List<Long> expectedAnswerIds) {
        if (output == null || output.overall == null || output.answerAdjectives == null || output.quadrants == null) {
            throw new IllegalArgumentException("OpenAI narrative response is incomplete");
        }
        if (isBlank(output.overall.keyword)
                || isBlank(output.overall.analysisTitle)
                || isBlank(output.overall.analysisBody)
                || isBlank(output.overall.tip)) {
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
        validateAnswerCoverage(adjectivesByAnswerId, expectedAnswerIds);

        Map<ResultQuadrantType, ResultNarrative.QuadrantNarrative> quadrants = new EnumMap<>(ResultQuadrantType.class);
        putQuadrant(quadrants, ResultQuadrantType.OPEN, output.quadrants.open);
        putQuadrant(quadrants, ResultQuadrantType.BLIND, output.quadrants.blind);
        putQuadrant(quadrants, ResultQuadrantType.HIDDEN, output.quadrants.hidden);
        putQuadrant(quadrants, ResultQuadrantType.UNKNOWN, output.quadrants.unknown);
        return new ResultNarrative(
                new ResultNarrative.Overview(
                        output.overall.keyword,
                        output.overall.analysisTitle,
                        output.overall.analysisBody,
                        output.overall.tip
                ),
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

    private static void validateAnswerCoverage(Map<Long, List<String>> adjectivesByAnswerId, List<Long> expectedAnswerIds) {
        List<Long> expectedIds = List.copyOf(new LinkedHashSet<>(expectedAnswerIds));
        List<Long> actualIds = List.copyOf(adjectivesByAnswerId.keySet());
        Set<Long> expectedIdSet = Set.copyOf(expectedIds);
        Set<Long> actualIdSet = Set.copyOf(actualIds);
        List<Long> missingIds = expectedIds.stream()
                .filter(expectedId -> !actualIdSet.contains(expectedId))
                .toList();
        List<Long> unexpectedIds = actualIds.stream()
                .filter(actualId -> !expectedIdSet.contains(actualId))
                .toList();
        if (!missingIds.isEmpty() || !unexpectedIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "OpenAI narrative does not cover every answer: expectedAnswerIds=%s, actualAnswerIds=%s, missingAnswerIds=%s, unexpectedAnswerIds=%s"
                            .formatted(expectedIds, actualIds, missingIds, unexpectedIds)
            );
        }
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
        public String analysisTitle;
        public String analysisBody;
        public String tip;
    }

    @JsonPropertyOrder({"OPEN", "BLIND", "HIDDEN", "UNKNOWN"})
    public static class Quadrants {
        @JsonProperty("OPEN")
        public QuadrantNarrative open;
        @JsonProperty("BLIND")
        public QuadrantNarrative blind;
        @JsonProperty("HIDDEN")
        public QuadrantNarrative hidden;
        @JsonProperty("UNKNOWN")
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
