# Result Generation Scheduler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a 1-minute scheduler that generates fake result image URLs for eligible surveys and persists `GENERATING → READY/FAILED` state transitions.

**Architecture:** Keep scheduling in infrastructure and business flow in core. `ResultGenerationService` orchestrates repositories and a `ResultGeneratorClient`; infrastructure provides `FakeResultGeneratorClient`, JPA persistence methods, and the Spring scheduler.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Scheduler, Spring Data JPA, JUnit 5.

---

## File Structure

### Core

- Create: `looky-core/src/main/java/com/looky/result/application/GeneratedResult.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultGeneratorClient.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultGenerationService.java`
- Create: `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultRepository.java`
- Modify: `looky-core/src/main/java/com/looky/survey/application/SurveyRepository.java`
- Modify: `looky-core/src/test/java/com/looky/result/application/ResultQueryServiceTest.java`
- Modify: `looky-core/src/test/java/com/looky/survey/application/SurveyCommandServiceTest.java`

### Infrastructure

- Create: `looky-infrastructure/src/main/java/com/looky/result/client/FakeResultGeneratorClient.java`
- Create: `looky-infrastructure/src/test/java/com/looky/result/client/FakeResultGeneratorClientTest.java`
- Create: `looky-infrastructure/src/main/java/com/looky/result/scheduler/ResultGenerationScheduler.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultJpaRepository.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultRepositoryImpl.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaRepository.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyRepositoryImpl.java`

### API

- Modify: `looky-api/src/main/java/com/looky/api/LookyApiApplication.java`
- Modify: `looky-api/src/main/resources/application.yaml`

### Docs

- Modify: `docs/superpowers/specs/2026-06-23-result-generation-scheduler-design.md`

---

### Task 1: Core Result Generation Use Case

**Files:**
- Create: `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`
- Create: `looky-core/src/main/java/com/looky/result/application/GeneratedResult.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultGeneratorClient.java`
- Create: `looky-core/src/main/java/com/looky/result/application/ResultGenerationService.java`
- Modify: `looky-core/src/main/java/com/looky/result/application/ResultRepository.java`
- Modify: `looky-core/src/main/java/com/looky/survey/application/SurveyRepository.java`
- Modify: `looky-core/src/test/java/com/looky/result/application/ResultQueryServiceTest.java`
- Modify: `looky-core/src/test/java/com/looky/survey/application/SurveyCommandServiceTest.java`

- [ ] **Step 1: Write the failing core service test**

Create `looky-core/src/test/java/com/looky/result/application/ResultGenerationServiceTest.java`:

```java
package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;
import com.looky.submission.application.SubmissionRepository;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultGenerationServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final FakeSurveyRepository surveyRepository = new FakeSurveyRepository();
    private final FakeSubmissionRepository submissionRepository = new FakeSubmissionRepository();
    private final FakeResultRepository resultRepository = new FakeResultRepository();
    private final FakeResultGeneratorClient resultGeneratorClient = new FakeResultGeneratorClient();
    private final ResultGenerationService service = new ResultGenerationService(
            surveyRepository,
            submissionRepository,
            resultRepository,
            resultGeneratorClient,
            clock
    );

    @Test
    void generateReadyResultsCreatesResultAndMarksReady() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);

        int generatedCount = service.generateReadyResults();

        assertEquals(1, generatedCount);
        assertEquals(List.of(ResultStatus.GENERATING, ResultStatus.READY), surveyRepository.statusUpdates.get(survey.id()));
        ResultRecord result = resultRepository.resultsBySurveyId.get(survey.id());
        assertEquals(4, result.quadrants().size());
        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/open.png", imageUrl(result, ResultQuadrantType.OPEN));
    }

    @Test
    void generateReadyResultsSkipsWhenSelfSubmissionIsMissing() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);

        int generatedCount = service.generateReadyResults();

        assertEquals(0, generatedCount);
        assertFalse(resultRepository.resultsBySurveyId.containsKey(survey.id()));
        assertTrue(surveyRepository.statusUpdates.isEmpty());
    }

    @Test
    void generateReadyResultsSkipsWhenPeerSubmissionCountIsLow() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 2L);

        int generatedCount = service.generateReadyResults();

        assertEquals(0, generatedCount);
        assertFalse(resultRepository.resultsBySurveyId.containsKey(survey.id()));
        assertTrue(surveyRepository.statusUpdates.isEmpty());
    }

    @Test
    void generateReadyResultsSkipsWhenResultOpenTimeIsInFuture() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).plusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);

        int generatedCount = service.generateReadyResults();

        assertEquals(0, generatedCount);
        assertFalse(resultRepository.resultsBySurveyId.containsKey(survey.id()));
        assertTrue(surveyRepository.statusUpdates.isEmpty());
    }

    @Test
    void generateReadyResultsSkipsWhenResultAlreadyExists() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);
        resultRepository.resultsBySurveyId.put(survey.id(), new ResultRecord(10L, survey.id(), List.of()));

        int generatedCount = service.generateReadyResults();

        assertEquals(0, generatedCount);
        assertTrue(surveyRepository.statusUpdates.isEmpty());
    }

    @Test
    void generateReadyResultsMarksFailedAndContinuesWhenGeneratorFails() {
        SurveyRecord failedSurvey = survey(1L, "failcode0001", ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        SurveyRecord successSurvey = survey(2L, "b91k2p8xq4z2", ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(failedSurvey);
        surveyRepository.save(successSurvey);
        submissionRepository.completedSelfSurveyIds.add(failedSurvey.id());
        submissionRepository.completedSelfSurveyIds.add(successSurvey.id());
        submissionRepository.completedPeerCounts.put(failedSurvey.id(), 3L);
        submissionRepository.completedPeerCounts.put(successSurvey.id(), 3L);
        resultGeneratorClient.failSurveyCode = failedSurvey.surveyCode();

        int generatedCount = service.generateReadyResults();

        assertEquals(1, generatedCount);
        assertEquals(List.of(ResultStatus.GENERATING, ResultStatus.FAILED), surveyRepository.statusUpdates.get(failedSurvey.id()));
        assertEquals(List.of(ResultStatus.GENERATING, ResultStatus.READY), surveyRepository.statusUpdates.get(successSurvey.id()));
        assertFalse(resultRepository.resultsBySurveyId.containsKey(failedSurvey.id()));
        assertTrue(resultRepository.resultsBySurveyId.containsKey(successSurvey.id()));
    }

    private static String imageUrl(ResultRecord result, ResultQuadrantType type) {
        return result.quadrants().stream()
                .filter(quadrant -> quadrant.quadrantType() == type)
                .findFirst()
                .orElseThrow()
                .imageUrl();
    }

    private SurveyRecord survey(ResultStatus resultStatus, OffsetDateTime resultAvailableAt) {
        return survey(1L, "b91k2p8xq4z2", resultStatus, resultAvailableAt);
    }

    private SurveyRecord survey(Long id, String surveyCode, ResultStatus resultStatus, OffsetDateTime resultAvailableAt) {
        return new SurveyRecord(
                id,
                "만두",
                surveyCode,
                SurveyStatus.COLLECTING,
                resultStatus,
                3,
                resultAvailableAt,
                OffsetDateTime.now(clock).minusDays(1)
        );
    }

    private static final class FakeSurveyRepository implements SurveyRepository {
        private final Map<Long, SurveyRecord> surveys = new LinkedHashMap<>();
        private final Map<Long, List<ResultStatus>> statusUpdates = new LinkedHashMap<>();

        void save(SurveyRecord survey) {
            surveys.put(survey.id(), survey);
        }

        @Override
        public SurveyRecord saveNewSurvey(String userNickname, String surveyCode, int requiredPeerSubmissionCount, OffsetDateTime now, OffsetDateTime resultAvailableAt) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public Optional<SurveyRecord> findBySurveyCode(String surveyCode) {
            return surveys.values().stream()
                    .filter(survey -> survey.surveyCode().equals(surveyCode))
                    .findFirst();
        }

        @Override
        public List<SurveyRecord> findResultGenerationCandidates(OffsetDateTime now) {
            return surveys.values().stream()
                    .filter(survey -> List.of(
                            ResultStatus.WAITING_SELF_RESPONSE,
                            ResultStatus.COLLECTING_PEER_RESPONSES,
                            ResultStatus.WAITING_RESULT_OPEN_TIME
                    ).contains(survey.resultStatus()))
                    .toList();
        }

        @Override
        public void markCollecting(Long surveyId) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public void updateResultStatus(Long surveyId, ResultStatus resultStatus) {
            SurveyRecord survey = surveys.get(surveyId);
            surveys.put(surveyId, new SurveyRecord(
                    survey.id(),
                    survey.userNickname(),
                    survey.surveyCode(),
                    survey.surveyStatus(),
                    resultStatus,
                    survey.requiredPeerSubmissionCount(),
                    survey.resultAvailableAt(),
                    survey.createdAt()
            ));
            statusUpdates.computeIfAbsent(surveyId, ignored -> new ArrayList<>()).add(resultStatus);
        }
    }

    private static final class FakeSubmissionRepository implements SubmissionRepository {
        private final List<Long> completedSelfSurveyIds = new ArrayList<>();
        private final Map<Long, Long> completedPeerCounts = new LinkedHashMap<>();

        @Override
        public boolean existsSelfSubmission(Long surveyId) {
            return completedSelfSurveyIds.contains(surveyId);
        }

        @Override
        public boolean existsCompletedSelfSubmission(Long surveyId) {
            return completedSelfSurveyIds.contains(surveyId);
        }

        @Override
        public long countCompletedPeerSubmissions(Long surveyId) {
            return completedPeerCounts.getOrDefault(surveyId, 0L);
        }

        @Override
        public com.looky.survey.application.dto.SubmissionStartedResult saveStartedSubmission(Long surveyId, String targetNickname, SubmitterType submitterType, String submitterKey, List<com.looky.question.application.QuestionRecord> questions, OffsetDateTime now) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public Optional<com.looky.submission.application.SubmissionRecord> findInProgressSubmission(Long submissionId) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public com.looky.survey.application.dto.SubmissionCompletedResult completeSubmission(Long submissionId, List<com.looky.survey.application.dto.AnswerCommand> answers, OffsetDateTime now) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }
    }

    private static final class FakeResultRepository implements ResultRepository {
        private final Map<Long, ResultRecord> resultsBySurveyId = new LinkedHashMap<>();
        private long sequence = 1;

        @Override
        public Optional<ResultRecord> findBySurveyId(Long surveyId) {
            return Optional.ofNullable(resultsBySurveyId.get(surveyId));
        }

        @Override
        public boolean existsBySurveyId(Long surveyId) {
            return resultsBySurveyId.containsKey(surveyId);
        }

        @Override
        public void saveResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now) {
            resultsBySurveyId.put(surveyId, new ResultRecord(sequence++, surveyId, quadrants));
        }
    }

    private static final class FakeResultGeneratorClient implements ResultGeneratorClient {
        private String failSurveyCode;

        @Override
        public GeneratedResult generate(SurveyRecord survey) {
            if (survey.surveyCode().equals(failSurveyCode)) {
                throw new IllegalStateException("fake generation failed");
            }

            Map<ResultQuadrantType, String> imageUrls = new EnumMap<>(ResultQuadrantType.class);
            imageUrls.put(ResultQuadrantType.OPEN, "https://cdn.looky.my/results/" + survey.surveyCode() + "/open.png");
            imageUrls.put(ResultQuadrantType.BLIND, "https://cdn.looky.my/results/" + survey.surveyCode() + "/blind.png");
            imageUrls.put(ResultQuadrantType.HIDDEN, "https://cdn.looky.my/results/" + survey.surveyCode() + "/hidden.png");
            imageUrls.put(ResultQuadrantType.UNKNOWN, "https://cdn.looky.my/results/" + survey.surveyCode() + "/unknown.png");
            return new GeneratedResult(imageUrls);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"
```

Expected: FAIL at compile time with missing symbols such as `ResultGenerationService`, `GeneratedResult`, and `ResultGeneratorClient`.

- [ ] **Step 3: Add generation contracts**

Create `looky-core/src/main/java/com/looky/result/application/GeneratedResult.java`:

```java
package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;

import java.util.List;
import java.util.Map;

public record GeneratedResult(
        Map<ResultQuadrantType, String> quadrantImageUrls
) {
    public List<ResultQuadrantRecord> toQuadrants() {
        return List.of(
                toQuadrant(ResultQuadrantType.OPEN),
                toQuadrant(ResultQuadrantType.BLIND),
                toQuadrant(ResultQuadrantType.HIDDEN),
                toQuadrant(ResultQuadrantType.UNKNOWN)
        );
    }

    private ResultQuadrantRecord toQuadrant(ResultQuadrantType type) {
        String imageUrl = quadrantImageUrls.get(type);
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Result image URL is required: " + type);
        }
        return new ResultQuadrantRecord(type, imageUrl);
    }
}
```

Create `looky-core/src/main/java/com/looky/result/application/ResultGeneratorClient.java`:

```java
package com.looky.result.application;

import com.looky.survey.application.SurveyRecord;

public interface ResultGeneratorClient {
    GeneratedResult generate(SurveyRecord survey);
}
```

Modify `looky-core/src/main/java/com/looky/result/application/ResultRepository.java`:

```java
package com.looky.result.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ResultRepository {
    Optional<ResultRecord> findBySurveyId(Long surveyId);

    boolean existsBySurveyId(Long surveyId);

    void saveResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now);
}
```

Modify `looky-core/src/main/java/com/looky/survey/application/SurveyRepository.java`:

```java
package com.looky.survey.application;

import com.looky.survey.domain.ResultStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SurveyRepository {
    SurveyRecord saveNewSurvey(
            String userNickname,
            String surveyCode,
            int requiredPeerSubmissionCount,
            OffsetDateTime now,
            OffsetDateTime resultAvailableAt
    );

    Optional<SurveyRecord> findBySurveyCode(String surveyCode);

    List<SurveyRecord> findResultGenerationCandidates(OffsetDateTime now);

    void markCollecting(Long surveyId);

    void updateResultStatus(Long surveyId, ResultStatus resultStatus);
}
```

- [ ] **Step 4: Implement core generation service**

Create `looky-core/src/main/java/com/looky/result/application/ResultGenerationService.java`:

```java
package com.looky.result.application;

import com.looky.submission.application.SubmissionRepository;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.domain.ResultStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ResultGenerationService {

    private final SurveyRepository surveyRepository;
    private final SubmissionRepository submissionRepository;
    private final ResultRepository resultRepository;
    private final ResultGeneratorClient resultGeneratorClient;
    private final Clock clock;

    public ResultGenerationService(
            SurveyRepository surveyRepository,
            SubmissionRepository submissionRepository,
            ResultRepository resultRepository,
            ResultGeneratorClient resultGeneratorClient,
            Clock clock
    ) {
        this.surveyRepository = surveyRepository;
        this.submissionRepository = submissionRepository;
        this.resultRepository = resultRepository;
        this.resultGeneratorClient = resultGeneratorClient;
        this.clock = clock;
    }

    public int generateReadyResults() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<SurveyRecord> candidates = surveyRepository.findResultGenerationCandidates(now);
        int generatedCount = 0;

        for (SurveyRecord survey : candidates) {
            if (!isReadyToGenerate(survey, now)) {
                continue;
            }
            if (resultRepository.existsBySurveyId(survey.id())) {
                continue;
            }

            surveyRepository.updateResultStatus(survey.id(), ResultStatus.GENERATING);
            try {
                GeneratedResult generatedResult = resultGeneratorClient.generate(survey);
                resultRepository.saveResult(survey.id(), generatedResult.toQuadrants(), now);
                surveyRepository.updateResultStatus(survey.id(), ResultStatus.READY);
                generatedCount++;
            } catch (RuntimeException exception) {
                surveyRepository.updateResultStatus(survey.id(), ResultStatus.FAILED);
            }
        }

        return generatedCount;
    }

    private boolean isReadyToGenerate(SurveyRecord survey, OffsetDateTime now) {
        return submissionRepository.existsCompletedSelfSubmission(survey.id())
                && submissionRepository.countCompletedPeerSubmissions(survey.id()) >= survey.requiredPeerSubmissionCount()
                && !survey.resultAvailableAt().isAfter(now);
    }
}
```

- [ ] **Step 5: Update existing test fakes for new repository methods**

Modify `looky-core/src/test/java/com/looky/result/application/ResultQueryServiceTest.java`:

```java
// Add imports:
import com.looky.survey.domain.ResultStatus;
import java.time.OffsetDateTime;
import java.util.List;

// In FakeSurveyRepository:
@Override
public List<SurveyRecord> findResultGenerationCandidates(OffsetDateTime now) {
    throw new UnsupportedOperationException("not used in result query tests");
}

@Override
public void updateResultStatus(Long surveyId, ResultStatus resultStatus) {
    throw new UnsupportedOperationException("not used in result query tests");
}

// In FakeResultRepository:
@Override
public boolean existsBySurveyId(Long surveyId) {
    return results.containsKey(surveyId);
}

@Override
public void saveResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now) {
    throw new UnsupportedOperationException("not used in result query tests");
}
```

Modify `looky-core/src/test/java/com/looky/survey/application/SurveyCommandServiceTest.java` inside `FakeSurveyRepository`:

```java
@Override
public List<SurveyRecord> findResultGenerationCandidates(OffsetDateTime now) {
    throw new UnsupportedOperationException("not used in survey command tests");
}

@Override
public void updateResultStatus(Long surveyId, ResultStatus resultStatus) {
    SurveyRecord survey = surveys.get(surveyId);
    surveys.put(surveyId, new SurveyRecord(
            survey.id(),
            survey.userNickname(),
            survey.surveyCode(),
            survey.surveyStatus(),
            resultStatus,
            survey.requiredPeerSubmissionCount(),
            survey.resultAvailableAt(),
            survey.createdAt()
    ));
}
```

- [ ] **Step 6: Run tests to verify core passes**

Run:

```bash
./gradlew :looky-core:test --tests "com.looky.result.application.ResultGenerationServiceTest"
./gradlew :looky-core:test
```

Expected: both commands PASS.

- [ ] **Step 7: Commit core generation service**

```bash
git add looky-core/src/main/java/com/looky/result/application \
  looky-core/src/main/java/com/looky/survey/application/SurveyRepository.java \
  looky-core/src/test/java/com/looky/result/application \
  looky-core/src/test/java/com/looky/survey/application/SurveyCommandServiceTest.java
git commit -m "feat: 결과 생성 유스케이스 추가"
```

---

### Task 2: Infrastructure Persistence for Generation

**Files:**
- Modify: `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaEntity.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaRepository.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyRepositoryImpl.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultJpaRepository.java`
- Modify: `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultRepositoryImpl.java`

- [ ] **Step 1: Run infrastructure compile to expose missing implementation methods**

Run:

```bash
./gradlew :looky-infrastructure:compileJava
```

Expected: FAIL because `SurveyRepositoryImpl` and `ResultRepositoryImpl` do not implement the new interface methods.

- [ ] **Step 2: Add result status mutation to survey entity**

Modify `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaEntity.java`:

```java
public void updateResultStatus(ResultStatus resultStatus) {
    this.resultStatus = resultStatus;
}
```

Place it near `markCollecting()`.

- [ ] **Step 3: Add survey candidate query**

Modify `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyJpaRepository.java`:

```java
package com.looky.survey.persistence;

import com.looky.survey.domain.ResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SurveyJpaRepository extends JpaRepository<SurveyJpaEntity, Long> {
    Optional<SurveyJpaEntity> findBySurveyCode(String surveyCode);

    List<SurveyJpaEntity> findByResultStatusInAndResultAvailableAtLessThanEqual(
            Collection<ResultStatus> resultStatuses,
            OffsetDateTime now
    );
}
```

- [ ] **Step 4: Implement survey repository methods**

Modify `looky-infrastructure/src/main/java/com/looky/survey/persistence/SurveyRepositoryImpl.java`:

```java
// Add imports:
import com.looky.survey.domain.ResultStatus;
import java.util.List;

@Override
@Transactional(readOnly = true)
public List<SurveyRecord> findResultGenerationCandidates(OffsetDateTime now) {
    return surveyJpaRepository.findByResultStatusInAndResultAvailableAtLessThanEqual(
                    List.of(
                            ResultStatus.WAITING_SELF_RESPONSE,
                            ResultStatus.COLLECTING_PEER_RESPONSES,
                            ResultStatus.WAITING_RESULT_OPEN_TIME
                    ),
                    now
            )
            .stream()
            .map(this::toRecord)
            .toList();
}

@Override
public void updateResultStatus(Long surveyId, ResultStatus resultStatus) {
    SurveyJpaEntity entity = surveyJpaRepository.findById(surveyId).orElseThrow();
    entity.updateResultStatus(resultStatus);
}
```

- [ ] **Step 5: Add result existence query**

Modify `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultJpaRepository.java`:

```java
package com.looky.result.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultJpaRepository extends JpaRepository<ResultJpaEntity, Long> {
    Optional<ResultJpaEntity> findBySurvey_Id(Long surveyId);

    boolean existsBySurvey_Id(Long surveyId);
}
```

- [ ] **Step 6: Implement result save methods**

Modify `looky-infrastructure/src/main/java/com/looky/result/persistence/ResultRepositoryImpl.java`:

```java
// Add imports:
import com.looky.survey.persistence.SurveyJpaEntity;
import com.looky.survey.persistence.SurveyJpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

// Add field:
private final SurveyJpaRepository surveyJpaRepository;

// Replace constructor:
public ResultRepositoryImpl(
        ResultJpaRepository resultJpaRepository,
        ResultQuadrantJpaRepository resultQuadrantJpaRepository,
        SurveyJpaRepository surveyJpaRepository
) {
    this.resultJpaRepository = resultJpaRepository;
    this.resultQuadrantJpaRepository = resultQuadrantJpaRepository;
    this.surveyJpaRepository = surveyJpaRepository;
}

@Override
public boolean existsBySurveyId(Long surveyId) {
    return resultJpaRepository.existsBySurvey_Id(surveyId);
}

@Override
@Transactional
public void saveResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now) {
    SurveyJpaEntity survey = surveyJpaRepository.findById(surveyId).orElseThrow();
    ResultJpaEntity result = resultJpaRepository.save(new ResultJpaEntity(survey, now));
    resultQuadrantJpaRepository.saveAll(quadrants.stream()
            .map(quadrant -> new ResultQuadrantJpaEntity(
                    result,
                    quadrant.quadrantType(),
                    quadrant.imageUrl()
            ))
            .toList());
}
```

Keep the existing `findBySurveyId()` and `toRecord()` methods.

- [ ] **Step 7: Run infrastructure compile**

Run:

```bash
./gradlew :looky-infrastructure:compileJava
```

Expected: PASS.

- [ ] **Step 8: Commit persistence implementation**

```bash
git add looky-infrastructure/src/main/java/com/looky/survey/persistence \
  looky-infrastructure/src/main/java/com/looky/result/persistence
git commit -m "feat: 결과 생성 저장소 구현"
```

---

### Task 3: Fake Result Generator

**Files:**
- Create: `looky-infrastructure/src/test/java/com/looky/result/client/FakeResultGeneratorClientTest.java`
- Create: `looky-infrastructure/src/main/java/com/looky/result/client/FakeResultGeneratorClient.java`

- [ ] **Step 1: Write failing fake generator test**

Create `looky-infrastructure/src/test/java/com/looky/result/client/FakeResultGeneratorClientTest.java`:

```java
package com.looky.result.client;

import com.looky.result.domain.ResultQuadrantType;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FakeResultGeneratorClientTest {

    private final FakeResultGeneratorClient client = new FakeResultGeneratorClient();

    @Test
    void generateReturnsFixedCdnUrlsForFourQuadrants() {
        var result = client.generate(new SurveyRecord(
                1L,
                "만두",
                "b91k2p8xq4z2",
                SurveyStatus.COLLECTING,
                ResultStatus.GENERATING,
                3,
                OffsetDateTime.parse("2026-06-23T03:00:00+09:00"),
                OffsetDateTime.parse("2026-06-22T03:00:00+09:00")
        ));

        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/open.png", result.quadrantImageUrls().get(ResultQuadrantType.OPEN));
        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/blind.png", result.quadrantImageUrls().get(ResultQuadrantType.BLIND));
        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/hidden.png", result.quadrantImageUrls().get(ResultQuadrantType.HIDDEN));
        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/unknown.png", result.quadrantImageUrls().get(ResultQuadrantType.UNKNOWN));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :looky-infrastructure:test --tests "com.looky.result.client.FakeResultGeneratorClientTest"
```

Expected: FAIL at compile time because `FakeResultGeneratorClient` does not exist.

- [ ] **Step 3: Implement fake generator**

Create `looky-infrastructure/src/main/java/com/looky/result/client/FakeResultGeneratorClient.java`:

```java
package com.looky.result.client;

import com.looky.result.application.GeneratedResult;
import com.looky.result.application.ResultGeneratorClient;
import com.looky.result.domain.ResultQuadrantType;
import com.looky.survey.application.SurveyRecord;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class FakeResultGeneratorClient implements ResultGeneratorClient {

    private static final String CDN_BASE_URL = "https://cdn.looky.my/results";

    @Override
    public GeneratedResult generate(SurveyRecord survey) {
        Map<ResultQuadrantType, String> imageUrls = new EnumMap<>(ResultQuadrantType.class);
        imageUrls.put(ResultQuadrantType.OPEN, imageUrl(survey.surveyCode(), "open"));
        imageUrls.put(ResultQuadrantType.BLIND, imageUrl(survey.surveyCode(), "blind"));
        imageUrls.put(ResultQuadrantType.HIDDEN, imageUrl(survey.surveyCode(), "hidden"));
        imageUrls.put(ResultQuadrantType.UNKNOWN, imageUrl(survey.surveyCode(), "unknown"));
        return new GeneratedResult(imageUrls);
    }

    private String imageUrl(String surveyCode, String quadrantName) {
        return CDN_BASE_URL + "/" + surveyCode + "/" + quadrantName + ".png";
    }
}
```

- [ ] **Step 4: Run fake generator test**

Run:

```bash
./gradlew :looky-infrastructure:test --tests "com.looky.result.client.FakeResultGeneratorClientTest"
```

Expected: PASS.

- [ ] **Step 5: Commit fake generator**

```bash
git add looky-infrastructure/src/main/java/com/looky/result/client \
  looky-infrastructure/src/test/java/com/looky/result/client
git commit -m "feat: fake 결과 생성기 추가"
```

---

### Task 4: Scheduler Wiring

**Files:**
- Create: `looky-infrastructure/src/main/java/com/looky/result/scheduler/ResultGenerationScheduler.java`
- Modify: `looky-api/src/main/java/com/looky/api/LookyApiApplication.java`
- Modify: `looky-api/src/main/resources/application.yaml`

- [ ] **Step 1: Add scheduler bean**

Create `looky-infrastructure/src/main/java/com/looky/result/scheduler/ResultGenerationScheduler.java`:

```java
package com.looky.result.scheduler;

import com.looky.result.application.ResultGenerationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResultGenerationScheduler {

    private final ResultGenerationService resultGenerationService;

    public ResultGenerationScheduler(ResultGenerationService resultGenerationService) {
        this.resultGenerationService = resultGenerationService;
    }

    @Scheduled(fixedDelayString = "${looky.result-generation.fixed-delay:60000}")
    public void generateReadyResults() {
        resultGenerationService.generateReadyResults();
    }
}
```

- [ ] **Step 2: Enable scheduling in the API application**

Modify `looky-api/src/main/java/com/looky/api/LookyApiApplication.java`:

```java
package com.looky.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.looky")
public class LookyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LookyApiApplication.class, args);
    }
}
```

- [ ] **Step 3: Add explicit scheduler interval configuration**

Modify `looky-api/src/main/resources/application.yaml`:

```yaml
looky:
  result-generation:
    fixed-delay: 60000

spring:
  datasource:
    url: jdbc:h2:mem:looky;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    defer-datasource-initialization: true
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        format_sql: true
  sql:
    init:
      mode: always
      data-locations: classpath:db/data.sql
```

- [ ] **Step 4: Run API bootJar**

Run:

```bash
./gradlew :looky-api:bootJar
```

Expected: PASS.

- [ ] **Step 5: Commit scheduler wiring**

```bash
git add looky-infrastructure/src/main/java/com/looky/result/scheduler \
  looky-api/src/main/java/com/looky/api/LookyApiApplication.java \
  looky-api/src/main/resources/application.yaml
git commit -m "feat: 결과 생성 스케줄러 연결"
```

---

### Task 5: Final Verification and Scope Check

**Files:**
- Modify: `docs/superpowers/specs/2026-06-23-result-generation-scheduler-design.md`

- [ ] **Step 1: Verify no stale package path remains in the spec**

Run:

```bash
rg "ResultGenerationScheduler" docs/superpowers/specs/2026-06-23-result-generation-scheduler-design.md
rg "persistence\\.ResultGenerationScheduler" docs/superpowers/specs/2026-06-23-result-generation-scheduler-design.md
```

Expected: first command contains `com.looky.result.scheduler.ResultGenerationScheduler`; second command has no matches and exits with status 1.

- [ ] **Step 2: Run full tests**

Run:

```bash
./gradlew test
```

Expected: PASS.

- [ ] **Step 3: Run bootJar**

Run:

```bash
./gradlew :looky-api:bootJar
```

Expected: PASS.

- [ ] **Step 4: Inspect changed files**

Run:

```bash
git status --short
git diff --stat HEAD
```

Expected: changes are limited to result generation scheduler code, repository contracts/implementations, scheduling config, and this plan/spec documentation.

- [ ] **Step 5: Commit spec package-path correction and plan**

```bash
git add docs/superpowers/specs/2026-06-23-result-generation-scheduler-design.md \
  docs/superpowers/plans/2026-06-23-result-generation-scheduler.md
git commit -m "docs: 결과 생성 스케줄러 구현 플랜 추가"
```

---

## Self-Review

- Spec coverage: The plan covers fake generation, 1-minute scheduling, eligibility checks, `GENERATING → READY/FAILED`, result persistence, scheduler enablement, and final verification.
- Scope check: The plan excludes AI, S3, CDN deployment, retry queues, internal admin APIs, and distributed locks as specified.
- Type consistency: `ResultGenerationService`, `ResultGeneratorClient`, `GeneratedResult`, `ResultRepository`, and `SurveyRepository` method signatures are consistent across tests, core implementation, and infrastructure implementation.
- Test coverage: Core business rules are covered with unit tests; fake URL generation has a unit test; JPA method names and scheduler wiring are covered through module compile, full tests, and bootJar.
