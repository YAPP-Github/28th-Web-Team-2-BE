# Result Generation Scheduler Design

## Goal

3차 목표는 조건을 만족한 설문에 대해 서버가 1분 주기로 fake 결과를 생성하고, 결과 조회 API가 실제 저장된 read model을 반환할 수 있게 만드는 것이다.

## Confirmed Decisions

- 결과 생성 방식은 MVP용 fake generator로 구현한다.
- 스케줄러는 1분마다 실행한다.
- 스케줄러는 매 주기마다 생성 조건을 만족한 설문을 조회한다.
- 대상 조건은 `SELF` 제출 완료, 완료된 `PEER` 응답 수가 `requiredPeerSubmissionCount` 이상, `resultAvailableAt <= now`, 결과 상태가 아직 완료 전인 설문이다.
- 상태 전이는 `GENERATING` 저장 후 결과 생성과 저장을 수행하고, 성공 시 `READY`, 실패 시 `FAILED`로 저장한다.
- fake 이미지 URL은 `https://cdn.looky.my/results/{surveyCode}/{quadrant}.png` 형식으로 저장한다.
- 이번 범위에는 실제 AI 호출, S3 업로드, CDN 배포, 관리자 API, 수동 재생성 API를 포함하지 않는다.

## Architecture

스케줄러는 infrastructure 레이어에 둔다. 스케줄러는 시간 기반 트리거만 담당하고, 업무 규칙은 core의 application service가 담당한다.

core에는 결과 생성 유스케이스를 담당하는 `ResultGenerationService`를 추가한다. 이 서비스는 `SurveyRepository`, `SubmissionRepository`, `ResultRepository`, `ResultGeneratorClient`를 조율한다. 외부 기술이나 Spring Scheduler 세부 구현은 알지 않는다.

`ResultGeneratorClient`는 core에 인터페이스로 둔다. 3차에서는 infrastructure에 `FakeResultGeneratorClient` 구현을 두고, 나중에 AI/S3 기반 구현체로 교체할 수 있게 한다.

## Data Flow

1. `ResultGenerationScheduler`가 1분마다 실행된다.
2. 스케줄러는 `ResultGenerationService.generateReadyResults()`를 호출한다.
3. 서비스는 현재 시각 기준으로 생성 후보 설문을 조회한다.
4. 각 후보 설문에 대해 `resultStatus`를 `GENERATING`으로 저장한다.
5. `ResultGeneratorClient`가 사분면별 이미지 URL 4개를 생성한다.
6. `ResultRepository`가 `results`와 `result_quadrants`를 저장한다.
7. 저장 성공 시 설문 상태를 `READY`로 변경한다.
8. 생성 또는 저장 중 예외가 발생하면 설문 상태를 `FAILED`로 변경하고 다음 후보 처리를 계속한다.

## Eligibility Rules

생성 대상 설문은 다음 조건을 모두 만족해야 한다.

- `resultStatus`는 `WAITING_SELF_RESPONSE`, `COLLECTING_PEER_RESPONSES`, `WAITING_RESULT_OPEN_TIME` 중 하나여야 한다.
- `resultStatus`가 `GENERATING`, `READY`, `FAILED`, `EXPIRED`인 설문은 제외한다.
- 완료된 `SELF` 응답이 존재해야 한다.
- 완료된 `PEER` 응답 수가 설문의 `requiredPeerSubmissionCount` 이상이어야 한다.
- 현재 시각이 `resultAvailableAt` 이상이어야 한다.
- 이미 `results` row가 있는 설문은 제외한다.

구현에서는 상태 조건을 단순하게 유지하기 위해 repository 조회 단계에서 결과 미완료 상태만 가져오고, service에서 응답 수와 시간 조건을 한 번 더 검증한다.

## State Transitions

- 후보 검증 성공: `WAITING_SELF_RESPONSE`, `COLLECTING_PEER_RESPONSES`, `WAITING_RESULT_OPEN_TIME` 중 현재 DB 상태에서 `GENERATING`
- 결과 저장 성공: `GENERATING`에서 `READY`
- 결과 생성 또는 저장 실패: `GENERATING`에서 `FAILED`
- 이미 `READY`, `FAILED`, `EXPIRED`인 설문은 스케줄러가 변경하지 않는다.

현재 `SurveyCommandService.getSurveyStatus()`는 상태를 계산해서 응답하지만 DB에 저장하지 않는다. 3차에서는 스케줄러가 최종 생성 흐름의 상태 저장 책임을 가진다.

## File Responsibilities

### Core

- `com.looky.result.application.ResultGenerationService`
  - 스케줄러에서 호출하는 결과 생성 유스케이스
  - 후보 검증, 상태 전이, generator 호출, 결과 저장 orchestration

- `com.looky.result.application.ResultGeneratorClient`
  - 사분면 이미지 URL 생성 추상화
  - fake 구현과 향후 AI/S3 구현 교체 지점

- `com.looky.result.application.GeneratedResult`
  - generator가 반환하는 사분면별 URL 묶음

- `com.looky.result.application.ResultRepository`
  - 기존 `findBySurveyId` 유지
  - 결과 존재 여부 확인과 결과 저장 메서드 추가

- `com.looky.survey.application.SurveyRepository`
  - 생성 후보 조회 메서드 추가
  - `resultStatus` 변경 메서드 추가

### Infrastructure

- `com.looky.result.scheduler.ResultGenerationScheduler`
  - `@Scheduled(fixedDelayString = "${looky.result-generation.fixed-delay:60000}")`
  - 스케줄링만 담당하고 업무 규칙은 core service에 위임

- `com.looky.api.LookyApiApplication`
  - `@EnableScheduling`을 추가해 infrastructure scheduler bean이 동작하게 함

- `com.looky.result.client.FakeResultGeneratorClient`
  - `https://cdn.looky.my/results/{surveyCode}/{quadrant}.png` URL 생성

- `com.looky.result.persistence.ResultRepositoryImpl`
  - 결과 존재 여부 확인
  - `results`, `result_quadrants` 저장

- `com.looky.survey.persistence.SurveyRepositoryImpl`
  - 생성 후보 조회
  - `resultStatus` 업데이트

## Error Handling

개별 설문 생성 실패는 전체 스케줄러 실행 실패로 전파하지 않는다. 실패한 설문만 `FAILED`로 저장하고, 남은 후보 설문 처리를 계속한다.

fake generator는 정상 입력에서는 실패하지 않는다. 테스트에서는 실패하는 generator를 주입해 `FAILED` 전이를 검증한다.

## Concurrency

3차에서는 단일 애플리케이션 인스턴스를 전제로 한다. 같은 설문을 중복 생성하지 않기 위해 service에서 결과 존재 여부를 확인하고, `results.survey_id` unique 제약을 유지한다.

다중 인스턴스 환경의 분산락, `SKIP LOCKED`, 재시도 큐는 이번 범위에서 제외한다.

## Testing

- core 단위 테스트로 후보가 없는 경우, 준비 조건 충족 시 `GENERATING → READY`, generator 실패 시 `FAILED`, 이미 결과가 있으면 skip하는 흐름을 검증한다.
- API 테스트는 변경하지 않는다. 2차에서 결과 조회 계약은 이미 검증했다.
- infrastructure는 compile과 bootJar로 JPA 메서드명, entity scan, scheduler 설정을 검증한다.
- 최종 검증은 `./gradlew test`와 `./gradlew :looky-api:bootJar`로 수행한다.

## Out of Scope

- 실제 AI 프롬프트 생성
- S3 업로드와 public URL 발급
- CDN invalidation
- 결과 재생성 API
- 관리자용 내부 API
- 스케줄러 실행 이력 테이블
- 분산락
