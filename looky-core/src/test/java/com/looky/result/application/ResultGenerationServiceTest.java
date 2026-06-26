package com.looky.result.application;

import com.looky.characterpack.application.CharacterPackRepository;
import com.looky.characterpack.application.CharacterPackVariantRecord;
import com.looky.result.domain.QuadrantWorkStatus;
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
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultGenerationServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final FakeSurveyRepository surveyRepository = new FakeSurveyRepository();
    private final FakeSubmissionRepository submissionRepository = new FakeSubmissionRepository();
    private final FakeResultRepository resultRepository = new FakeResultRepository(surveyRepository);
    private final FakeResultGeneratorClient resultGeneratorClient = new FakeResultGeneratorClient();
    private final FakeResultGenerationSourceReader sourceReader = new FakeResultGenerationSourceReader();
    private final FakeResultNarrativeClient narrativeClient = new FakeResultNarrativeClient();
    private final FakeCharacterPackRepository characterPackRepository = new FakeCharacterPackRepository();
    private final FakeResultImageClient resultImageClient = new FakeResultImageClient();
    private final FakeResultImageStorage resultImageStorage = new FakeResultImageStorage();
    private final ResultGenerationService service = new ResultGenerationService(
            surveyRepository,
            submissionRepository,
            resultRepository,
            resultGeneratorClient,
            sourceReader,
            narrativeClient,
            new ResultGenerationPolicy(3),
            clock
    );
    private final ResultGenerationService imageService = new ResultGenerationService(
            surveyRepository,
            submissionRepository,
            resultRepository,
            resultGeneratorClient,
            sourceReader,
            narrativeClient,
            new ResultGenerationPolicy(3),
            clock,
            resultImageClient,
            resultImageStorage,
            characterPackRepository
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
        assertEquals(OffsetDateTime.now(clock), resultRepository.savedAtBySurveyId.get(survey.id()));
        ResultRecord result = resultRepository.resultsBySurveyId.get(survey.id());
        assertEquals(4, result.quadrants().size());
        assertEquals("https://cdn.looky.my/results/b91k2p8xq4z2/open.png", imageUrl(result, ResultQuadrantType.OPEN));
    }

    @Test
    void generateReadyResultsPersistsNarrativeBeforeGeneratingImages() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);
        sourceReader.answers = List.of(new ResultAnswerAdjectiveRecord(101L, 11L, SubmitterType.SELF, "SELF", com.looky.question.domain.TraitCode.OPENNESS, "질문", "답변", List.of()));
        narrativeClient.narrative = new ResultNarrative(
                new ResultNarrative.Overview("마음을 잘 여는 사람", "대화를 여는 다정한 기운", "종합 분석 본문", "새로운 대화를 시작해보세요."),
                Map.of(101L, List.of("호기심 많은")),
                Map.of(
                        ResultQuadrantType.OPEN, new ResultNarrative.QuadrantNarrative("탐험가", List.of("호기심 많은", "새로운 거 좋아"), "공유 강점", "open"),
                        ResultQuadrantType.BLIND, new ResultNarrative.QuadrantNarrative("관찰자", List.of("따뜻한", "다정한"), "타인이 보는 강점", "blind"),
                        ResultQuadrantType.HIDDEN, new ResultNarrative.QuadrantNarrative("사색가", List.of("차분한", "깊은"), "내면", "hidden"),
                        ResultQuadrantType.UNKNOWN, new ResultNarrative.QuadrantNarrative("가능성", List.of("새로운", "미지의"), "가능성", "unknown")
                )
        );

        service.generateReadyResults();

        assertEquals(List.of("호기심 많은"), resultRepository.savedNarrative.adjectivesBySubmissionAnswerId().get(101L));
        assertEquals("타인이 보는 강점", resultRepository.savedNarrative.quadrants().get(ResultQuadrantType.BLIND).interpretation());
    }

    @Test
    void generateReadyResultsUsesSnapshotVariantAndStoresSelectedVariantKey() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1), "pomang", "v1");
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);
        resultRepository.resultsBySurveyId.put(survey.id(), narrativeReadyResult(survey.id()));

        int generatedCount = imageService.generateReadyResults();

        assertEquals(1, generatedCount);
        assertEquals(
                List.of(
                        "character-packs/pomang/qa-20260625/base/base.png",
                        "character-packs/pomang/qa-20260625/variants/blind-magnifier.png",
                        "character-packs/pomang/qa-20260625/variants/hidden-letter.png",
                        "character-packs/pomang/qa-20260625/variants/open-stars.png",
                        "character-packs/pomang/qa-20260625/variants/unknown-clock.png"
                ),
                resultImageClient.generatedRequests.getFirst().referenceAssetKeys()
        );
        assertEquals(
                "open-stars",
                selectedVariantKey(resultRepository.resultsBySurveyId.get(survey.id()), ResultQuadrantType.OPEN)
        );
    }

    @Test
    void generateReadyResultsBuildsReferenceAwareImagePrompt() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1), "pomang", "v1");
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);
        resultRepository.resultsBySurveyId.put(survey.id(), narrativeReadyResult(survey.id()));

        imageService.generateReadyResults();

        assertEquals(
                """
                        The hamster character in the attached reference images is our product character.
                        Use the references only to preserve the hamster's core identity, proportions, color palette, and illustration style.
                        Do not copy the exact pose, scene, composition, background, props, or facial expression from any reference image.
                        Create a fresh illustration with a distinct scene, camera angle, pose, expression, and mood for this Johari Window quadrant.
                        Additional scene guidance:
                        OPEN image prompt
                        """,
                resultImageClient.generatedRequests.getFirst().imagePrompt()
        );
    }

    @Test
    void generateReadyResultsSubmitsImageWorkToExecutor() {
        CountingExecutor executor = new CountingExecutor();
        ResultGenerationService executorBackedImageService = new ResultGenerationService(
                surveyRepository,
                submissionRepository,
                resultRepository,
                resultGeneratorClient,
                sourceReader,
                narrativeClient,
                new ResultGenerationPolicy(3),
                clock,
                resultImageClient,
                resultImageStorage,
                characterPackRepository,
                executor
        );
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1), "pomang", "v1");
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);
        resultRepository.resultsBySurveyId.put(survey.id(), narrativeReadyResult(survey.id()));

        int generatedCount = executorBackedImageService.generateReadyResults();

        assertEquals(1, generatedCount);
        assertEquals(4, executor.executedCount);
        assertEquals(4, resultImageClient.generatedRequests.size());
    }

    @Test
    void generateReadyResultsFailsWhenReferenceVariantsDoNotCoverEveryQuadrant() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1), "pomang", "v1");
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);
        resultRepository.resultsBySurveyId.put(survey.id(), narrativeReadyResult(survey.id()));
        characterPackRepository.referenceVariants = characterPackRepository.referenceVariants.stream()
                .filter(variant -> variant.quadrantType() != ResultQuadrantType.UNKNOWN)
                .toList();

        int generatedCount = imageService.generateReadyResults();

        assertEquals(0, generatedCount);
        assertEquals(List.of(ResultStatus.GENERATING), surveyRepository.statusUpdates.get(survey.id()));
        assertTrue(resultImageClient.generatedRequests.isEmpty());
        assertEquals(
                QuadrantWorkStatus.NARRATIVE_READY,
                resultRepository.resultsBySurveyId.get(survey.id()).quadrants().stream()
                        .filter(quadrant -> quadrant.quadrantType() == ResultQuadrantType.OPEN)
                        .findFirst()
                        .orElseThrow()
                        .workStatus()
        );
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
    void generateReadyResultsReusesExistingNarrativeOnRetry() {
        SurveyRecord survey = survey(ResultStatus.GENERATING, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);
        resultRepository.resultsBySurveyId.put(survey.id(), new ResultRecord(10L, survey.id(), List.of()));

        int generatedCount = service.generateReadyResults();

        assertEquals(1, generatedCount);
        assertEquals(0, narrativeClient.calls);
    }

    @Test
    void generateReadyResultsSkipsWhenGeneratingClaimFails() {
        SurveyRecord survey = survey(ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);
        surveyRepository.unclaimedSurveyIds.add(survey.id());

        int generatedCount = service.generateReadyResults();

        assertEquals(0, generatedCount);
        assertTrue(surveyRepository.statusUpdates.isEmpty());
        assertTrue(resultGeneratorClient.generatedSurveyCodes.isEmpty());
        assertFalse(resultRepository.resultsBySurveyId.containsKey(survey.id()));
    }

    @Test
    void generateReadyResultsKeepsGeneratingAndContinuesWhenGeneratorFailsBeforeAttemptsAreExhausted() {
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
        assertEquals(List.of(ResultStatus.GENERATING), surveyRepository.statusUpdates.get(failedSurvey.id()));
        assertEquals(List.of(ResultStatus.GENERATING, ResultStatus.READY), surveyRepository.statusUpdates.get(successSurvey.id()));
        assertFalse(resultRepository.resultsBySurveyId.containsKey(failedSurvey.id()));
        assertTrue(resultRepository.resultsBySurveyId.containsKey(successSurvey.id()));
    }

    @Test
    void generateReadyResultsMarksFailedWhenGeneratorFailsOnLastAttempt() {
        SurveyRecord failedSurvey = survey(
                1L,
                "failcode0001",
                ResultStatus.GENERATING,
                OffsetDateTime.now(clock).minusMinutes(1),
                2
        );
        surveyRepository.save(failedSurvey);
        submissionRepository.completedSelfSurveyIds.add(failedSurvey.id());
        submissionRepository.completedPeerCounts.put(failedSurvey.id(), 3L);
        resultGeneratorClient.failSurveyCode = failedSurvey.surveyCode();

        int generatedCount = service.generateReadyResults();

        assertEquals(0, generatedCount);
        assertEquals(List.of(ResultStatus.GENERATING, ResultStatus.FAILED), surveyRepository.statusUpdates.get(failedSurvey.id()));
        assertFalse(resultRepository.resultsBySurveyId.containsKey(failedSurvey.id()));
    }

    @Test
    void generateReadyResultsKeepsGeneratingAndContinuesWhenSaveReadyResultFailsBeforeAttemptsAreExhausted() {
        SurveyRecord failedSurvey = survey(1L, "failcode0001", ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        SurveyRecord successSurvey = survey(2L, "b91k2p8xq4z2", ResultStatus.WAITING_RESULT_OPEN_TIME, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(failedSurvey);
        surveyRepository.save(successSurvey);
        submissionRepository.completedSelfSurveyIds.add(failedSurvey.id());
        submissionRepository.completedSelfSurveyIds.add(successSurvey.id());
        submissionRepository.completedPeerCounts.put(failedSurvey.id(), 3L);
        submissionRepository.completedPeerCounts.put(successSurvey.id(), 3L);
        resultRepository.failSaveReadySurveyId = failedSurvey.id();

        int generatedCount = service.generateReadyResults();

        assertEquals(1, generatedCount);
        assertEquals(List.of(ResultStatus.GENERATING), surveyRepository.statusUpdates.get(failedSurvey.id()));
        assertEquals(List.of(ResultStatus.GENERATING, ResultStatus.READY), surveyRepository.statusUpdates.get(successSurvey.id()));
        assertFalse(resultRepository.resultsBySurveyId.containsKey(failedSurvey.id()));
        assertTrue(resultRepository.resultsBySurveyId.containsKey(successSurvey.id()));
    }

    @Test
    void generateReadyResultsRetriesGeneratingResultWhenAttemptsRemain() {
        SurveyRecord survey = survey(ResultStatus.GENERATING, OffsetDateTime.now(clock).minusMinutes(1));
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);

        int generatedCount = service.generateReadyResults();

        assertEquals(1, generatedCount);
        assertEquals(List.of(ResultStatus.GENERATING, ResultStatus.READY), surveyRepository.statusUpdates.get(survey.id()));
        assertTrue(resultRepository.resultsBySurveyId.containsKey(survey.id()));
    }

    @Test
    void generateReadyResultsDoesNotRetryFailedResult() {
        SurveyRecord survey = survey(
                1L,
                "b91k2p8xq4z2",
                ResultStatus.FAILED,
                OffsetDateTime.now(clock).minusMinutes(1),
                1
        );
        surveyRepository.save(survey);
        submissionRepository.completedSelfSurveyIds.add(survey.id());
        submissionRepository.completedPeerCounts.put(survey.id(), 3L);

        int generatedCount = service.generateReadyResults();

        assertEquals(0, generatedCount);
        assertTrue(surveyRepository.statusUpdates.isEmpty());
        assertFalse(resultRepository.resultsBySurveyId.containsKey(survey.id()));
    }

    @Test
    void generatedResultRejectsMissingQuadrantImageUrl() {
        Map<ResultQuadrantType, String> imageUrls = completeImageUrls();
        imageUrls.remove(ResultQuadrantType.UNKNOWN);

        GeneratedResult result = new GeneratedResult(imageUrls);

        assertThrows(IllegalArgumentException.class, result::toQuadrants);
    }

    @Test
    void generatedResultRejectsBlankQuadrantImageUrl() {
        Map<ResultQuadrantType, String> imageUrls = completeImageUrls();
        imageUrls.put(ResultQuadrantType.BLIND, " ");

        GeneratedResult result = new GeneratedResult(imageUrls);

        assertThrows(IllegalArgumentException.class, result::toQuadrants);
    }

    private static String imageUrl(ResultRecord result, ResultQuadrantType type) {
        return result.quadrants().stream()
                .filter(quadrant -> quadrant.quadrantType() == type)
                .findFirst()
                .orElseThrow()
                .imageUrl();
    }

    private static String selectedVariantKey(ResultRecord result, ResultQuadrantType type) {
        return result.quadrants().stream()
                .filter(quadrant -> quadrant.quadrantType() == type)
                .findFirst()
                .orElseThrow()
                .selectedVariantKey();
    }

    private SurveyRecord survey(ResultStatus resultStatus, OffsetDateTime resultAvailableAt) {
        return survey(1L, "b91k2p8xq4z2", resultStatus, resultAvailableAt, 0, "pomang", "v1");
    }

    private SurveyRecord survey(ResultStatus resultStatus, OffsetDateTime resultAvailableAt, String characterPackKey, String characterPackVersion) {
        return survey(1L, "b91k2p8xq4z2", resultStatus, resultAvailableAt, 0, characterPackKey, characterPackVersion);
    }

    private SurveyRecord survey(Long id, String surveyCode, ResultStatus resultStatus, OffsetDateTime resultAvailableAt) {
        return survey(id, surveyCode, resultStatus, resultAvailableAt, 0, "pomang", "v1");
    }

    private SurveyRecord survey(
            Long id,
            String surveyCode,
            ResultStatus resultStatus,
            OffsetDateTime resultAvailableAt,
            int resultGenerationAttemptCount
    ) {
        return survey(id, surveyCode, resultStatus, resultAvailableAt, resultGenerationAttemptCount, "pomang", "v1");
    }

    private SurveyRecord survey(
            Long id,
            String surveyCode,
            ResultStatus resultStatus,
            OffsetDateTime resultAvailableAt,
            int resultGenerationAttemptCount,
            String characterPackKey,
            String characterPackVersion
    ) {
        return new SurveyRecord(
                id,
                "만두",
                surveyCode,
                SurveyStatus.COLLECTING,
                resultStatus,
                resultGenerationAttemptCount,
                3,
                resultAvailableAt,
                OffsetDateTime.now(clock).minusDays(1),
                characterPackKey,
                characterPackVersion
        );
    }

    private static ResultRecord narrativeReadyResult(Long surveyId) {
        return new ResultRecord(
                10L + surveyId,
                surveyId,
                new ResultOverviewRecord("마음을 잘 여는 사람", "대화를 여는 다정한 기운", "종합 분석 본문", "새로운 대화를 시작해보세요."),
                ResultQuadrantType.values().length == 0 ? List.of() : List.of(
                        new ResultQuadrantRecord(ResultQuadrantType.OPEN, null, "OPEN 해석", "OPEN image prompt", null, null, QuadrantWorkStatus.NARRATIVE_READY, 0, "OPEN 탐험가", List.of("호기심 많은", "새로운 거 좋아")),
                        new ResultQuadrantRecord(ResultQuadrantType.BLIND, null, "BLIND 해석", "BLIND image prompt", null, null, QuadrantWorkStatus.NARRATIVE_READY, 0, "BLIND 관찰자", List.of("다정한", "세심한")),
                        new ResultQuadrantRecord(ResultQuadrantType.HIDDEN, null, "HIDDEN 해석", "HIDDEN image prompt", null, null, QuadrantWorkStatus.NARRATIVE_READY, 0, "HIDDEN 사색가", List.of("차분한", "깊이 있는")),
                        new ResultQuadrantRecord(ResultQuadrantType.UNKNOWN, null, "UNKNOWN 해석", "UNKNOWN image prompt", null, null, QuadrantWorkStatus.NARRATIVE_READY, 0, "UNKNOWN 개척자", List.of("가능성 큰", "새 판 짜봐"))
                )
        );
    }

    private static Map<ResultQuadrantType, String> completeImageUrls() {
        Map<ResultQuadrantType, String> imageUrls = new EnumMap<>(ResultQuadrantType.class);
        imageUrls.put(ResultQuadrantType.OPEN, "https://cdn.looky.my/results/b91/open.png");
        imageUrls.put(ResultQuadrantType.BLIND, "https://cdn.looky.my/results/b91/blind.png");
        imageUrls.put(ResultQuadrantType.HIDDEN, "https://cdn.looky.my/results/b91/hidden.png");
        imageUrls.put(ResultQuadrantType.UNKNOWN, "https://cdn.looky.my/results/b91/unknown.png");
        return imageUrls;
    }

    private static final class FakeSurveyRepository implements SurveyRepository {
        private final Map<Long, SurveyRecord> surveys = new LinkedHashMap<>();
        private final Map<Long, List<ResultStatus>> statusUpdates = new LinkedHashMap<>();
        private final List<Long> unclaimedSurveyIds = new ArrayList<>();

        void save(SurveyRecord survey) {
            surveys.put(survey.id(), survey);
        }

        @Override
        public SurveyRecord saveNewSurvey(
                String userNickname,
                String surveyCode,
                int requiredPeerSubmissionCount,
                OffsetDateTime now,
                OffsetDateTime resultAvailableAt,
                String characterPackKey,
                String characterPackVersion
        ) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public Optional<SurveyRecord> findById(Long surveyId) {
            return Optional.ofNullable(surveys.get(surveyId));
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
                            ResultStatus.WAITING_RESULT_OPEN_TIME,
                            ResultStatus.GENERATING
                    ).contains(survey.resultStatus()))
                    .toList();
        }

        @Override
        public void markCollecting(Long surveyId) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public boolean markGenerating(Long surveyId, int maxAttempts) {
            SurveyRecord survey = surveys.get(surveyId);
            if (unclaimedSurveyIds.contains(surveyId) || survey.resultGenerationAttemptCount() >= maxAttempts) {
                return false;
            }
            statusUpdates.computeIfAbsent(surveyId, ignored -> new ArrayList<>()).add(ResultStatus.GENERATING);
            surveys.put(surveyId, new SurveyRecord(
                    survey.id(),
                    survey.userNickname(),
                    survey.surveyCode(),
                    survey.surveyStatus(),
                    ResultStatus.GENERATING,
                    survey.resultGenerationAttemptCount() + 1,
                    survey.requiredPeerSubmissionCount(),
                    survey.resultAvailableAt(),
                    survey.createdAt(),
                    survey.characterPackKey(),
                    survey.characterPackVersion()
            ));
            return true;
        }

        @Override
        public void updateResultStatus(Long surveyId, ResultStatus resultStatus) {
            statusUpdates.computeIfAbsent(surveyId, ignored -> new ArrayList<>()).add(resultStatus);
            SurveyRecord survey = surveys.get(surveyId);
            surveys.put(surveyId, new SurveyRecord(
                    survey.id(),
                    survey.userNickname(),
                    survey.surveyCode(),
                    survey.surveyStatus(),
                    resultStatus,
                    survey.resultGenerationAttemptCount(),
                    survey.requiredPeerSubmissionCount(),
                    survey.resultAvailableAt(),
                    survey.createdAt(),
                    survey.characterPackKey(),
                    survey.characterPackVersion()
            ));
        }

        @Override
        public void syncResultStatus(Long surveyId, ResultStatus resultStatus) {
            SurveyRecord survey = surveys.get(surveyId);
            if (List.of(
                    ResultStatus.WAITING_SELF_RESPONSE,
                    ResultStatus.COLLECTING_PEER_RESPONSES,
                    ResultStatus.WAITING_RESULT_OPEN_TIME,
                    ResultStatus.GENERATING
            ).contains(survey.resultStatus())) {
                updateResultStatus(surveyId, resultStatus);
            }
        }
    }

    private static final class FakeSubmissionRepository implements SubmissionRepository {
        private final List<Long> completedSelfSurveyIds = new ArrayList<>();
        private final Map<Long, Long> completedPeerCounts = new LinkedHashMap<>();

        @Override
        public boolean existsSelfSubmission(Long surveyId) {
            throw new UnsupportedOperationException("not used in result generation tests");
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
        public com.looky.survey.application.dto.SubmissionStartedResult saveStartedSubmission(
                Long surveyId,
                String targetNickname,
                SubmitterType submitterType,
                String submitterKey,
                List<com.looky.question.application.QuestionRecord> questions,
                OffsetDateTime now
        ) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public Optional<com.looky.submission.application.SubmissionRecord> findInProgressSubmission(Long submissionId) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public com.looky.survey.application.dto.SubmissionCompletedResult completeSubmission(
                Long submissionId,
                List<com.looky.survey.application.dto.AnswerCommand> answers,
                OffsetDateTime now
        ) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }
    }

    private static final class FakeResultRepository implements ResultRepository {
        private final Map<Long, ResultRecord> resultsBySurveyId = new LinkedHashMap<>();
        private final Map<Long, OffsetDateTime> savedAtBySurveyId = new LinkedHashMap<>();
        private final FakeSurveyRepository surveyRepository;
        private Long failSaveReadySurveyId;
        private ResultNarrative savedNarrative;

        private FakeResultRepository(FakeSurveyRepository surveyRepository) {
            this.surveyRepository = surveyRepository;
        }

        @Override
        public Optional<ResultRecord> findBySurveyId(Long surveyId) {
            return Optional.ofNullable(resultsBySurveyId.get(surveyId));
        }

        @Override
        public boolean existsBySurveyId(Long surveyId) {
            return resultsBySurveyId.containsKey(surveyId);
        }

        @Override
        public boolean hasNarrative(Long surveyId) {
            return resultsBySurveyId.containsKey(surveyId);
        }

        @Override
        public void saveNarrative(Long surveyId, List<ResultAnswerAdjectiveRecord> answers, ResultNarrative narrative, OffsetDateTime now) {
            savedNarrative = narrative;
        }

        @Override
        public void markQuadrantImageReady(Long surveyId, ResultQuadrantType quadrantType, String s3ObjectKey, String selectedVariantKey) {
            ResultRecord result = resultsBySurveyId.get(surveyId);
            resultsBySurveyId.put(
                    surveyId,
                    new ResultRecord(
                            result.id(),
                            result.surveyId(),
                            result.overview(),
                            result.quadrants().stream()
                                    .map(quadrant -> quadrant.quadrantType() == quadrantType
                                            ? new ResultQuadrantRecord(
                                            quadrant.quadrantType(),
                                            null,
                                            quadrant.interpretation(),
                                            quadrant.imagePrompt(),
                                            s3ObjectKey,
                                            selectedVariantKey,
                                            QuadrantWorkStatus.IMAGE_READY,
                                            quadrant.attemptCount(),
                                            quadrant.definitionKeyword(),
                                            quadrant.adjectiveKeywords()
                                    )
                                            : quadrant)
                                    .toList()
                    )
            );
        }

        @Override
        public void markQuadrantImageFailed(Long surveyId, ResultQuadrantType quadrantType, String failureReason) {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public void saveReadyResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now) {
            if (surveyId.equals(failSaveReadySurveyId)) {
                throw new IllegalStateException("save ready result failed");
            }
            ResultRecord existing = resultsBySurveyId.get(surveyId);
            resultsBySurveyId.put(surveyId, new ResultRecord(
                    10L + surveyId,
                    surveyId,
                    existing == null ? null : existing.overview(),
                    quadrants
            ));
            savedAtBySurveyId.put(surveyId, now);
            surveyRepository.updateResultStatus(surveyId, ResultStatus.READY);
        }
    }

    private static final class FakeCharacterPackRepository implements CharacterPackRepository {
        private List<CharacterPackVariantRecord> referenceVariants = List.of(
                new CharacterPackVariantRecord(
                        "blind-magnifier",
                        ResultQuadrantType.BLIND,
                        "character-packs/pomang/qa-20260625/base/base.png",
                        "character-packs/pomang/qa-20260625/variants/blind-magnifier.png"
                ),
                new CharacterPackVariantRecord(
                        "hidden-letter",
                        ResultQuadrantType.HIDDEN,
                        "character-packs/pomang/qa-20260625/base/base.png",
                        "character-packs/pomang/qa-20260625/variants/hidden-letter.png"
                ),
                new CharacterPackVariantRecord(
                        "open-stars",
                        ResultQuadrantType.OPEN,
                        "character-packs/pomang/qa-20260625/base/base.png",
                        "character-packs/pomang/qa-20260625/variants/open-stars.png"
                ),
                new CharacterPackVariantRecord(
                        "unknown-clock",
                        ResultQuadrantType.UNKNOWN,
                        "character-packs/pomang/qa-20260625/base/base.png",
                        "character-packs/pomang/qa-20260625/variants/unknown-clock.png"
                )
        );

        @Override
        public Optional<com.looky.characterpack.application.CharacterPackSnapshot> findActiveSnapshot() {
            throw new UnsupportedOperationException("not used in result generation tests");
        }

        @Override
        public Optional<CharacterPackVariantRecord> findPrimaryVariant(String packKey, String packVersion, ResultQuadrantType quadrantType) {
            return referenceVariants.stream()
                    .filter(variant -> variant.quadrantType() == quadrantType)
                    .findFirst();
        }

        @Override
        public List<CharacterPackVariantRecord> findReferenceVariants(String packKey, String packVersion) {
            return referenceVariants;
        }
    }

    private static final class FakeResultGeneratorClient implements ResultGeneratorClient {
        private final List<String> generatedSurveyCodes = new ArrayList<>();
        private String failSurveyCode;

        @Override
        public GeneratedResult generate(ResultGenerationRequest request) {
            SurveyRecord survey = request.survey();
            generatedSurveyCodes.add(survey.surveyCode());
            if (survey.surveyCode().equals(failSurveyCode)) {
                throw new IllegalStateException("generation failed");
            }
            Map<ResultQuadrantType, String> imageUrls = new EnumMap<>(ResultQuadrantType.class);
            for (ResultQuadrantType type : ResultQuadrantType.values()) {
                imageUrls.put(
                        type,
                        "https://cdn.looky.my/results/%s/%s.png".formatted(survey.surveyCode(), type.name().toLowerCase())
                );
            }
            return new GeneratedResult(imageUrls);
        }
    }

    private static final class FakeResultGenerationSourceReader implements ResultGenerationSourceReader {
        private List<ResultAnswerAdjectiveRecord> answers = List.of();

        @Override
        public List<ResultAnswerAdjectiveRecord> readCompletedAnswers(Long surveyId) {
            return answers;
        }
    }

    private static final class FakeResultNarrativeClient implements ResultNarrativeClient {
        private ResultNarrative narrative = new ResultNarrative(new ResultNarrative.Overview("", "", "", ""), Map.of(), Map.of());
        private int calls;

        @Override
        public ResultNarrative generate(List<ResultAnswerAdjectiveRecord> answers) {
            calls++;
            return narrative;
        }
    }

    private static final class FakeResultImageClient implements ResultImageClient {
        private final List<ResultImageRequest> generatedRequests = new ArrayList<>();

        @Override
        public byte[] generate(ResultImageRequest request) {
            generatedRequests.add(request);
            return request.imagePrompt().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static final class FakeResultImageStorage implements ResultImageStorage {
        @Override
        public String upload(String surveyCode, ResultQuadrantType quadrantType, byte[] imageBytes) {
            return "surveys/%s/results/%s.png".formatted(surveyCode, quadrantType.name());
        }
    }

    private static final class CountingExecutor implements Executor {
        private int executedCount;

        @Override
        public void execute(Runnable command) {
            executedCount++;
            command.run();
        }
    }
}
