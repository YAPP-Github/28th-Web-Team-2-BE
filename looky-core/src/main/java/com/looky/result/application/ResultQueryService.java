package com.looky.result.application;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.result.domain.ResultQuadrantType;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.application.ResultStatusResolver;
import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.application.dto.SurveyResultQuadrant;
import com.looky.survey.domain.ResultStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ResultQueryService {

    private final SurveyRepository surveyRepository;
    private final ResultRepository resultRepository;
    private final ResultStatusResolver resultStatusResolver;
    private final ResultUrlSigner resultUrlSigner;

    public ResultQueryService(SurveyRepository surveyRepository, ResultRepository resultRepository, ResultStatusResolver resultStatusResolver) {
        this(surveyRepository, resultRepository, resultStatusResolver, objectKey -> objectKey);
    }

    public SurveyResultResult getSurveyResult(String surveyCode) {
        SurveyRecord survey = surveyRepository.findBySurveyCode(surveyCode)
                .orElseThrow(() -> new LookyException(ErrorCode.INVALID_SURVEY_CODE));
        ResultStatus resultStatus = resultStatusResolver.resolve(survey);
        if (resultStatus != ResultStatus.READY) {
            return new SurveyResultResult(survey.surveyCode(), resultStatus, null);
        }

        ResultRecord result = resultRepository.findBySurveyId(survey.id())
                .orElseThrow(() -> new LookyException(ErrorCode.INTERNAL_SERVER_ERROR));
        ResultOverviewRecord overview = requireOverview(result);
        Map<ResultQuadrantType, String> imageUrls = toQuadrantImageUrls(result);
        Map<ResultQuadrantType, String> interpretations = toQuadrantInterpretations(result);

        return new SurveyResultResult(
                survey.surveyCode(),
                resultStatus,
                orderedQuadrantMap(imageUrls),
                orderedQuadrantMap(interpretations),
                overview,
                toQuadrants(result, imageUrls)
        );
    }

    private Map<String, SurveyResultQuadrant> toQuadrants(ResultRecord result, Map<ResultQuadrantType, String> imageUrls) {
        Map<String, SurveyResultQuadrant> quadrants = new LinkedHashMap<>();
        for (ResultQuadrantType type : ResultQuadrantType.values()) {
            ResultQuadrantRecord quadrant = requireQuadrant(result, type);
            if (isBlank(quadrant.definitionKeyword())
                    || quadrant.adjectiveKeywords() == null
                    || quadrant.adjectiveKeywords().size() != 2
                    || quadrant.adjectiveKeywords().stream().anyMatch(ResultQueryService::isBlank)
                    || isBlank(quadrant.interpretation())) {
                throw new LookyException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            quadrants.put(type.name(), new SurveyResultQuadrant(quadrant.definitionKeyword(), quadrant.adjectiveKeywords(), quadrant.interpretation(), imageUrls.get(type)));
        }
        return Collections.unmodifiableMap(quadrants);
    }

    private Map<ResultQuadrantType, String> toQuadrantImageUrls(ResultRecord result) {
        Map<ResultQuadrantType, String> imageUrls = new EnumMap<>(ResultQuadrantType.class);
        for (ResultQuadrantRecord quadrant : result.quadrants()) {
            imageUrls.put(quadrant.quadrantType(), quadrant.s3ObjectKey() == null ? quadrant.imageUrl() : resultUrlSigner.sign(quadrant.s3ObjectKey()));
        }
        for (ResultQuadrantType type : ResultQuadrantType.values()) {
            String imageUrl = imageUrls.get(type);
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new LookyException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
        return imageUrls;
    }

    private Map<ResultQuadrantType, String> toQuadrantInterpretations(ResultRecord result) {
        Map<ResultQuadrantType, String> interpretations = new EnumMap<>(ResultQuadrantType.class);
        for (ResultQuadrantType type : ResultQuadrantType.values()) {
            ResultQuadrantRecord quadrant = requireQuadrant(result, type);
            if (isBlank(quadrant.interpretation())) {
                throw new LookyException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            interpretations.put(type, quadrant.interpretation());
        }
        return interpretations;
    }

    private ResultOverviewRecord requireOverview(ResultRecord result) {
        ResultOverviewRecord overview = result.overview();
        if (overview == null
                || isBlank(overview.keyword())
                || isBlank(overview.analysisTitle())
                || isBlank(overview.analysisBody())
                || isBlank(overview.tip())) {
            throw new LookyException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return overview;
    }

    private ResultQuadrantRecord requireQuadrant(ResultRecord result, ResultQuadrantType type) {
        return result.quadrants().stream()
                .filter(quadrant -> quadrant.quadrantType() == type)
                .findFirst()
                .orElseThrow(() -> new LookyException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private static <T> Map<String, T> orderedQuadrantMap(Map<ResultQuadrantType, T> valuesByType) {
        Map<String, T> ordered = new LinkedHashMap<>();
        for (ResultQuadrantType type : ResultQuadrantType.values()) {
            ordered.put(type.name(), valuesByType.get(type));
        }
        return Collections.unmodifiableMap(ordered);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
