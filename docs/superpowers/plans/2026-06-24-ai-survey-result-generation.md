# AI Survey Result Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate and serve four Johari-window interpretations and private-S3 images from completed survey responses, retrying only failed work units.

**Architecture:** Core owns source, narrative, image, storage, and signing ports. Infrastructure implements OpenAI Java SDK calls and private S3 storage. Result persistence records per-answer adjectives and per-quadrant interpretation, prompt, object key, status, and attempts; API signs stored keys only at read time.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JPA, OpenAI Java SDK, AWS SDK v2 S3, JUnit 5, H2.

---

### Task 1: Add dependency and typed configuration boundaries

**Files:**
- Modify: `looky-infrastructure/build.gradle`
- Modify: `looky-api/src/main/resources/application.yaml`
- Create: `looky-api/src/main/java/com/looky/api/support/spring/OpenAiResultGenerationConfig.java`
- Create: `looky-api/src/main/java/com/looky/api/support/spring/S3ResultStorageConfig.java`
- Test: `looky-api/src/test/java/com/looky/api/support/spring/ResultGenerationPropertiesTest.java`

- [ ] **Step 1: Write failing configuration-binding tests**

```java
assertEquals("gpt-5.4-mini", properties.narrativeModel());
assertEquals("gpt-image-2", properties.imageModel());
assertEquals("low", properties.imageQuality());
assertEquals(Duration.ofHours(24), properties.presignedUrlTtl());
```

- [ ] **Step 2: Run the configuration test to verify failure**

Run: `./gradlew :looky-api:test --tests "com.looky.api.support.spring.ResultGenerationPropertiesTest"`

Expected: FAIL because no typed result-generation properties exist.

- [ ] **Step 3: Add SDK dependencies and configuration**

Add `com.openai:openai-java` and AWS SDK v2 S3 modules only to infrastructure. Bind `looky.result-generation` properties for models, quality, bucket, region, and `presigned-url-ttl=24h`; obtain credentials from the SDK default provider chain. Never add API keys or AWS credentials to YAML.

- [ ] **Step 4: Re-run the focused configuration test**

Run: `./gradlew :looky-api:test --tests "com.looky.api.support.spring.ResultGenerationPropertiesTest"`

Expected: PASS.

### Task 2: Persist source analysis and quadrant work state

**Files:**
- Create: `looky-core/src/main/java/com/looky/result/application/ResultAnswerAdjectiveRecord.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultGenerationSourceReader.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultRepository.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultQuadrantRecord.java`
- Create: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultAnswerAdjectiveJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultQuadrantJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultRepositoryImpl.java`
- Test: `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`

- [ ] **Step 1: Write failing tests for persisted adjective and partial quadrant state**

```java
assertEquals(List.of("thoughtful", "curious"), savedAdjectives.getFirst().adjectives());
assertEquals(QuadrantWorkStatus.IMAGE_READY, savedQuadrants.get(ResultQuadrantType.OPEN).status());
assertEquals(QuadrantWorkStatus.FAILED, savedQuadrants.get(ResultQuadrantType.BLIND).status());
```

- [ ] **Step 2: Run the result generation test to verify failure**

Run: `./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"`

Expected: FAIL because there are no adjective records or per-quadrant statuses.

- [ ] **Step 3: Add the model and repository operations**

Define `QuadrantWorkStatus { PENDING, NARRATIVE_READY, IMAGE_READY, FAILED }`. Store `interpretation`, `imagePrompt`, `s3ObjectKey`, `status`, `attemptCount`, and `failureReason` per quadrant. Store each source answer's submission ID, question ID, trait code, answer snapshot, and adjective JSON. Add repository methods that load only incomplete or retryable units.

- [ ] **Step 4: Re-run the result generation unit test**

Run: `./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"`

Expected: PASS.

### Task 3: Read completed response snapshots and generate narratives

**Files:**
- Create: `looky-core/src/main/java/com/looky/result/application/ResultNarrativeClient.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultNarrative.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultGenerationService.java`
- Modify: `looky-core/src/main/java/com/looky/submission/application/SubmissionRepository.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/submission/persistence/SubmissionRepositoryImpl.java`
- Create: `looky-infrastructure/src/main/java/com/looky/result/client/OpenAiResultNarrativeClient.java`
- Test: `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`

- [ ] **Step 1: Write failing narrative tests**

```java
assertEquals("thoughtful", narrative.answerAdjectives().get(answerId).getFirst());
assertEquals("타인이 먼저 발견하는 강점", narrative.quadrants().get(ResultQuadrantType.BLIND).interpretation());
assertFalse(narrative.quadrants().get(ResultQuadrantType.BLIND).imagePrompt().isBlank());
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"`

Expected: FAIL because the narrative port is absent.

- [ ] **Step 3: Implement strict structured output parsing**

The OpenAI client sends raw SELF/PEER question and answer snapshots, requires one adjective list for every answer ID and all four quadrants, and rejects missing/blank fields. Persist extracted adjectives before persisting quadrant narratives. Do not log response text or API keys.

- [ ] **Step 4: Re-run the unit test**

Run: `./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"`

Expected: PASS.

### Task 4: Generate failed-only images and store them privately

**Files:**
- Create: `looky-core/src/main/java/com/looky/result/application/ResultImageClient.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultImageStorage.java`
- Create: `looky-infrastructure/src/main/java/com/looky/result/client/OpenAiResultImageClient.java`
- Create: `looky-infrastructure/src/main/java/com/looky/result/storage/S3ResultImageStorage.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultGenerationService.java`
- Test: `looky-infrastructure/src/test/java/com/looky/result/storage/S3ResultImageStorageTest.java`

- [ ] **Step 1: Write failing failed-quadrant-only tests**

```java
assertThat(imageClient.generatedQuadrants()).containsExactly(ResultQuadrantType.BLIND);
assertEquals("surveys/b91k2p8xq4z2/results/BLIND.png", storage.savedObjectKey());
```

- [ ] **Step 2: Run the focused tests to verify failure**

Run: `./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"`

Expected: FAIL because image work is still all-or-nothing.

- [ ] **Step 3: Implement independent image work**

Call `gpt-image-2` with the stored prompt and configured quality. Upload image bytes as `image/png` to `surveys/{surveyCode}/results/{quadrant}.png` with no public ACL. On success save the object key and `IMAGE_READY`; on failure increment only that quadrant's attempt count and preserve all successful units.

- [ ] **Step 4: Run storage and core tests**

Run: `./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest" && ./gradlew :looky-infrastructure:test --tests "com.looky.result.storage.S3ResultImageStorageTest"`

Expected: PASS.

### Task 5: Sign images and extend the result API

**Files:**
- Create: `looky-core/src/main/java/com/looky/result/application/ResultUrlSigner.java`
- Create: `looky-infrastructure/src/main/java/com/looky/result/storage/S3ResultUrlSigner.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultQueryService.java`
- Modify: `looky-core/src/main/java/com/looky/survey/application/dto/SurveyResultResult.java`
- Modify: `looky-api/src/main/java/com/looky/api/survey/dto/SurveyResultResponse.java`
- Modify: `looky-api/src/main/java/com/looky/api/survey/SurveyApi.java`
- Test: `looky-api/src/test/java/com/looky/api/survey/SurveyControllerTest.java`

- [ ] **Step 1: Write failing API contract tests**

```java
.andExpect(jsonPath("$.payload.quadrantImageUrls.OPEN").value("https://signed.example/open"))
.andExpect(jsonPath("$.payload.quadrantInterpretations.OPEN").value("서로 알고 있는 강점"));
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :looky-api:test --tests "com.looky.api.survey.SurveyControllerTest"`

Expected: FAIL because `quadrantInterpretations` does not exist and stored keys are returned directly.

- [ ] **Step 3: Implement signing at query time**

For `READY`, sign each stored key with `Duration.ofHours(24)` and return the signed URL plus interpretation. Keep `quadrantImageUrls` and the common response wrapper unchanged; add `quadrantInterpretations` as a nullable compatibility-safe field. Keep Swagger examples and descriptions in `SurveyApi`.

- [ ] **Step 4: Run API test class**

Run: `./gradlew :looky-api:test --tests "com.looky.api.survey.SurveyControllerTest"`

Expected: PASS.

### Task 6: End-to-end retry and final verification

**Files:**
- Modify: `looky-api/src/test/java/com/looky/api/survey/SurveyResultFlowIntegrationTest.java`
- Modify: `docs/handoffs/2026-06-23-survey-backend-handoff.md`

- [ ] **Step 1: Add integration coverage for partial recovery**

```java
generationService.generateReadyResults(); // BLIND image fails once
assertEquals(ResultStatus.GENERATING, queryService.getSurveyResult(code).resultStatus());
generationService.generateReadyResults(); // only BLIND retries
assertEquals(ResultStatus.READY, queryService.getSurveyResult(code).resultStatus());
```

Also assert that all four public URLs are signed and all four interpretations are present only after `READY`.

- [ ] **Step 2: Run the integration test**

Run: `./gradlew :looky-api:test --tests "com.looky.api.survey.SurveyResultFlowIntegrationTest"`

Expected: PASS.

- [ ] **Step 3: Update handoff documentation**

Document model names, private-S3/presigned behavior, the three-stage pipeline, per-unit retries, and the added result API field. Do not include credentials or raw survey content.

- [ ] **Step 4: Run final verification**

Run: `./gradlew test && ./gradlew :looky-api:bootJar`

Expected: both commands exit `0`.
