# Trait-Balanced Question Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Assign exactly two random active questions from each of the four trait codes whenever a SELF or PEER submission starts.

**Architecture:** The core question record and repository contract carry `TraitCode`. Infrastructure loads active questions per trait and independently shuffles each group. `SurveyCommandService` requests the fixed balanced selection and retains `NOT_ENOUGH_ACTIVE_QUESTIONS` when any group has fewer than two questions.

**Tech Stack:** Java 25, Spring Data JPA, JUnit 5, H2 integration tests.

---

### Task 1: Define the trait-aware core contract

**Files:**
- Create: `looky-core/src/main/java/com/looky/question/domain/TraitCode.java`
- Modify: `looky-core/src/main/java/com/looky/question/application/QuestionRecord.java`
- Modify: `looky-core/src/main/java/com/looky/question/application/QuestionRepository.java`
- Modify: `looky-core/src/main/java/com/looky/survey/application/SurveyCommandService.java`
- Test: `looky-core/src/test/java/com/looky/survey/application/SurveyCommandServiceTest.java`

- [ ] **Step 1: Write the failing balanced-selection test**

```java
assertEquals(8, result.questions().size());
assertEquals(2, result.questions().stream().filter(q -> q.traitCode() == TraitCode.OPENNESS).count());
assertEquals(2, result.questions().stream().filter(q -> q.traitCode() == TraitCode.CONSCIENTIOUSNESS).count());
assertEquals(2, result.questions().stream().filter(q -> q.traitCode() == TraitCode.EXTRAVERSION).count());
assertEquals(2, result.questions().stream().filter(q -> q.traitCode() == TraitCode.AGREEABLENESS).count());
```

- [ ] **Step 2: Run the test to verify failure**

Run: `./gradlew :looky-core:test --tests "com.looky.survey.application.SurveyCommandServiceTest"`

Expected: FAIL because `QuestionRecord` has no trait code and the fake repository cannot model balanced selection.

- [ ] **Step 3: Add the core types and balanced repository method**

```java
public enum TraitCode {
    OPENNESS, CONSCIENTIOUSNESS, EXTRAVERSION, AGREEABLENESS
}

public record QuestionRecord(Long questionId, TraitCode traitCode, String content, List<AnswerOptionRecord> options) {}

public interface QuestionRepository {
    List<QuestionRecord> findRandomActiveQuestionsByTrait(int countPerTrait, SubmitterType submitterType);
}
```

Set `QUESTION_COUNT_PER_TRAIT = 2` in `SurveyCommandService`, call the new repository method, and require exactly `TraitCode.values().length * QUESTION_COUNT_PER_TRAIT` records.

- [ ] **Step 4: Update all test fixtures to populate the four trait codes**

Make the fake question repository emit two or more records for each enum value. Preserve its answer-option fixture data so answer submission tests keep exercising validation.

- [ ] **Step 5: Run the core test class**

Run: `./gradlew :looky-core:test --tests "com.looky.survey.application.SurveyCommandServiceTest"`

Expected: PASS.

### Task 2: Implement trait-grouped JPA selection

**Files:**
- Modify: `looky-infrastructure/src/main/java/com/looky/question/persistence/QuestionJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/question/persistence/QuestionJpaRepository.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/question/persistence/QuestionRepositoryImpl.java`
- Test: `looky-infrastructure/src/test/java/com/looky/question/persistence/QuestionRepositoryImplTest.java`

- [ ] **Step 1: Write failing persistence tests**

```java
List<QuestionRecord> questions = repository.findRandomActiveQuestionsByTrait(2, SubmitterType.SELF);
assertEquals(8, questions.size());
for (TraitCode traitCode : TraitCode.values()) {
    assertEquals(2, questions.stream().filter(question -> question.traitCode() == traitCode).count());
}
```

Also seed one trait with one active row and assert the method returns that incomplete group; `SurveyCommandService` owns conversion to `NOT_ENOUGH_ACTIVE_QUESTIONS`.

- [ ] **Step 2: Run the persistence test to verify failure**

Run: `./gradlew :looky-infrastructure:test --tests "com.looky.question.persistence.QuestionRepositoryImplTest"`

Expected: FAIL because JPA entity/repository do not expose `trait_code`.

- [ ] **Step 3: Map and select by trait**

```java
@Enumerated(EnumType.STRING)
@Column(name = "trait_code", nullable = false, length = 40)
private TraitCode traitCode;

List<QuestionJpaEntity> findByActiveTrueAndTraitCode(TraitCode traitCode);
```

For each `TraitCode`, load active rows, shuffle a defensive `ArrayList`, take `countPerTrait`, and map to a `QuestionRecord` carrying the trait. Do not rely on one global `limit(8)` query.

- [ ] **Step 4: Run focused infrastructure tests**

Run: `./gradlew :looky-infrastructure:test --tests "com.looky.question.persistence.QuestionRepositoryImplTest"`

Expected: PASS.

### Task 3: Verify API-level assignment behavior

**Files:**
- Modify: `looky-api/src/test/java/com/looky/api/survey/SurveyResultFlowIntegrationTest.java`

- [ ] **Step 1: Add an integration assertion for a started SELF submission**

```java
SubmissionStartedResult submission = surveyService.startSubmission(survey.surveyCode());
assertEquals(8, submission.questions().size());
```

Persisted fixture data must provide two active questions per known trait before this test runs.

- [ ] **Step 2: Run the integration test**

Run: `./gradlew :looky-api:test --tests "com.looky.api.survey.SurveyResultFlowIntegrationTest"`

Expected: PASS.

- [ ] **Step 3: Run complete verification**

Run: `./gradlew test && ./gradlew :looky-api:bootJar`

Expected: both commands exit `0`.
