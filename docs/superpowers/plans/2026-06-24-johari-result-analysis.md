# Johari Result Analysis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** SELF 1묶음과 익명화된 PEER 3묶음 이상을 조하리 창 분석으로 변환해 전체 요약, 사분면 카드 텍스트, 이미지, 행동 팁을 결과 API로 제공한다.

**Architecture:** 결과 분석은 OpenAI 구조화 응답 호출 1회에서 전체 요약과 네 사분면의 텍스트·이미지 프롬프트를 함께 만든다. 결과 생성 입력은 응답 출처와 피어별 가명 라벨을 보존하고, DB는 전체 결과와 사분면 메타데이터를 저장한다. 이미지 생성·S3 저장·실패한 이미지 단위 재시도는 기존 흐름을 유지한다.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JPA, Flyway, MySQL/H2, OpenAI Responses API 및 Images API, JUnit 5.

---

## File structure

- `looky-core/.../ResultAnswerAdjectiveRecord.java`: 분석 입력의 SELF/PEER 출처와 익명 응답자 라벨을 보유한다.
- `looky-core/.../ResultNarrative.java`: AI가 만든 전체 결과와 사분면 텍스트 결과를 표현한다.
- `looky-core/.../ResultOverviewRecord.java`, `ResultRecord.java`, `ResultQuadrantRecord.java`: 저장된 결과를 조회 계층에 전달한다.
- `looky-core/.../ResultQueryService.java`, `survey/application/dto/*`: 기존 결과 URL/해석 필드를 유지하면서 API에 의존하지 않는 새 결과 카드 DTO를 추가한다.
- `looky-infrastructure/.../ResultGenerationSourceReaderImpl.java`: 제출별로 `SELF`, `PEER_1`, `PEER_2` 같은 안전한 라벨을 만든다.
- `looky-infrastructure/.../OpenAiResultNarrativeClient.java`: 조하리 분석 페르소나, 구조화 응답 검증, 이미지 프롬프트 생성을 담당한다.
- `looky-infrastructure/.../ResultJpaEntity.java`, `ResultQuadrantJpaEntity.java`, `ResultAnswerAdjectiveJpaEntity.java`, `ResultRepositoryImpl.java`: 분석 결과와 원천 응답 메타데이터를 저장·복원한다.
- `looky-infrastructure/src/main/resources/db/migration/V3__*.sql`: 아직 커밋되지 않은 V3를 이 기능의 단일 스키마 마이그레이션으로 완성한다.
- `looky-api/.../SurveyResultResponse.java`, `SurveyApi.java`: `ApiResponse` 구조를 그대로 유지하며 확장된 결과 payload와 Swagger 예시를 제공한다.

### Task 1: 결과 분석 도메인 계약을 확장한다

**Files:**

- Modify: `looky-core/src/main/java/com/looky/result/application/ResultAnswerAdjectiveRecord.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultNarrative.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultOverviewRecord.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultRecord.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultQuadrantRecord.java`
- Modify: `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`

- [ ] **Step 1: SELF/PEER 및 전체 결과를 요구하는 실패 테스트를 작성한다.**

`ResultGenerationServiceTest`의 분석 fixture를 다음 계약으로 변경한다. `ResultAnswerAdjectiveRecord`에는 `SubmitterType.SELF`와 `"SELF"`를 넣고, 저장된 narrative가 전체 키워드와 단일 팁을 보유한다고 검증한다.

```java
assertEquals("마음을 잘 여는 사람", resultRepository.savedNarrative.overview().keyword());
assertEquals("낯선 사람에게 먼저 말을 걸어보세요.", resultRepository.savedNarrative.overview().tip());
assertEquals("탐험가", resultRepository.savedNarrative.quadrants().get(ResultQuadrantType.OPEN).definitionKeyword());
```

- [ ] **Step 2: 테스트가 새 accessor 또는 생성자 계약 때문에 실패하는지 확인한다.**

Run: `./gradlew :looky-core:test --tests 'com.looky.result.application.ResultGenerationServiceTest'`

Expected: `overview()`, `definitionKeyword()` 또는 새 record 생성자가 없다는 컴파일 실패.

- [ ] **Step 3: 최소 도메인 record를 구현한다.**

`ResultAnswerAdjectiveRecord`에 `SubmitterType submitterType`, `String respondentLabel`을 추가한다. `ResultNarrative`는 다음 형태로 만든다.

```java
public record ResultNarrative(
        Overview overview,
        Map<Long, List<String>> adjectivesBySubmissionAnswerId,
        Map<ResultQuadrantType, QuadrantNarrative> quadrants
) {
    public record Overview(String keyword, String analysis, String tip) { }
    public record QuadrantNarrative(
            String definitionKeyword,
            List<String> adjectiveKeywords,
            String interpretation,
            String imagePrompt
    ) { }
}
```

`ResultOverviewRecord`는 `String keyword`, `String analysis`, `List<String> tips`를 갖는다. `ResultRecord`에는 `ResultOverviewRecord overview`를, `ResultQuadrantRecord`에는 `definitionKeyword`와 `List<String> adjectiveKeywords`를 추가한다. 기존 URL 전용 생성자는 기존 호출을 깨지 않도록 유지한다.

- [ ] **Step 4: 도메인 테스트를 통과시킨다.**

Run: `./gradlew :looky-core:test --tests 'com.looky.result.application.ResultGenerationServiceTest'`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: 커밋한다.**

```bash
git add looky-core/src/main/java/com/looky/result/application \
  looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java
git commit -m "feat: 결과 분석 도메인 확장"
```

### Task 2: SELF와 피어별 익명 라벨을 분석 입력에 보존한다

**Files:**

- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultGenerationSourceReaderImpl.java`
- Modify: `looky-api/src/test/java/com/looky/api/survey/SurveyResultFlowIntegrationTest.java`

- [ ] **Step 1: 응답자 라벨 분리 통합 테스트를 작성한다.**

SELF 1건과 PEER 3건을 완료한 뒤, `readCompletedAnswers`의 32개 답변이 다음 분포를 갖는지 검증한다.

```java
assertEquals(8, answers.stream().filter(answer -> answer.respondentLabel().equals("SELF")).count());
assertEquals(8, answers.stream().filter(answer -> answer.respondentLabel().equals("PEER_1")).count());
assertEquals(8, answers.stream().filter(answer -> answer.respondentLabel().equals("PEER_2")).count());
assertEquals(8, answers.stream().filter(answer -> answer.respondentLabel().equals("PEER_3")).count());
```

- [ ] **Step 2: 테스트가 라벨 미생성으로 실패하는지 확인한다.**

Run: `./gradlew :looky-api:test --tests 'com.looky.api.survey.SurveyResultFlowIntegrationTest'`

Expected: `respondentLabel()` 부재 또는 라벨 분포 assertion 실패.

- [ ] **Step 3: 제출 ID 순서로 안정적인 익명 라벨을 생성한다.**

`ResultGenerationSourceReaderImpl`에서 완료된 답변의 `SubmissionJpaEntity` ID를 `LinkedHashMap`으로 처음 등장한 순서에 매핑한다. SELF는 항상 `SELF`, PEER는 처음 등장한 제출마다 `PEER_1`, `PEER_2` 순서로 만든다. 실제 `submitterKey`는 record·프롬프트·로그 어느 곳에도 전달하지 않는다.

```java
Map<Long, String> labelsBySubmissionId = new LinkedHashMap<>();
String label = labelsBySubmissionId.computeIfAbsent(submission.getId(), ignored ->
        submission.getSubmitterType() == SubmitterType.SELF
                ? "SELF"
                : "PEER_" + (int) (labelsBySubmissionId.values().stream()
                        .filter(value -> value.startsWith("PEER_"))
                        .count() + 1));
```

- [ ] **Step 4: 통합 테스트를 통과시킨다.**

Run: `./gradlew :looky-api:test --tests 'com.looky.api.survey.SurveyResultFlowIntegrationTest'`

Expected: `BUILD SUCCESSFUL`이며 SELF 8개와 피어 라벨별 8개가 확인된다.

- [ ] **Step 5: 커밋한다.**

```bash
git add looky-infrastructure/src/main/java/com/looky/result/persistence/ResultGenerationSourceReaderImpl.java \
  looky-api/src/test/java/com/looky/api/survey/SurveyResultFlowIntegrationTest.java
git commit -m "feat: 결과 분석 응답 출처 구분"
```

### Task 3: 조하리 창 구조화 AI 출력과 검증을 구현한다

**Files:**

- Modify: `looky-infrastructure/src/main/java/com/looky/result/client/OpenAiResultNarrativeClient.java`
- Modify: `looky-infrastructure/src/test/java/com/looky/result/client/OpenAiResultNarrativeClientTest.java`

- [ ] **Step 1: 전체 결과와 사분면 카드 파싱 실패 테스트를 작성한다.**

유효 출력 fixture에는 전체 키워드, 종합 분석, 팁 1개와 네 사분면을 넣는다. OPEN에 `"탐험가"`, 태그 `List.of("탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지")`를 넣어 parser 결과를 검증한다. 팁이 비어 있거나 태그가 1개인 fixture에는 `IllegalArgumentException`을 검증한다.

```java
assertEquals("마음을 잘 여는 사람", narrative.overview().keyword());
assertEquals("새로운 대화를 시작해보세요.", narrative.overview().tip());
assertEquals(List.of("탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지"),
        narrative.quadrants().get(ResultQuadrantType.OPEN).adjectiveKeywords());
```

- [ ] **Step 2: 새 구조화 결과가 아직 없어서 테스트가 실패하는지 확인한다.**

Run: `./gradlew :looky-infrastructure:test --tests 'com.looky.result.client.OpenAiResultNarrativeClientTest'`

Expected: overview 또는 새 사분면 필드 부재로 실패.

- [ ] **Step 3: 사분면 list 대신 고정 필드 구조를 사용한다.**

중복 타입을 구조적으로 만들 수 없도록 `OpenAiNarrativeOutput`을 다음처럼 바꾼다. parser는 각 필드가 null/blank가 아닌지, 단일 tip과 태그가 정확히 2개인지 검증한다.

```java
public static class OpenAiNarrativeOutput {
    public OverallNarrative overall;
    public List<AnswerAdjectives> answerAdjectives;
    public Quadrants quadrants;
}

public static class Quadrants {
    public QuadrantNarrative open;
    public QuadrantNarrative blind;
    public QuadrantNarrative hidden;
    public QuadrantNarrative unknown;
}
```

프롬프트에는 `respondentLabel`, `submitterType`, 질문·답변·특성 코드를 전달한다. 페르소나와 OPEN/BLIND/HIDDEN/UNKNOWN 정의, UNKNOWN의 신중한 표현, 한글 키워드·태그 스타일, 개인 식별 정보와 원문 답변을 이미지 프롬프트에 넣지 말라는 조건을 명시한다. `prompt`를 package-private static으로 두고 테스트에서 `SELF`, `PEER_1` 라벨 포함 여부를 검증한다.

- [ ] **Step 4: AI parser 테스트를 통과시킨다.**

Run: `./gradlew :looky-infrastructure:test --tests 'com.looky.result.client.OpenAiResultNarrativeClientTest'`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: 커밋한다.**

```bash
git add looky-infrastructure/src/main/java/com/looky/result/client/OpenAiResultNarrativeClient.java \
  looky-infrastructure/src/test/java/com/looky/result/client/OpenAiResultNarrativeClientTest.java
git commit -m "feat: 조하리 AI 분석 결과 확장"
```

### Task 4: 결과 메타데이터를 DB에 저장하고 기존 행을 마이그레이션한다

**Files:**

- Modify: `looky-infrastructure/src/main/resources/db/migration/V3__add_submitter_type_to_result_answer_adjectives.sql`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultQuadrantJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultAnswerAdjectiveJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultRepositoryImpl.java`
- Modify: `looky-infrastructure/src/test/java/com/looky/result/persistence/ResultAnswerAdjectiveJpaEntityTest.java`
- Modify: `looky-infrastructure/src/test/java/com/looky/result/persistence/ResultQuadrantJpaEntityTest.java`
- Modify: `looky-api/src/test/java/com/looky/api/support/spring/FlywayMigrationIntegrationTest.java`

- [ ] **Step 1: 저장 필드와 V3 컬럼을 요구하는 테스트를 작성한다.**

Flyway 통합 테스트에서 `results.overall_keyword`, `results.overall_analysis`, `results.tips_json`, `result_quadrants.definition_keyword`, `result_quadrants.adjective_keywords_json`, `result_answer_adjectives.submitter_type`, `result_answer_adjectives.respondent_label` 컬럼 존재를 `information_schema.columns`로 검증한다. entity 테스트는 태그 JSON 직렬화와 SELF/PEER 라벨 저장을 검증한다.

- [ ] **Step 2: V3이 아직 새 컬럼을 만들지 않아 실패하는지 확인한다.**

Run: `./gradlew :looky-api:test --tests 'com.looky.api.support.spring.FlywayMigrationIntegrationTest'`

Expected: 누락된 컬럼 assertion 실패.

- [ ] **Step 3: 아직 미커밋인 V3을 완성한다.**

V3에 nullable 결과 메타데이터 컬럼을 추가한다. 기존 결과에는 생성 당시 텍스트가 없으므로 전체·사분면 신규 메타데이터는 nullable로 둔다. `submitter_type`은 원본 제출 테이블을 상관 서브쿼리로 조인해 백필한 뒤 `not null`로 바꾼다. H2와 MySQL 모두에서 실행되어야 하므로 MySQL 전용 `update ... join` 대신 상관 서브쿼리를 사용한다.

```sql
alter table results add column overall_keyword varchar(120) null;
alter table results add column overall_analysis text null;
alter table results add column tips_json text null;
alter table result_quadrants add column definition_keyword varchar(120) null;
alter table result_quadrants add column adjective_keywords_json text null;
alter table result_answer_adjectives add column respondent_label varchar(32) null;
```

`ResultJpaEntity`에는 `saveOverview(ResultNarrative.Overview)`와 getter를, `ResultQuadrantJpaEntity`에는 정의 키워드·태그 JSON 필드와 getter를 둔다. `ResultRepositoryImpl.saveNarrative`는 overview와 사분면 메타데이터를 한 트랜잭션에 저장하고 `toRecord`에서 복원한다.

- [ ] **Step 4: Flyway 및 persistence 테스트를 통과시킨다.**

Run: `./gradlew :looky-infrastructure:test :looky-api:test --tests 'com.looky.api.support.spring.FlywayMigrationIntegrationTest'`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: 커밋한다.**

```bash
git add looky-infrastructure/src/main/java/com/looky/result/persistence \
  looky-infrastructure/src/main/resources/db/migration/V3__add_submitter_type_to_result_answer_adjectives.sql \
  looky-infrastructure/src/test/java/com/looky/result/persistence \
  looky-api/src/test/java/com/looky/api/support/spring/FlywayMigrationIntegrationTest.java
git commit -m "feat: 조하리 결과 메타데이터 저장"
```

### Task 5: 기존 공통 응답을 유지하며 결과 조회 payload를 확장한다

**Files:**

- Modify: `looky-core/src/main/java/com/looky/result/application/ResultQueryService.java`
- Modify: `looky-core/src/main/java/com/looky/survey/application/dto/SurveyResultResult.java`
- Create: `looky-core/src/main/java/com/looky/survey/application/dto/SurveyResultQuadrant.java`
- Modify: `looky-core/src/test/java/com/looky/result/application/ResultQueryServiceTest.java`
- Modify: `looky-api/src/main/java/com/looky/api/survey/dto/SurveyResultResponse.java`
- Create: `looky-api/src/main/java/com/looky/api/survey/dto/QuadrantResultResponse.java`
- Modify: `looky-api/src/main/java/com/looky/api/survey/SurveyApi.java`
- Modify: `looky-api/src/test/java/com/looky/api/survey/SurveyControllerTest.java`

- [ ] **Step 1: READY 결과의 새 payload 계약 테스트를 작성한다.**

기존 `quadrantImageUrls`, `quadrantInterpretations` assertion을 유지하고, 추가 필드도 검증한다.

```java
assertEquals("마음을 잘 여는 사람", result.overallKeyword());
assertEquals(3, result.actionTips().size());
assertEquals("탐험가", result.quadrants().get("OPEN").definitionKeyword());
assertEquals(2, result.quadrants().get("OPEN").adjectiveKeywords().size());
```

컨트롤러 테스트는 공통 wrapper를 유지한 채 `$.payload.overallKeyword`, `$.payload.actionTips[0]`, `$.payload.quadrants.OPEN.imageUrl`을 검증한다. WAITING/GENERATING payload의 새 필드는 `null`이어야 한다.

- [ ] **Step 2: 확장 결과 필드가 없어 테스트가 실패하는지 확인한다.**

Run: `./gradlew :looky-core:test --tests 'com.looky.result.application.ResultQueryServiceTest' && ./gradlew :looky-api:test --tests 'com.looky.api.survey.SurveyControllerTest'`

Expected: 새 result DTO accessor 또는 JSON path assertion 실패.

- [ ] **Step 3: 호환 가능한 DTO 확장을 구현한다.**

`SurveyResultResult`에는 기존 두 map을 그대로 남기고 다음 필드를 추가한다.

```java
String overallKeyword,
String overallAnalysis,
List<String> actionTips,
Map<String, SurveyResultQuadrant> quadrants
```

`SurveyResultQuadrant`와 API의 `QuadrantResultResponse`는 각각 `definitionKeyword`, `List<String> adjectiveKeywords`, `interpretation`, `imageUrl`을 갖는다. `SurveyResultResponse.from`은 core DTO를 API DTO로 변환한다. `ResultQueryService`는 사분면의 presigned URL을 새 카드에도 재사용하고, READY인 오래된 결과에 신규 텍스트가 없다면 새 필드는 `null`로 반환한다. `ApiResponse`는 수정하지 않는다.

`SurveyApi`의 Swagger example은 READY payload에 전체 키워드·종합 분석·팁·사분면 카드 구조를 추가한다. Swagger 어노테이션은 계속 `SurveyApi`에만 둔다.

- [ ] **Step 4: 조회 및 컨트롤러 테스트를 통과시킨다.**

Run: `./gradlew :looky-core:test --tests 'com.looky.result.application.ResultQueryServiceTest' && ./gradlew :looky-api:test --tests 'com.looky.api.survey.SurveyControllerTest'`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: 커밋한다.**

```bash
git add looky-core/src/main/java/com/looky/result/application/ResultQueryService.java \
  looky-core/src/main/java/com/looky/survey/application/dto \
  looky-core/src/test/java/com/looky/result/application/ResultQueryServiceTest.java \
  looky-api/src/main/java/com/looky/api/survey \
  looky-api/src/test/java/com/looky/api/survey/SurveyControllerTest.java
git commit -m "feat: 조하리 결과 조회 응답 확장"
```

### Task 6: 전체 흐름과 재시도를 검증하고 배포 산출물을 만든다

**Files:**

- Modify: `looky-api/src/test/java/com/looky/api/survey/SurveyResultFlowIntegrationTest.java`
- Modify: `looky-api/src/test/java/com/looky/api/survey/ResultImageRetryIntegrationTest.java` (필요한 fake narrative fixture만)
- Modify: `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java` (필요한 fake narrative fixture만)

- [ ] **Step 1: 전체 결과 저장 후 이미지 재시도 회귀 테스트를 작성한다.**

텍스트 분석 fixture가 전체 키워드·팁·사분면 메타데이터를 저장한 뒤 OPEN 이미지 실패 상태에서 다음 실행에 OPEN만 재시도하고, 전체 결과 텍스트는 다시 AI 호출하지 않는지 검증한다.

```java
assertEquals(1, narrativeClient.generateCallCount);
assertEquals(2, imageClient.generatedQuadrants.get(ResultQuadrantType.OPEN));
assertEquals(1, imageClient.generatedQuadrants.get(ResultQuadrantType.BLIND));
```

- [ ] **Step 2: 새 fixture가 없어 실패하는지 확인한다.**

Run: `./gradlew :looky-api:test --tests 'com.looky.api.survey.ResultImageRetryIntegrationTest'`

Expected: 전체 결과 메타데이터가 없는 fixture 또는 재시도 assertion 실패.

- [ ] **Step 3: fake narrative fixture를 새 도메인 계약으로만 갱신한다.**

`FakeResultNarrativeClient`와 테스트 fixture가 `ResultNarrative.Overview` 및 네 사분면의 정의 키워드·태그·해석·프롬프트를 반환하게 한다. production 재시도 로직은 변경하지 않는다.

- [ ] **Step 4: 기능 흐름 테스트를 통과시킨다.**

Run: `./gradlew :looky-api:test --tests 'com.looky.api.survey.SurveyResultFlowIntegrationTest' --tests 'com.looky.api.survey.ResultImageRetryIntegrationTest'`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: 전체 검증을 실행한다.**

Run: `./gradlew test :looky-api:bootJar`

Expected: 모든 테스트와 `looky-api` 실행 JAR 생성이 성공한다.

- [ ] **Step 6: 최종 커밋한다.**

```bash
git add looky-api/src/test/java/com/looky/api/survey \
  looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java
git commit -m "test: 조하리 결과 생성 흐름 검증"
```

## Final verification checklist

- [ ] `SELF`와 `PEER_1~N` 라벨만 OpenAI 분석 입력에 전달되고 원본 submitter key는 전달되지 않는다.
- [ ] OpenAI 구조화 출력은 전체 키워드·분석·팁 3개와 사분면별 키워드·태그 2개·해석·이미지 프롬프트를 모두 검증한다.
- [ ] 중복 사분면은 고정 구조 스키마로 예방하고, 누락 사분면은 저장 전에 실패한다.
- [ ] S3 키, presigned URL TTL, 저품질 이미지 설정, 실패 이미지 단위 재시도는 기존 동작을 유지한다.
- [ ] Swagger는 `SurveyApi`에만 있고 `ApiResponse` 공통 wrapper는 변경하지 않는다.
- [ ] `./gradlew test :looky-api:bootJar`가 성공한다.
