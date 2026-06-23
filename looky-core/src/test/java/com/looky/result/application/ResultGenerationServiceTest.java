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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(OffsetDateTime.now(clock), resultRepository.savedAtBySurveyId.get(survey.id()));
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
            statusUpdates.computeIfAbsent(surveyId, ignored -> new ArrayList<>()).add(resultStatus);
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
            resultsBySurveyId.put(surveyId, new ResultRecord(10L + surveyId, surveyId, quadrants));
            savedAtBySurveyId.put(surveyId, now);
        }
    }

    private static final class FakeResultGeneratorClient implements ResultGeneratorClient {
        private String failSurveyCode;

        @Override
        public GeneratedResult generate(SurveyRecord survey) {
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
}
