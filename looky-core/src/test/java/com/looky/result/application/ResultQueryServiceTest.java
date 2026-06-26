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
                overview(),
                readyQuadrants()
        ));

        SurveyResultResult result = service.getSurveyResult("b91k2p8xq4z2");

        assertEquals("b91k2p8xq4z2", result.surveyCode());
        assertEquals(ResultStatus.READY, result.resultStatus());
        assertEquals("https://cdn.looky.my/results/b91/open.png", result.quadrantImageUrls().get("OPEN"));
        assertEquals("https://cdn.looky.my/results/b91/blind.png", result.quadrantImageUrls().get("BLIND"));
        assertEquals("https://cdn.looky.my/results/b91/hidden.png", result.quadrantImageUrls().get("HIDDEN"));
        assertEquals("https://cdn.looky.my/results/b91/unknown.png", result.quadrantImageUrls().get("UNKNOWN"));
        assertEquals(4, result.quadrantImageUrls().size());
        assertEquals("마음을 잘 여는 사람", result.overall().keyword());
        assertEquals("대화를 여는 다정한 기운", result.overall().analysisTitle());
        assertEquals("앞장서다 혼자 짐을 다 안을 때가 있어요.\n한 번만 속도 맞춰볼까요? 하고 먼저 물어보세요.\n주변도 더 편하게 움직이고 관계가 오래 따뜻하게 돌아올 거예요.", result.overall().tip());
        assertEquals("탐험가", result.quadrants().get("OPEN").definitionKeyword());
        assertEquals(List.of("탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지"), result.quadrants().get("OPEN").adjectiveKeywords());
    }

    @Test
    void getSurveyResultSignsStoredS3ObjectKeysWhenReady() {
        surveyRepository.save(survey(ResultStatus.READY));
        resultRepository.save(new ResultRecord(10L, 1L, overview(), List.of(
                new ResultQuadrantRecord(ResultQuadrantType.OPEN, null, "open", null, "surveys/code/results/OPEN.png", null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "탐험가", List.of("탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지")),
                new ResultQuadrantRecord(ResultQuadrantType.BLIND, null, "blind", null, "surveys/code/results/BLIND.png", null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "관찰자", List.of("사람 잘 챙기기 1순위", "분위기 메이커")),
                new ResultQuadrantRecord(ResultQuadrantType.HIDDEN, null, "hidden", null, "surveys/code/results/HIDDEN.png", null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "사색가", List.of("혼자가 편해", "디테일 집착")),
                new ResultQuadrantRecord(ResultQuadrantType.UNKNOWN, null, "unknown", null, "surveys/code/results/UNKNOWN.png", null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "개척자", List.of("한 번 꽂히면 끝장", "새로운 곳 좋아"))
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
    void getSurveyResultFailsWhenOverviewIsMissing() {
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

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.getSurveyResult("b91k2p8xq4z2")
        );

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.errorCode());
    }

    @Test
    void getSurveyResultFailsWhenQuadrantKeywordsAreInvalid() {
        surveyRepository.save(survey(ResultStatus.READY));
        resultRepository.save(new ResultRecord(
                10L,
                1L,
                overview(),
                List.of(
                        new ResultQuadrantRecord(ResultQuadrantType.OPEN, "https://cdn.looky.my/results/b91/open.png", "open", null, null, null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "탐험가", List.of("태그 하나")),
                        new ResultQuadrantRecord(ResultQuadrantType.BLIND, "https://cdn.looky.my/results/b91/blind.png", "blind", null, null, null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "관찰자", List.of("사람 잘 챙기기 1순위", "분위기 메이커")),
                        new ResultQuadrantRecord(ResultQuadrantType.HIDDEN, "https://cdn.looky.my/results/b91/hidden.png", "hidden", null, null, null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "사색가", List.of("혼자가 편해", "디테일 집착")),
                        new ResultQuadrantRecord(ResultQuadrantType.UNKNOWN, "https://cdn.looky.my/results/b91/unknown.png", "unknown", null, null, null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "개척자", List.of("한 번 꽂히면 끝장", "새로운 곳 좋아"))
                )
        ));

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.getSurveyResult("b91k2p8xq4z2")
        );

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.errorCode());
    }

    private ResultOverviewRecord overview() {
        return new ResultOverviewRecord(
                "마음을 잘 여는 사람",
                "대화를 여는 다정한 기운",
                "낯선 자리에서도 \"먼저 같이 해볼까요?\" 하고 말을 건넵니다. 주변도 금세 편하게 반응하고 흐름이 부드러워집니다. 끝나고 나면 따뜻한 여운이 오래 남습니다.",
                "앞장서다 혼자 짐을 다 안을 때가 있어요.\n한 번만 속도 맞춰볼까요? 하고 먼저 물어보세요.\n주변도 더 편하게 움직이고 관계가 오래 따뜻하게 돌아올 거예요."
        );
    }

    private List<ResultQuadrantRecord> readyQuadrants() {
        return List.of(
                new ResultQuadrantRecord(ResultQuadrantType.OPEN, "https://cdn.looky.my/results/b91/open.png", "서로 알고 있는 강점", null, null, null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "탐험가", List.of("탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지")),
                new ResultQuadrantRecord(ResultQuadrantType.BLIND, "https://cdn.looky.my/results/b91/blind.png", "타인이 먼저 발견하는 특성", null, null, null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "관찰자", List.of("사람 잘 챙기기 1순위", "분위기 메이커")),
                new ResultQuadrantRecord(ResultQuadrantType.HIDDEN, "https://cdn.looky.my/results/b91/hidden.png", "혼자 알고 있는 내면", null, null, null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "사색가", List.of("혼자가 편해", "디테일 집착")),
                new ResultQuadrantRecord(ResultQuadrantType.UNKNOWN, "https://cdn.looky.my/results/b91/unknown.png", "아직 발견되지 않은 가능성", null, null, null, com.looky.result.domain.QuadrantWorkStatus.IMAGE_READY, 0, "개척자", List.of("한 번 꽂히면 끝장", "새로운 곳 좋아"))
        );
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
                NOW,
                "pomang",
                "v1"
        );
    }

    private static final class FakeSurveyRepository implements SurveyRepository {
        private SurveyRecord survey;

        void save(SurveyRecord survey) {
            this.survey = survey;
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
            throw new UnsupportedOperationException("not used in result query tests");
        }

        @Override
        public Optional<SurveyRecord> findById(Long surveyId) {
            if (survey != null && survey.id().equals(surveyId)) {
                return Optional.of(survey);
            }
            return Optional.empty();
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

        @Override
        public void syncResultStatus(Long surveyId, ResultStatus resultStatus) {
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
        public void markQuadrantImageReady(Long surveyId, ResultQuadrantType quadrantType, String s3ObjectKey, String selectedVariantKey) {
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
