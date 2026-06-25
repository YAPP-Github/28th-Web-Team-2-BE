# Character Pack Result Image Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist survey character-pack snapshots, pick quadrant variants from pack metadata, and generate result images through reference-asset-based OpenAI edit flow.

**Architecture:** Add pack metadata and snapshot columns at DB level first, then thread the snapshot through core survey/result contracts. Keep pack lookup and persistence in core/infrastructure boundaries, and switch image generation from prompt-only bytes to a request object that carries prompt plus reference asset bytes/keys.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JPA, Flyway, OpenAI Java SDK, AWS S3, JUnit 5

---

### Task 1: Schema And Seed

**Files:**
- Create: `looky-infrastructure/src/main/resources/db/migration/V4__add_character_pack_result_assets.sql`
- Modify: `looky-infrastructure/src/main/resources/db/data.sql`

- [ ] Add `character_packs`, `character_pack_versions`, `character_pack_variants` tables.
- [ ] Add `surveys.character_pack_key`, `surveys.character_pack_version`, `result_quadrants.selected_variant_key`.
- [ ] Seed one active pack/version and four quadrant variants with stable S3 object keys.
- [ ] Mirror minimal seed data in `db/data.sql` for `test` profile.

### Task 2: Core Snapshot And Variant Contracts

**Files:**
- Create: `looky-core/src/main/java/com/looky/characterpack/application/CharacterPackRepository.java`
- Create: `looky-core/src/main/java/com/looky/characterpack/application/CharacterPackVariantRecord.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultImageRequest.java`
- Modify: `looky-core/src/main/java/com/looky/survey/application/SurveyRecord.java`
- Modify: `looky-core/src/main/java/com/looky/survey/application/SurveyRepository.java`
- Modify: `looky-core/src/main/java/com/looky/survey/application/SurveyCommandService.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultQuadrantRecord.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultImageClient.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultGenerationService.java`
- Test: `looky-core/src/test/java/com/looky/survey/application/SurveyCommandServiceTest.java`
- Test: `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`

- [ ] Write failing tests for survey snapshot persistence and quadrant variant selection.
- [ ] Add active pack/version lookup to survey creation.
- [ ] Add `selectedVariantKey` to quadrant record and request object boundary for image generation.
- [ ] Make result generation choose first variant by lowest `sort_order` for each quadrant from survey snapshot.

### Task 3: Infrastructure Persistence

**Files:**
- Create: `looky-infrastructure/src/main/java/com/looky/characterpack/persistence/CharacterPackJpaEntity.java`
- Create: `looky-infrastructure/src/main/java/com/looky/characterpack/persistence/CharacterPackVersionJpaEntity.java`
- Create: `looky-infrastructure/src/main/java/com/looky/characterpack/persistence/CharacterPackVariantJpaEntity.java`
- Create: `looky-infrastructure/src/main/java/com/looky/characterpack/persistence/CharacterPackJpaRepository.java`
- Create: `looky-infrastructure/src/main/java/com/looky/characterpack/persistence/CharacterPackVariantJpaRepository.java`
- Create: `looky-infrastructure/src/main/java/com/looky/characterpack/persistence/CharacterPackRepositoryImpl.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyRepositoryImpl.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultQuadrantJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultRepositoryImpl.java`
- Test: `looky-infrastructure/src/test/java/com/looky/result/persistence/ResultQuadrantJpaEntityTest.java`

- [ ] Map new snapshot and variant columns in JPA entities.
- [ ] Implement pack lookup repository for active version + variants.
- [ ] Persist `selected_variant_key` when quadrant image work succeeds.

### Task 4: Image Edit Flow

**Files:**
- Modify: `looky-infrastructure/src/main/java/com/looky/result/client/TestResultImageClient.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/client/OpenAiResultImageClient.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/storage/S3ResultImageStorage.java`
- Test: `looky-api/src/test/java/com/looky/api/survey/ResultImageRetryIntegrationTest.java`

- [ ] Write failing retry/integration assertions for selected variant tracking.
- [ ] Change test client to accept `ResultImageRequest`.
- [ ] Fetch reference asset bytes from S3 and call OpenAI image edit with base + variant inputs.
- [ ] Keep retry behavior limited to failed quadrants only.

### Task 5: Verify

**Files:**
- Test: `looky-core/src/test/java/com/looky/survey/application/SurveyCommandServiceTest.java`
- Test: `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`
- Test: `looky-api/src/test/java/com/looky/api/survey/SurveyResultFlowIntegrationTest.java`
- Test: `looky-api/src/test/java/com/looky/api/survey/ResultImageRetryIntegrationTest.java`

- [ ] Run `./gradlew --gradle-user-home /Users/kangchaewon/Documents/Projects/YAPP28th/28th-Web-Team-2/backend/.gradle-local :looky-core:test --tests "com.looky.survey.application.SurveyCommandServiceTest"`
- [ ] Run `./gradlew --gradle-user-home /Users/kangchaewon/Documents/Projects/YAPP28th/28th-Web-Team-2/backend/.gradle-local :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"`
- [ ] Run `./gradlew --gradle-user-home /Users/kangchaewon/Documents/Projects/YAPP28th/28th-Web-Team-2/backend/.gradle-local :looky-api:test --tests "com.looky.api.survey.SurveyResultFlowIntegrationTest"`
- [ ] Run `./gradlew --gradle-user-home /Users/kangchaewon/Documents/Projects/YAPP28th/28th-Web-Team-2/backend/.gradle-local :looky-api:test --tests "com.looky.api.survey.ResultImageRetryIntegrationTest"`
- [ ] Run `./gradlew --gradle-user-home /Users/kangchaewon/Documents/Projects/YAPP28th/28th-Web-Team-2/backend/.gradle-local :looky-api:bootJar`
