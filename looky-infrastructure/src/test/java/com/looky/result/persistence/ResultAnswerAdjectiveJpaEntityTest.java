package com.looky.result.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looky.question.domain.TraitCode;
import com.looky.result.application.ResultAnswerAdjectiveRecord;
import com.looky.submission.domain.SubmitterType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultAnswerAdjectiveJpaEntityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesAdjectivesAsEscapedJsonArray() throws Exception {
        var answer = new ResultAnswerAdjectiveRecord(
                1L,
                10L,
                SubmitterType.SELF,
                "SELF",
                TraitCode.OPENNESS,
                "내가 좋아하는 표현은?",
                "따옴표와 역슬래시를 포함한 답변",
                List.of()
        );
        var entity = new ResultAnswerAdjectiveJpaEntity(
                null,
                answer,
                List.of("따\"뜻한", "꼼\\꼼한")
        );

        String[] adjectives = objectMapper.readValue(adjectivesJson(entity), String[].class);

        assertEquals(List.of("따\"뜻한", "꼼\\꼼한"), List.of(adjectives));
    }

    private static String adjectivesJson(ResultAnswerAdjectiveJpaEntity entity) throws Exception {
        Field field = ResultAnswerAdjectiveJpaEntity.class.getDeclaredField("adjectivesJson");
        field.setAccessible(true);
        return (String) field.get(entity);
    }
}
