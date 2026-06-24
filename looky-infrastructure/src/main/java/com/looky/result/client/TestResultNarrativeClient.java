package com.looky.result.client;

import com.looky.result.application.ResultAnswerAdjectiveRecord;
import com.looky.result.application.ResultNarrative;
import com.looky.result.application.ResultNarrativeClient;
import com.looky.result.domain.ResultQuadrantType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Profile({"test", "local"})
public class TestResultNarrativeClient implements ResultNarrativeClient {

    @Override
    public ResultNarrative generate(List<ResultAnswerAdjectiveRecord> answers) {
        Map<Long, List<String>> adjectives = answers.stream()
                .collect(java.util.stream.Collectors.toMap(ResultAnswerAdjectiveRecord::submissionAnswerId, answer -> List.of("성찰적인")));
        Map<ResultQuadrantType, ResultNarrative.QuadrantNarrative> quadrants = new EnumMap<>(ResultQuadrantType.class);
        for (ResultQuadrantType quadrant : ResultQuadrantType.values()) {
            quadrants.put(quadrant, new ResultNarrative.QuadrantNarrative(
                    quadrant.name() + " 탐험가", List.of("호기심 많은", "새로운 거 좋아"), quadrant.name() + " 해석", quadrant.name() + " image prompt"));
        }
        return new ResultNarrative(new ResultNarrative.Overview("마음을 잘 여는 사람", "종합 분석", "새로운 대화를 시작해보세요."), adjectives, quadrants);
    }
}
