# Character Pack Result Image Handoff

## 문서 목적

이 문서는 `looky-server`에서 진행 중인 `캐릭터팩 기반 결과 이미지 생성` 작업을 다른 개발자가 바로 이어받을 수 있게 정리한 인수인계 문서다.

기준 시점은 `origin/main`의 `f4edc3f feat: shorten survey share codes and personalize questions`이다.

## 한 줄 요약

현재 운영 배포본에는 `6자리 surveyCode`, `https://looky.my/{surveyCode}` 공유 링크, `"나는?" -> "{닉네임}은/는?"` 개인화는 반영되어 있다.

하지만 `캐릭터팩 스냅샷 저장`, `quadrant별 reference asset 선택`, `OpenAI image edit 호출`, `selected_variant_key 추적 저장`은 아직 구현되지 않았다.

즉, 결과 이미지는 아직 `prompt-only` 경로다.

## 현재 확인된 상태

### 이미 main에 들어간 것

- 설문 코드 6자리
- 공유 링크 `https://looky.my/{surveyCode}`
- 질문 문구 개인화
- 결과 공개 시간 설정화 및 현재 기본값 `0`

### 아직 안 들어간 것

- 설문 생성 시 `characterPackKey`, `characterPackVersion` 스냅샷 저장
- 캐릭터팩 메타데이터 테이블
- 결과 생성 시 quadrant별 variant 선택
- OpenAI `images.generate(...)` -> `images.edit(...)` 전환
- S3 reference asset fetch
- `result_quadrants.selected_variant_key` 저장

## 미구현 근거

### survey snapshot 부재

`looky-core/src/main/java/com/looky/survey/application/SurveyRecord.java`

- 현재 `SurveyRecord`에는 `characterPackKey`, `characterPackVersion` 필드가 없다.

`looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaEntity.java`

- `surveys` 엔티티에도 대응 컬럼이 없다.

### result image request 경계 미구현

`looky-core/src/main/java/com/looky/result/application/ResultImageClient.java`

- 현재 계약은 `byte[] generate(String imagePrompt)` 한 줄이다.
- reference asset key 목록을 넘길 수 없다.

### OpenAI edit 미사용

`looky-infrastructure/src/main/java/com/looky/result/client/OpenAiResultImageClient.java`

- 현재 `OpenAIOkHttpClient.fromEnv().images().generate(...)`만 사용한다.
- `ImageEditParams`와 `images().edit(...)` 경로가 없다.

### character pack 저장소 미구현

아래가 현재 소스 트리에 없다.

- `looky-core/.../characterpack/...`
- `looky-infrastructure/.../characterpack/...`
- `V4__...character_pack...sql`
- `selected_variant_key` 관련 코드

## 참고 문서

이미 저장소에 있는 설계 문서:

- `docs/superpowers/specs/2026-06-25-character-pack-result-image-design.md`

로컬에서 작성만 되었고 아직 main에는 없는 플랜 문서가 있었다:

- `docs/superpowers/plans/2026-06-25-character-pack-result-image.md`

이번 handoff 브랜치에는 위 플랜 문서를 별도로 싣지 않고, 필요한 핵심만 이 문서에 요약했다.

## 출시 우선순위 기준 최소 범위

토요일 출시 기준으로는 `운영자용 CRUD 전체`보다 아래 1차 구현이 우선이다.

1. `character_packs`, `character_pack_versions`, `character_pack_variants` 테이블 추가
2. `surveys.character_pack_key`, `surveys.character_pack_version` 추가
3. `result_quadrants.selected_variant_key` 추가
4. 현재 사용할 캐릭터팩 1개와 quadrant용 variant 4개 seed
5. 설문 생성 시 활성 pack/version 스냅샷 저장
6. 결과 생성 시 설문 스냅샷 기준 variant 선택
7. OpenAI image edit 호출 시 S3 reference asset 바이트 주입
8. 생성 성공 시 `selected_variant_key`와 결과 이미지 key 저장

내부 운영 API는 있으면 좋지만, 출시 직전 최소 범위에서는 뒤로 미룰 수 있다.

## 추천 구현 순서

### 1. DB/Flyway

새 migration 하나로 아래를 처리한다.

- `surveys.character_pack_key varchar(...)`
- `surveys.character_pack_version varchar(...)`
- `result_quadrants.selected_variant_key varchar(...)`
- `character_packs`
- `character_pack_versions`
- `character_pack_variants`
- 현재 pack/version/variant seed insert

권장 규칙:

- 활성 version은 `character_pack_versions.active=true` 한 건만 허용
- 같은 quadrant에 여러 variant가 있으면 `sort_order` 가장 낮은 것 우선

### 2. core 계약

추가 또는 수정 대상:

- `looky-core/src/main/java/com/looky/characterpack/application/...`
- `looky-core/src/main/java/com/looky/result/application/ResultImageRequest.java`
- `looky-core/src/main/java/com/looky/survey/application/SurveyRecord.java`
- `looky-core/src/main/java/com/looky/survey/application/SurveyRepository.java`
- `looky-core/src/main/java/com/looky/survey/application/SurveyCommandService.java`
- `looky-core/src/main/java/com/looky/result/application/ResultImageClient.java`
- `looky-core/src/main/java/com/looky/result/application/ResultRepository.java`
- `looky-core/src/main/java/com/looky/result/application/ResultQuadrantRecord.java`
- `looky-core/src/main/java/com/looky/result/application/ResultGenerationService.java`

핵심 방향:

- 설문 생성 시 활성 pack/version 조회
- `saveNewSurvey(...)`에 snapshot 전달
- 결과 생성 시 `survey.characterPackKey`, `survey.characterPackVersion` 기준으로 variant 선택
- `ResultImageClient`는 문자열 대신 요청 객체를 받게 변경

### 3. infrastructure

추가 또는 수정 대상:

- `looky-infrastructure/src/main/java/com/looky/characterpack/persistence/...`
- `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaEntity.java`
- `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyRepositoryImpl.java`
- `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultQuadrantJpaEntity.java`
- `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultRepositoryImpl.java`
- `looky-infrastructure/src/main/java/com/looky/result/client/OpenAiResultImageClient.java`
- `looky-infrastructure/src/main/java/com/looky/result/client/TestResultImageClient.java`

핵심 방향:

- reference asset은 S3 object key만 저장
- OpenAI 호출 직전에 `S3Client.getObject(...)`로 asset bytes 로드
- `ImageEditParams.builder()`
  - `.model(...)`
  - `.quality(...)`
  - `.outputFormat(...)`
  - `.prompt(...)`
  - `.imageOfInputStreams(...)`
- 결과 생성 성공 시 `selected_variant_key` 저장

## 캐릭터 asset 준비물

운영 반영 전 별도 준비가 필요한 것:

1. 실제 PNG 파일을 S3에 업로드
2. 업로드 object key를 seed 데이터와 맞춤
3. 배경 투명 PNG 유지

권장 prefix:

- `character-packs/pomang/v1/base/base.png`
- `character-packs/pomang/v1/variants/open-cheer.png`
- `character-packs/pomang/v1/variants/blind-magnifier.png`
- `character-packs/pomang/v1/variants/hidden-letter.png`
- `character-packs/pomang/v1/variants/unknown-teary.png`

대화 기준 제안 매핑:

- `BLIND` -> 돋보기 든 캐릭터
- `OPEN` -> 양손 들고 별 있는 캐릭터
- `HIDDEN` -> 편지 든 캐릭터
- `UNKNOWN` -> 우는 캐릭터

`시계 24` 캐릭터는 quadrant 결과 이미지보다 대기/안내 자산에 더 가깝다. 이번 1차 범위에서는 빼도 된다.

## 테스트 포인트

### core

- 설문 생성 시 활성 pack/version 스냅샷 저장
- quadrant 타입별 variant 선택
- 동일 quadrant 다건일 때 `sort_order` 우선
- 실패한 quadrant만 재시도하는 기존 동작 유지

### integration

- 새 설문 생성
- SELF 1건 + PEER 3건 제출
- `generateReadyResults()`
- 각 quadrant 결과에 `selected_variant_key`가 저장되는지
- 결과 조회 시 서명 URL이 정상인지

## 우선 실행할 검증 명령

```bash
./gradlew :looky-core:test --tests "com.looky.survey.application.SurveyCommandServiceTest"
./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"
./gradlew :looky-api:test --tests "com.looky.api.survey.SurveyResultFlowIntegrationTest"
./gradlew :looky-api:test --tests "com.looky.api.survey.ResultImageRetryIntegrationTest"
./gradlew :looky-api:bootJar
```

마지막 전체 확인:

```bash
./gradlew test
```

## 리스크

### 1. S3 object key와 실제 업로드 파일 불일치

코드는 맞아도 운영에서 asset fetch가 실패하면 quadrant가 계속 실패한다.

### 2. OpenAI model 제약

현재 설정은 `gpt-image-2`다.

`images.edit(...)`에서 multi-image reference가 허용되더라도, 실제 품질은 `base + variant` 조합과 `variant only` 조합을 둘 다 짧게 확인하는 것이 좋다.

### 3. 테스트/로컬 프로필 seed 누락

`test` 프로필은 Flyway 대신 `ddl-auto=create-drop` + `db/data.sql`을 쓴다.

즉, entity만 바꾸고 test seed를 안 넣으면 통합 테스트가 깨질 수 있다.

## 다음 담당자 첫 액션

1. `V4` migration 초안 작성
2. `SurveyRecord`와 `SurveyJpaEntity` snapshot 필드 추가
3. `ResultImageClient`를 요청 객체 기반으로 변경
4. `TestResultImageClient`부터 먼저 맞춰서 테스트 통과
5. 마지막에 `OpenAiResultImageClient` edit 경로 연결
