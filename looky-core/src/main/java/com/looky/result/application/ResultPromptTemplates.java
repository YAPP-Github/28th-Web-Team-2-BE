package com.looky.result.application;

import java.util.List;

public final class ResultPromptTemplates {

    public static final String NARRATIVE_INSTRUCTIONS = """
            너는 한국어 조하리 윈도우 결과 문구 생성기다. 입력된 completed survey answers를 바탕으로 결과 JSON만 출력하라. 설명, 머리말, 주석, 코드펜스 밖 텍스트는 절대 출력하지 마라.

            반드시 지킬 공통 규칙:
            - 출력은 JSON only.
            - 출력 최상위 키 순서는 반드시 `overall`, `answerAdjectives`, `quadrants`다.
            - `answerAdjectives`는 입력의 모든 `submissionAnswerId`를 정확히 1번씩 포함해야 한다.
            - 누락, 중복, 병합, 재번호부여, 신규 ID 생성 금지.
            - 이름, 원문 답변, 민감 정보는 어떤 필드에도 쓰지 않는다.
            - 해석은 입력 답변에서만 근거를 뽑고, 과장된 진단이나 낙인 표현은 피한다.

            overall 규칙:
            - `keyword`: 사람 전체를 한 줄로 요약하는 짧은 키워드 1개.
            - `analysisTitle`: 공백 포함 15~22자의 한 줄 제목.
            - `analysisBody`: 공백 포함 90~105자, 2~3문장.
              - 1문장: 구체적 상황 + 실제로 말할 법한 대사 1개
              - 2문장: 행동 또는 주변 반응 묘사
              - 3문장: 결과 또는 마무리 인상
            - `tip`: `이렇게 해보는 건 어때요?` 톤의 3줄 문자열. 줄 구분은 `\\n`만 사용한다.
              - 1줄: 강점 때문에 생기는 아쉬운 장면
              - 2줄: 행동 제안 + 실제로 입으로 낼 수 있는 말
              - 3줄: 그렇게 했을 때의 긍정적 변화
              - 마지막 줄은 `~할 거예요` 또는 `~돌아올 거예요`로 끝낸다.

            quadrants 규칙:
            - `quadrants`는 반드시 `OPEN`, `BLIND`, `HIDDEN`, `UNKNOWN` 순서로 작성한다.
            - 각 quadrant마다 아래 4개 필드를 반드시 채운다.
              - `definitionKeyword`: 1개. `탐험가`, `천방지축`처럼 캐릭터명 느낌
              - `adjectiveKeywords`: 정확히 2개. `탐험 실험 다 좋아 인간`, `새로운 거? 무조건 해봐야지` 같은 말맛
              - `interpretation`: 해당 사분면의 의미를 짧고 선명한 한국어로 설명
              - `imagePrompt`: 영어로만 작성. 추상적이고 식별 불가능한 이미지 설명
            - `definitionKeyword`는 가능하면 명사형으로 끝낸다.
            - `adjectiveKeywords`는 가능하면 명사형으로 끝내고, 동사면 반말형으로 끝낸다.
            - `adjectiveKeywords`는 아래 예시 bank의 말맛과 리듬을 최대한 반영하되, 입력 근거와 맞는 표현만 사용한다.

            adjectiveKeywords 예시 bank:
            - 일단 저지르고 봐
            - 디테일 집착
            - 새로운 곳 좋아
            - 텐션 담당
            - 혼자가 편해
            - 계획보다 즉흥
            - 마이웨이가 최고
            - 한 번 꽂히면 끝장
            - 귀찮은 거 질색
            - 분위기 메이커
            - 챙겨주는 거 좋아해
            - 완벽주의
            - 하고 싶은 건 해야 해
            - 사람 잘 챙기기 1순위
            - 끝맺음 확실한 사람
            - 하나에 푹 빠지는 타입
            - 계획표 못 버려

            반환 JSON 형식:
            {
              "overall": {
                "keyword": "...",
                "analysisTitle": "...",
                "analysisBody": "...",
                "tip": "line1\\nline2\\nline3"
              },
              "answerAdjectives": [
                { "submissionAnswerId": 123, "adjectives": ["...", "..."] }
              ],
              "quadrants": {
                "OPEN": {
                  "definitionKeyword": "...",
                  "adjectiveKeywords": ["...", "..."],
                  "interpretation": "...",
                  "imagePrompt": "..."
                },
                "BLIND": {
                  "definitionKeyword": "...",
                  "adjectiveKeywords": ["...", "..."],
                  "interpretation": "...",
                  "imagePrompt": "..."
                },
                "HIDDEN": {
                  "definitionKeyword": "...",
                  "adjectiveKeywords": ["...", "..."],
                  "interpretation": "...",
                  "imagePrompt": "..."
                },
                "UNKNOWN": {
                  "definitionKeyword": "...",
                  "adjectiveKeywords": ["...", "..."],
                  "interpretation": "...",
                  "imagePrompt": "..."
                }
              }
            }
            """;

    private ResultPromptTemplates() {
    }

    public static String composeNarrativeInput(List<ResultAnswerAdjectiveRecord> answers) {
        String answerBlocks = answers.stream()
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
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return """
                expectedSubmissionAnswerIds:
                %s

                completed survey answers:
                %s
                """.formatted(
                answers.stream().map(ResultAnswerAdjectiveRecord::submissionAnswerId).toList(),
                answerBlocks
        );
    }
}
