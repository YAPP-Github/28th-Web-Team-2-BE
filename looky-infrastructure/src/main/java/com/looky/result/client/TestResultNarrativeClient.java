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
                    quadrant.name() + " 탐험가",
                    List.of("호기심 많은", "새로운 거 좋아"),
                    quadrant.name() + " 해석",
                    quadrant.name() + " image prompt"
            ));
        }
        return new ResultNarrative(
                new ResultNarrative.Overview(
                        "마음을 잘 여는 사람",
                        "대화를 여는 다정한 기운",
                        "낯선 자리에서도 \"먼저 같이 해볼까요?\" 하고 말을 건넵니다. 분위기가 금세 부드러워지고 끝엔 따뜻한 여운이 남습니다.",
                        "앞장서다 혼자 짐을 안을 때가 있어요.\n한 번만 속도 맞춰볼까요? 하고 먼저 물어보세요.\n주변도 더 편하게 움직이고 온기가 오래 돌아올 거예요."
                ),
                adjectives,
                quadrants
        );
    }

    @Override
    public String modelName() {
        return "test-narrative-stub";
    }
}
