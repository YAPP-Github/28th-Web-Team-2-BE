# Survey Backend Handoff

## 문서 목적

이 문서는 `looky-server` 백엔드에서 지금까지 진행한 설문/결과 기능 작업을 다른 개발자가 바로 이어받을 수 있게 정리한 인수인계 문서다.

대상 독자는 다음 정보를 한 번에 파악할 수 있어야 한다.

- 현재 `main`에 무엇이 반영되어 있는지
- 왜 이런 구조와 계약으로 구현했는지
- 다음 작업을 어디서부터 시작하면 되는지
- 어떤 파일과 어떤 테스트를 먼저 보면 되는지

기준 시점은 `main`의 `a7841b7 feat: 결과 조회 상태형 계약 반영`까지다.

## 빠른 시작

1. `main` 최신 커밋을 pull 한다.
2. JDK 25로 Gradle 프로젝트를 import 한다.
3. 아래 검증 명령을 먼저 한 번 실행한다.

```bash
./gradlew test
./gradlew :looky-api:bootJar
```

4. 이 문서와 아래 두 문서를 읽는다.
   - `docs/superpowers/specs/2026-06-23-result-generation-scheduler-design.md`
   - `docs/superpowers/plans/2026-06-23-result-generation-scheduler.md`
5. 결과 조회 API를 만질 예정이면 `looky-api/src/main/java/com/looky/api/survey/SurveyApi.java`부터 본다.

## 현재 반영 상태

### 1차

설문 API 기본 구조가 들어가 있다.

- 설문 생성
- 응답 시작
- 응답 제출
- 설문 상태 조회

주요 커밋:

- `f05eef6 feat: 설문 1차 API 구조 추가`

### 2차

결과 조회용 read model이 들어가 있다.

- `results`
- `result_quadrants`
- 결과 조회 서비스

주요 커밋:

- `b2ba4db feat: 결과 조회 read model 추가`

### 3차

서버가 조건을 만족한 설문에 대해 fake 결과를 자동 생성하는 스케줄러가 들어가 있다.

- `1분 주기` 스케줄러
- `GENERATING -> READY/FAILED` 상태 전이
- fake generator
- 실패 시 상태 전이와 로깅

주요 커밋:

- `d2374fa feat: 결과 생성 유스케이스 추가`
- `4f785f4 fix: 결과 READY 전환 실패 처리`
- `306d12f feat: 결과 생성 스케줄러 추가`
- `40efc2e fix: 결과 생성 실패 기록 로그 추가`

### 3.5차

결과 오픈 지연을 설정값으로 빼고, 생성-조회 흐름 통합 검증을 넣었다.

- `looky.survey.result-open-delay-hours`
- `SurveyPolicy`
- E2E 통합 테스트

주요 커밋:

- `7c2e069 feat: 결과 오픈 지연 설정화`

### 4차 성격의 계약 정리

결과 조회 API를 프론트 친화적인 상태형 계약으로 정리했다.

- `GET /surveys/{surveyCode}/result`
- 유효한 `surveyCode`면 `200`
- `payload.resultStatus`로 상태 판단
- `quadrantImageUrls`는 `READY`에서만 존재

주요 커밋:

- `a7841b7 feat: 결과 조회 상태형 계약 반영`

## 현재 API 계약에서 중요한 결정

### 공통 응답 구조

모든 API는 아래 공통 래퍼를 사용한다.

```json
{
  "status": "success",
  "message": "설문 결과를 조회했습니다.",
  "payload": {}
}
```

구현 위치:

- `looky-api/src/main/java/com/looky/api/support/response/ApiResponse.java`

### Swagger 위치

Swagger 어노테이션은 컨트롤러가 아니라 `*Api` 인터페이스에 둔다.

- `SurveyController implements SurveyApi`

이 패턴은 계속 유지하는 것이 좋다.

### 결과 조회 API 계약

현재 `GET /api/v1/surveys/{surveyCode}/result`의 의미는 아래와 같다.

- `404`: 잘못된 `surveyCode`
- `200`: 유효한 `surveyCode`
  - `resultStatus=WAITING_SELF_RESPONSE`
  - `resultStatus=COLLECTING_PEER_RESPONSES`
  - `resultStatus=WAITING_RESULT_OPEN_TIME`
  - `resultStatus=GENERATING`
  - `resultStatus=READY`
  - `resultStatus=FAILED`
- `500`: `READY`인데 결과 row가 없거나 사분면 데이터가 깨진 서버 내부 불일치

즉, 결과 미준비/생성실패를 `409` 예외로 던지지 않는다. 프론트는 `200` 응답의 `payload.resultStatus`를 보고 화면을 분기해야 한다.

### 설문 상태와 결과 상태

현재 결과 관련 상태는 아래 enum으로 관리한다.

- `WAITING_SELF_RESPONSE`
- `COLLECTING_PEER_RESPONSES`
- `WAITING_RESULT_OPEN_TIME`
- `GENERATING`
- `READY`
- `FAILED`
- `EXPIRED`

구현 위치:

- `looky-core/src/main/java/com/looky/survey/domain/ResultStatus.java`

### 결과 오픈 시간

결과 오픈 시간은 더 이상 하드코딩 24시간이 아니다.

- 설정 키: `looky.survey.result-open-delay-hours`
- 기본값: `24`

구현 위치:

- `looky-api/src/main/resources/application.yaml`
- `looky-api/src/main/java/com/looky/api/support/spring/SurveyPolicyConfig.java`
- `looky-core/src/main/java/com/looky/survey/application/SurveyPolicy.java`

### 스케줄러 동작 기준

결과 생성 스케줄러는 `looky.result-generation.fixed-delay`를 따른다.

- 기본값: `60000` ms
- 현재 fake generator 기준
- 단일 인스턴스 가정

구현 위치:

- `looky-api/src/main/java/com/looky/api/LookyApiApplication.java`
- `looky-infrastructure/src/main/java/com/looky/result/scheduler/ResultGenerationScheduler.java`

## 아키텍처와 구현 규칙

### 모듈 구조

- `looky-common`: 공통 예외, 에러 코드
- `looky-core`: 유스케이스, 도메인 상태, repository/client 추상화
- `looky-infrastructure`: JPA, scheduler, fake client, persistence 구현
- `looky-api`: controller, request/response DTO, Swagger, Spring Boot 진입점

실제 의존은 아래 방향이다.

- `looky-api -> looky-core`
- `looky-api -> looky-common`
- `looky-api -(runtimeOnly)-> looky-infrastructure`
- `looky-infrastructure -> looky-core`
- `looky-infrastructure -> looky-common`

### 서비스 레이어 원칙

합의된 원칙은 아래와 같다.

- service 레이어에서 `S3Client`, `BCryptPasswordEncoder` 같은 외부 기술을 직접 쓰지 않는다.
- 외부 연동은 core 인터페이스와 infrastructure 구현으로 분리한다.
- 기능 단위 서비스 분리를 선호한다.
- Swagger는 `Controller`가 아니라 `*Api` 인터페이스에 둔다.

현재 코드에서는 이 원칙으로 가는 중이고, 신규 작업도 이 방향을 유지하는 것이 좋다.

### 패키지 깊이

`com.looky.core.*`처럼 모듈명을 패키지에 중복하지 않는다. 현재는 `com.looky.survey`, `com.looky.result`, `com.looky.api` 방식으로 정리되어 있다.

## 지금 중요한 파일들

### API 계약

- `looky-api/src/main/java/com/looky/api/survey/SurveyApi.java`
- `looky-api/src/main/java/com/looky/api/survey/SurveyController.java`
- `looky-api/src/main/java/com/looky/api/survey/dto/SurveyStatusResponse.java`
- `looky-api/src/main/java/com/looky/api/survey/dto/SurveyResultResponse.java`

### 설문 생성/상태 계산

- `looky-core/src/main/java/com/looky/survey/application/SurveyCommandService.java`
- `looky-core/src/main/java/com/looky/survey/application/SurveyService.java`
- `looky-core/src/main/java/com/looky/survey/application/SurveyPolicy.java`

### 결과 생성/조회

- `looky-core/src/main/java/com/looky/result/application/ResultGenerationService.java`
- `looky-core/src/main/java/com/looky/result/application/ResultQueryService.java`
- `looky-core/src/main/java/com/looky/result/application/ResultRepository.java`
- `looky-core/src/main/java/com/looky/result/application/ResultGeneratorClient.java`

### infrastructure

- `looky-infrastructure/src/main/java/com/looky/result/scheduler/ResultGenerationScheduler.java`
- `looky-infrastructure/src/main/java/com/looky/result/client/FakeResultGeneratorClient.java`
- `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultRepositoryImpl.java`
- `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyRepositoryImpl.java`

### 테스트

- `looky-api/src/test/java/com/looky/api/survey/SurveyControllerTest.java`
- `looky-api/src/test/java/com/looky/api/survey/SurveyResultFlowIntegrationTest.java`
- `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`
- `looky-core/src/test/java/com/looky/result/application/ResultQueryServiceTest.java`
- `looky-core/src/test/java/com/looky/survey/application/SurveyCommandServiceTest.java`
- `looky-infrastructure/src/test/java/com/looky/result/client/FakeResultGeneratorClientTest.java`

## 검증 명령

가장 먼저 보는 검증 명령은 아래 두 개다.

```bash
./gradlew test
./gradlew :looky-api:bootJar
```

기능별로 빠르게 확인하려면 아래를 쓴다.

```bash
./gradlew :looky-api:test --tests "com.looky.api.survey.SurveyControllerTest"
./gradlew :looky-api:test --tests "com.looky.api.survey.SurveyResultFlowIntegrationTest"
./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"
./gradlew :looky-core:test --tests "com.looky.result.application.ResultQueryServiceTest"
./gradlew :looky-core:test --tests "com.looky.survey.application.SurveyCommandServiceTest"
./gradlew :looky-infrastructure:test --tests "com.looky.result.client.FakeResultGeneratorClientTest"
```

## 아직 의도적으로 안 한 것

이번 범위에서 제외한 내용이다.

- 실제 AI 결과 생성
- S3 업로드
- CDN 배포
- 결과 재생성 API
- 관리자 내부 API
- 스케줄러 실행 이력
- 분산락과 다중 인스턴스 제어

즉, 지금 결과 생성은 `FakeResultGeneratorClient` 기반 MVP다.

## 동료가 바로 이어서 하기 좋은 다음 작업

### 추천 1순위

프론트 연동 기준을 더 단단하게 만들기

- 결과 조회 API 상태별 문구 확정
- 프론트 폴링 정책 합의
- `SurveyStatusResponse`와 `SurveyResultResponse` 역할 차이 정리

추천 이유:

- 지금 백엔드 내부 플로우는 어느 정도 닫혔다.
- 다음 병목은 프론트가 어떤 상태를 믿고 붙을지다.

### 추천 2순위

결과 생성 실패 복구 전략 만들기

- `FAILED` 상태 재시도 정책
- 관리자/내부 재생성 API 여부
- 실패 원인 추적 로그 체계 보강

추천 이유:

- 지금은 실패하면 `FAILED`로 끝난다.
- 운영을 시작하면 가장 먼저 아쉬운 부분이 된다.

### 추천 3순위

fake generator를 real generator로 치환하기 위한 인터페이스 확장

- prompt 입력 구조
- 이미지 저장 구조
- 사분면별 메타데이터

추천 이유:

- 현재 인터페이스 분리는 이미 되어 있다.
- 다만 실제 AI/S3로 넘어가려면 반환 모델과 에러 모델이 더 구체화되어야 한다.

## 작업 시작 추천 순서

1. `main` 최신 pull
2. `./gradlew test`
3. `./gradlew :looky-api:bootJar`
4. 이 문서 읽기
5. `SurveyApi`, `ResultQueryService`, `ResultGenerationService` 순서로 읽기
6. 필요한 기능 브랜치 생성 후 작업 시작

## 참고 문서

- `docs/superpowers/specs/2026-06-23-result-generation-scheduler-design.md`
- `docs/superpowers/plans/2026-06-23-result-generation-scheduler.md`

## 참고 커밋

- `f05eef6 feat: 설문 1차 API 구조 추가`
- `b2ba4db feat: 결과 조회 read model 추가`
- `d2374fa feat: 결과 생성 유스케이스 추가`
- `306d12f feat: 결과 생성 스케줄러 추가`
- `7c2e069 feat: 결과 오픈 지연 설정화`
- `a7841b7 feat: 결과 조회 상태형 계약 반영`
# AI 결과 생성 후속 반영

- 완료된 SELF/PEER 응답에서 형용사와 조하리 창 사분면 해석·이미지 프롬프트를 생성한다.
- 텍스트 분석은 `gpt-5.4-mini`, 이미지는 `gpt-image-2`를 사용한다.
- 이미지는 private S3(`app-contents-dev`, `ap-northeast-2`)에 저장하고 결과 조회 시 24시간 presigned URL을 반환한다.
- 이미지 품질은 `looky.result-generation.image-quality`로 설정하며 기본값은 `low`다.
- 사분면별 상태와 시도 횟수를 저장한다. 성공한 사분면은 재생성하지 않으며 실패한 사분면만 최대 3회 재시도한다.
- 모든 사분면이 `IMAGE_READY`일 때만 결과 상태를 `READY`로 전환한다.
