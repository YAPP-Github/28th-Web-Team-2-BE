package com.looky.result.application;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.result.domain.ResultQuadrantType;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.application.ResultStatusResolver;
import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;
import com.looky.question.application.QuestionRecord;
import com.looky.submission.application.SubmissionRecord;
import com.looky.submission.application.SubmissionRepository;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.SubmissionCompletedResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultQueryServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-23T03:00:00+09:00");

    private final FakeSurveyRepository surveyRepository = new FakeSurveyRepository();
    private final FakeResultRepository resultRepository = new FakeResultRepository();
    private final FakeSubmissionRepository submissionRepository = new FakeSubmissionRepository();
    private final ResultQueryService service = new ResultQueryService(
            surveyRepository,
            resultRepository,
            new ResultStatusResolver(submissionRepository, Clock.fixed(Instant.parse("2026-06-22T18:00:00Z"), ZoneId.of("Asia/Seoul")))
    );

    @Test
    void getSurveyResultReturnsFourQuadrantImageUrlsWhenReady() {
        surveyRepository.save(survey(ResultStatus.READY));
        resultRepository.save(new ResultRecord(
                10L,
                1L,
                List.of(
                        new ResultQuadrantRecord(ResultQuadrantType.OPEN, "https://cdn.looky.my/results/b91/open.png"),
                        new ResultQuadrantRecord(ResultQuadrantType.BLIND, "https://cdn.looky.my/results/b91/blind.png"),
                        new ResultQuadrantRecord(ResultQuadrantType.HIDDEN, "https://cdn.looky.my/results/b91/hidden.png"),
                        new ResultQuadrantRecord(ResultQuadrantType.UNKNOWN, "https://cdn.looky.my/results/b91/unknown.png")
                )
        ));

        SurveyResultResult result = service.getSurveyResult("b91k2p8xq4z2");

        assertEquals("b91k2p8xq4z2", result.surveyCode());
        assertEquals(ResultStatus.READY, result.resultStatus());
        assertEquals("https://cdn.looky.my/results/b91/open.png", result.quadrantImageUrls().get("OPEN"));
        assertEquals("https://cdn.looky.my/results/b91/blind.png", result.quadrantImageUrls().get("BLIND"));
        assertEquals("https://cdn.looky.my/results/b91/hidden.png", result.quadrantImageUrls().get("HIDDEN"));
        assertEquals("https://cdn.looky.my/results/b91/unknown.png", result.quadrantImageUrls().get("UNKNOWN"));
        assertEquals(4, result.quadrantImageUrls().size());
    }

    @Test
    void getSurveyResultSignsStoredS3ObjectKeysWhenReady() {
        surveyRepository.save(survey(ResultStatus.READY));
        resultRepository.save(new ResultRecord(10L, 1L, List.of(
                new ResultQuadrantRecord(ResultQuadrantType.OPEN, null, "open", "surveys/code/results/OPEN.png"),
                new ResultQuadrantRecord(ResultQuadrantType.BLIND, null, "blind", "surveys/code/results/BLIND.png"),
                new ResultQuadrantRecord(ResultQuadrantType.HIDDEN, null, "hidden", "surveys/code/results/HIDDEN.png"),
                new ResultQuadrantRecord(ResultQuadrantType.UNKNOWN, null, "unknown", "surveys/code/results/UNKNOWN.png")
        )));
        ResultQueryService signingService = new ResultQueryService(
                surveyRepository, resultRepository,
                new ResultStatusResolver(submissionRepository, Clock.fixed(Instant.parse("2026-06-22T18:00:00Z"), ZoneId.of("Asia/Seoul"))),
                key -> "https://signed.example/" + key
        );

        SurveyResultResult result = signingService.getSurveyResult("b91k2p8xq4z2");

        assertEquals("https://signed.example/surveys/code/results/OPEN.png", result.quadrantImageUrls().get("OPEN"));
        assertEquals("open", result.quadrantInterpretations().get("OPEN"));
    }

    @Test
    void getSurveyResultFailsWhenSurveyCodeIsInvalid() {
        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.getSurveyResult("missing-code")
        );

        assertEquals(ErrorCode.INVALID_SURVEY_CODE, exception.errorCode());
    }

    @Test
    void getSurveyResultReturnsWaitingStatusWhenSurveyIsNotReady() {
        surveyRepository.save(survey(ResultStatus.WAITING_SELF_RESPONSE));
        submissionRepository.selfSubmitted = true;
        submissionRepository.peerSubmissionCount = 2;

        SurveyResultResult result = service.getSurveyResult("b91k2p8xq4z2");

        assertEquals("b91k2p8xq4z2", result.surveyCode());
        assertEquals(ResultStatus.COLLECTING_PEER_RESPONSES, result.resultStatus());
        assertEquals(null, result.quadrantImageUrls());
    }

    @Test
    void getSurveyResultReturnsFailedStatusWhenGenerationFailed() {
        surveyRepository.save(survey(ResultStatus.FAILED));

        SurveyResultResult result = service.getSurveyResult("b91k2p8xq4z2");

        assertEquals("b91k2p8xq4z2", result.surveyCode());
        assertEquals(ResultStatus.FAILED, result.resultStatus());
        assertEquals(null, result.quadrantImageUrls());
    }

    @Test
    void getSurveyResultReturnsGeneratingStatusWhenGenerationIsInProgress() {
        surveyRepository.save(survey(ResultStatus.GENERATING));

        SurveyResultResult result = service.getSurveyResult("b91k2p8xq4z2");

        assertEquals("b91k2p8xq4z2", result.surveyCode());
        assertEquals(ResultStatus.GENERATING, result.resultStatus());
        assertEquals(null, result.quadrantImageUrls());
    }

    @Test
    void getSurveyResultFailsWhenReadyButResultRowDoesNotExist() {
        surveyRepository.save(survey(ResultStatus.READY));

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.getSurveyResult("b91k2p8xq4z2")
        );

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.errorCode());
    }

    @Test
    void getSurveyResultFailsWhenQuadrantsAreIncomplete() {
        surveyRepository.save(survey(ResultStatus.READY));
        resultRepository.save(new ResultRecord(
                10L,
                1L,
                List.of(new ResultQuadrantRecord(ResultQuadrantType.OPEN, "https://cdn.looky.my/results/b91/open.png"))
        ));

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.getSurveyResult("b91k2p8xq4z2")
        );

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.errorCode());
    }

    private SurveyRecord survey(ResultStatus resultStatus) {
        return new SurveyRecord(
                1L,
                "만두",
                "b91k2p8xq4z2",
                SurveyStatus.COLLECTING,
                resultStatus,
                0,
                3,
                NOW.plusHours(24),
                NOW
        );
    }

    private static final class FakeSurveyRepository implements SurveyRepository {
        private SurveyRecord survey;

        void save(SurveyRecord survey) {
            this.survey = survey;
        }

        @Override
        public SurveyRecord saveNewSurvey(String userNickname, String surveyCode, int requiredPeerSubmissionCount, OffsetDateTime now, OffsetDateTime resultAvailableAt) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public Optional<SurveyRecord> findBySurveyCode(String surveyCode) {
            if (survey != null && survey.surveyCode().equals(surveyCode)) {
                return Optional.of(survey);
            }
            return Optional.empty();
        }

        @Override
        public List<SurveyRecord> findResultGenerationCandidates(OffsetDateTime now) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public void markCollecting(Long surveyId) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public boolean markGenerating(Long surveyId, int maxAttempts) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public void updateResultStatus(Long surveyId, ResultStatus resultStatus) {
            throw new UnsupportedOperationException("not used in result query tests");
        }
    }

    private static final class FakeResultRepository implements ResultRepository {
        private final Map<Long, ResultRecord> results = new LinkedHashMap<>();

        void save(ResultRecord result) {
            results.put(result.surveyId(), result);
        }

        @Override
        public Optional<ResultRecord> findBySurveyId(Long surveyId) {
            return Optional.ofNullable(results.get(surveyId));
        }

        @Override
        public boolean existsBySurveyId(Long surveyId) {
            return results.containsKey(surveyId);
        }

        @Override
        public boolean hasNarrative(Long surveyId) {
            return false;
        }

        @Override
        public void saveNarrative(Long surveyId, List<ResultAnswerAdjectiveRecord> answers, ResultNarrative narrative, OffsetDateTime now) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public void markQuadrantImageReady(Long surveyId, ResultQuadrantType quadrantType, String s3ObjectKey) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public void markQuadrantImageFailed(Long surveyId, ResultQuadrantType quadrantType, String failureReason) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public void saveReadyResult(Long surveyId, List<ResultQuadrantRecord> quadrants, OffsetDateTime now) {
            throw new UnsupportedOperationException("not used in result query tests");
        }
    }

    private static final class FakeSubmissionRepository implements SubmissionRepository {
        private boolean selfSubmitted;
        private long peerSubmissionCount;

        @Override
        public boolean existsSelfSubmission(Long surveyId) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public boolean existsCompletedSelfSubmission(Long surveyId) {
            return selfSubmitted;
        }

        @Override
        public long countCompletedPeerSubmissions(Long surveyId) {
            return peerSubmissionCount;
        }

        @Override
        public SubmissionStartedResult saveStartedSubmission(
                Long surveyId,
                String targetNickname,
                SubmitterType submitterType,
                String submitterKey,
                List<QuestionRecord> questions,
                OffsetDateTime now
        ) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public Optional<SubmissionRecord> findInProgressSubmission(Long submissionId) {
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public SubmissionCompletedResult completeSubmission(Long submissionId, List<AnswerCommand> answers, OffsetDateTime now) {
            throw new UnsupportedOperationException("not used in result query tests");
        }
    }
}
