# Character Pack Result Image Design

## Goal

현재 결과 생성 파이프라인을 유지하면서, 결과 이미지 생성에 캐릭터팩 기반의 reference asset을 주입할 수 있게 만든다. 운영에서는 OpenAI 이미지 생성 호출을 사용하고, 로컬과 테스트에서는 기존 stub 경로를 유지한다.

이번 범위는 다음에 한정한다.

- 캐릭터팩 메타데이터 저장
- 활성 pack/version 관리
- 설문 생성 시 pack/version 스냅샷 저장
- 결과 생성 시 quadrant별 reference asset 선택
- 최종 결과 이미지 S3 저장

이번 범위에서 제외한다.

- 결과 공개 조건 변경
- 테스트용 peer 수 또는 공개 시간 완화
- 운영자용 웹 UI
- 다중 이미지 공급자 추상화
- 설문별 pack override
- `ResultGeneratorClient` 경로 정리

## Existing Constraints

- 결과 공개 조건은 기존과 동일하게 유지한다.
  - SELF 제출 완료
  - PEER 3명 이상 제출 완료
  - `resultAvailableAt` 도달
- 결과 생성은 현재 스케줄러 기반 흐름을 유지한다.
- 기존 `ResultGenerationService -> ResultImageClient -> ResultImageStorage` 경계는 유지한다.
- 설문 생성 시 계산된 `resultAvailableAt`은 설문 row에 저장되는 스냅샷이다. 이후 설정 변경이 기존 설문에 소급 적용되면 안 된다.

## Recommended Shape

이번 구현은 "현재 파이프라인 확장형"으로 간다.

- 결과 생성 흐름은 유지한다.
- 캐릭터팩 도메인을 추가한다.
- 설문에는 `characterPackKey`, `characterPackVersion`만 스냅샷으로 저장한다.
- 활성 pack/version은 별도 전역 설정 테이블을 만들지 않고 캐릭터팩 버전 테이블 내부에서 해결한다.
- 내부 API는 전체 CRUD가 아니라 "등록, 조회, 활성 전환" 중심으로 제한한다.
- 이미지 생성 요청 객체는 최소 필드만 갖는다.

## Architecture

### looky-core

- `CharacterPackRepository`를 추가한다.
- `ActiveCharacterPackResolver` 또는 이에 준하는 코어 서비스가 현재 활성 pack/version을 조회한다.
- 설문 생성 시 활성 pack/version을 읽어 설문 스냅샷에 저장한다.
- 결과 생성 시 설문 스냅샷 기준으로 pack/version과 quadrant용 variant를 조회한다.
- `ResultImageClient`는 문자열 prompt 대신 요청 객체를 입력으로 받는다.

### looky-infrastructure

- 캐릭터팩 메타데이터용 JPA 엔티티와 repository 구현을 둔다.
- base asset과 variant asset은 S3 object key로만 저장한다.
- OpenAI 이미지 클라이언트는 prompt와 reference asset key 목록을 받아 실제 호출을 수행한다.
- 최종 결과 이미지는 기존 `S3ResultImageStorage`에 저장한다.

### looky-api

- 내부 운영 API를 추가한다.
- 설문 생성 API는 pack/version을 직접 받지 않는다.
- 외부 공개 결과 조회 API는 바꾸지 않는다.

## Data Model

### Survey Snapshot

설문 저장소에 아래 필드를 추가한다.

- `character_pack_key`
- `character_pack_version`

의미는 "이 설문 결과 이미지는 어떤 캐릭터팩 기준으로 생성되어야 하는가"이다.

활성 pack/version이 이후 바뀌어도, 이미 생성된 설문은 저장된 스냅샷 기준으로 계속 생성해야 한다.

### Character Pack Tables

#### `character_packs`

- `pack_key`
- `display_name`
- `description`
- `created_at`
- `updated_at`

#### `character_pack_versions`

- `pack_id`
- `version`
- `base_asset_object_key`
- `active`
- `created_at`
- `updated_at`

`active`는 전역에서 정확히 하나의 pack/version 조합만 활성이라는 규칙으로 사용한다. 별도 설정 테이블은 만들지 않는다.

#### `character_pack_variants`

- `pack_version_id`
- `variant_key`
- `label`
- `recommended_quadrant_type`
- `reference_asset_object_key`
- `sort_order`
- `created_at`
- `updated_at`

이번 범위에서는 `recommended_quadrant_type` 기반의 단순 규칙으로 quadrant별 기본 variant를 선택한다.

### Result Quadrant Trace

`result_quadrants`에 아래 필드를 추가한다.

- `selected_variant_key`

운영 추적성을 위해 어떤 variant로 생성했는지 남긴다.

## S3 Layout

원본 자산과 최종 결과를 prefix로 분리한다.

- `character-packs/{packKey}/{version}/base/...`
- `character-packs/{packKey}/{version}/variants/{variantKey}.png`
- `surveys/{surveyCode}/results/{quadrant}.png`

이번 범위에서는 메타데이터에 S3 object key만 저장하고, 업로드 UI나 배치 업로더는 포함하지 않는다.

## Internal API Scope

이번 범위의 내부 API는 전체 CRUD가 아니라 아래만 지원한다.

- pack 등록
- pack version 등록
- variant 등록
- pack/version 조회
- 활성 pack/version 조회
- 활성 pack/version 전환

삭제, 수정 이력, 파일 업로드, 대량 동기화 API는 제외한다.

보호 방식은 기존 debug endpoint처럼 최소한의 내부 보호 장치를 둔다. 자세한 인증 체계 확장은 이번 범위 밖이다.

## Image Generation Request

`ResultImageClient` 입력은 최소 요청 객체로 바꾼다.

예시 필드:

- `surveyCode`
- `quadrantType`
- `imagePrompt`
- `packKey`
- `packVersion`
- `referenceAssetObjectKeys`

이번 범위에서는 아래를 요청 객체 필수 필드로 넣지 않는다.

- `interpretation`
- `base asset key`
- 기타 확장용 메타데이터

필요하면 다음 확장에서 추가한다. 1차는 prompt와 reference asset 목록이면 충분하다.

## Variant Selection Rule

복잡한 추천 엔진은 만들지 않는다.

- 각 variant는 `recommended_quadrant_type`을 가진다.
- 결과 생성 시 quadrant 타입에 맞는 variant를 우선 선택한다.
- 동일 quadrant에 여러 variant가 있으면 `sort_order`가 가장 낮은 것을 먼저 사용한다.

이번 범위에서는 quadrant당 1개 variant 선택만 지원한다. 여러 reference asset 조합은 이후 확장 포인트로 둔다.

## Runtime Flow

1. 운영자가 내부 API로 pack, version, variant 메타데이터를 등록한다.
2. 운영자가 하나의 pack/version을 활성 상태로 전환한다.
3. 사용자가 새 설문을 생성하면, 활성 pack/version을 설문 row에 스냅샷 저장한다.
4. 기존 공개 조건이 충족되면 결과 생성 스케줄러가 survey를 후보로 집는다.
5. 결과 narrative가 없으면 기존 방식대로 narrative와 quadrant image prompt를 생성한다.
6. 각 quadrant에 대해 설문 스냅샷 기준 pack/version을 조회한다.
7. quadrant 타입에 맞는 variant를 선택한다.
8. `ResultImageClient`에 prompt와 reference asset key 목록을 넘긴다.
9. 생성된 이미지를 기존 `ResultImageStorage`로 업로드한다.
10. 업로드 key와 `selected_variant_key`를 저장한다.
11. 모든 quadrant가 완료되면 결과를 `READY`로 전환한다.

## Error Handling

- 설문에 저장된 pack/version을 찾지 못하면 결과 생성 실패로 처리한다.
- variant 메타데이터가 없거나 reference asset key가 비어 있으면 quadrant 실패로 처리한다.
- OpenAI 호출 실패는 기존처럼 quadrant 단위 재시도를 탄다.
- 일부 quadrant만 실패했으면 성공한 quadrant는 재생성하지 않는다.
- 최대 재시도 횟수를 넘긴 quadrant가 있으면 survey 결과 상태를 `FAILED`로 올린다.

이번 범위에서도 실패 단위는 survey 전체가 아니라 quadrant다.

## Testing Strategy

### Core Tests

- 설문 생성 시 활성 pack/version 스냅샷 저장
- 결과 생성 시 설문 스냅샷 기준 pack/version 조회
- quadrant별 variant 선택 규칙
- quadrant 실패 시 다른 quadrant 재생성 없이 재시도

### Infrastructure Tests

- character pack JPA 매핑
- 활성 version 조회
- S3 key prefix 분리
- OpenAI 이미지 요청 조립 시 reference asset key 전달

### API Tests

- 내부 API 등록
- 내부 API 조회
- 활성 전환

### Integration Tests

- 새 설문 생성
- SELF 1건 + PEER 3건 제출
- 결과 생성 실행
- 각 quadrant 이미지가 캐릭터팩 기준으로 생성되고 업로드 key가 저장되는지 검증

## Deliberate Non-Goals

- 테스트 전용 peer 수 조절
- 테스트 전용 공개 시간 완화
- 운영자 UI
- 다중 provider 지원
- 설문별 pack override
- `ResultGeneratorClient` 제거 또는 구조 재정리

## Implementation Notes

이 설계의 핵심은 "기존 흐름을 보존하면서 캐릭터 자산 선택 책임만 얹는다"이다. 신규 기능보다 주변 공사가 더 커지는 것을 피하기 위해, 전역 설정 저장소와 과도한 내부 CRUD, 확장용 메타데이터는 1차에서 의도적으로 제외한다.
