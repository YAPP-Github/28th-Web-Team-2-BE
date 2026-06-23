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
@Profile("!test")
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
                                        Then classify all evidence into exactly OPEN, BLIND, HIDDEN, and UNKNOWN.
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
        if (output == null || output.answerAdjectives == null || output.quadrants == null) {
            throw new IllegalArgumentException("OpenAI narrative response is incomplete");
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
        for (QuadrantNarrative quadrant : output.quadrants) {
            if (quadrant == null || isBlank(quadrant.quadrantType) || isBlank(quadrant.interpretation) || isBlank(quadrant.imagePrompt)) {
                throw new IllegalArgumentException("OpenAI narrative contains an invalid quadrant");
            }
            ResultQuadrantType type;
            try {
                type = ResultQuadrantType.valueOf(quadrant.quadrantType);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("OpenAI narrative contains an unknown quadrant", exception);
            }
            if (quadrants.put(type, new ResultNarrative.QuadrantNarrative(quadrant.interpretation, quadrant.imagePrompt)) != null) {
                throw new IllegalArgumentException("OpenAI narrative contains duplicate quadrants");
            }
        }
        if (quadrants.size() != ResultQuadrantType.values().length) {
            throw new IllegalArgumentException("OpenAI narrative does not contain every quadrant");
        }
        return new ResultNarrative(Map.copyOf(adjectivesByAnswerId), Map.copyOf(quadrants));
    }

    private static String prompt(List<ResultAnswerAdjectiveRecord> answers) {
        return answers.stream()
                .map(answer -> """
                        submissionAnswerId: %d
                        traitCode: %s
                        question: %s
                        answer: %s
                        """.formatted(
                        answer.submissionAnswerId(),
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
        public List<AnswerAdjectives> answerAdjectives;
        public List<QuadrantNarrative> quadrants;
    }

    public static class AnswerAdjectives {
        public Long submissionAnswerId;
        public List<String> adjectives;
    }

    public static class QuadrantNarrative {
        public String quadrantType;
        public String interpretation;
        public String imagePrompt;
    }
}
