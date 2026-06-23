package com.looky.result.application;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.result.domain.ResultQuadrantType;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
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
    private final ResultQueryService service = new ResultQueryService(surveyRepository, resultRepository);

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
        assertEquals("https://cdn.looky.my/results/b91/open.png", result.quadrantImageUrls().get("OPEN"));
        assertEquals("https://cdn.looky.my/results/b91/blind.png", result.quadrantImageUrls().get("BLIND"));
        assertEquals("https://cdn.looky.my/results/b91/hidden.png", result.quadrantImageUrls().get("HIDDEN"));
        assertEquals("https://cdn.looky.my/results/b91/unknown.png", result.quadrantImageUrls().get("UNKNOWN"));
        assertEquals(4, result.quadrantImageUrls().size());
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
    void getSurveyResultFailsWhenSurveyIsNotReady() {
        surveyRepository.save(survey(ResultStatus.COLLECTING_PEER_RESPONSES));

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.getSurveyResult("b91k2p8xq4z2")
        );

        assertEquals(ErrorCode.RESULT_NOT_READY, exception.errorCode());
    }

    @Test
    void getSurveyResultFailsWhenGenerationFailed() {
        surveyRepository.save(survey(ResultStatus.FAILED));

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.getSurveyResult("b91k2p8xq4z2")
        );

        assertEquals(ErrorCode.RESULT_GENERATION_FAILED, exception.errorCode());
    }

    @Test
    void getSurveyResultFailsWhenReadyButResultRowDoesNotExist() {
        surveyRepository.save(survey(ResultStatus.READY));

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.getSurveyResult("b91k2p8xq4z2")
        );

        assertEquals(ErrorCode.RESULT_NOT_READY, exception.errorCode());
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

        assertEquals(ErrorCode.RESULT_NOT_READY, exception.errorCode());
    }

    private SurveyRecord survey(ResultStatus resultStatus) {
        return new SurveyRecord(
                1L,
                "만두",
                "b91k2p8xq4z2",
                SurveyStatus.COLLECTING,
                resultStatus,
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
        public void markCollecting(Long surveyId) {
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
    }
}
