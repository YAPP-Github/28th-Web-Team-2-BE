package com.looky.result.application;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.result.domain.ResultQuadrantType;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.domain.ResultStatus;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class ResultQueryService {

    private final SurveyRepository surveyRepository;
    private final ResultRepository resultRepository;

    public ResultQueryService(SurveyRepository surveyRepository, ResultRepository resultRepository) {
        this.surveyRepository = surveyRepository;
        this.resultRepository = resultRepository;
    }

    public SurveyResultResult getSurveyResult(String surveyCode) {
        SurveyRecord survey = surveyRepository.findBySurveyCode(surveyCode)
                .orElseThrow(() -> new LookyException(ErrorCode.INVALID_SURVEY_CODE));
        if (survey.resultStatus() != ResultStatus.READY) {
            return new SurveyResultResult(
                    survey.surveyCode(),
                    survey.resultStatus(),
                    null
            );
        }

        ResultRecord result = resultRepository.findBySurveyId(survey.id())
                .orElseThrow(() -> new LookyException(ErrorCode.INTERNAL_SERVER_ERROR));
        Map<ResultQuadrantType, String> imageUrls = toQuadrantImageUrls(result);

        return new SurveyResultResult(
                survey.surveyCode(),
                survey.resultStatus(),
                Map.of(
                        ResultQuadrantType.OPEN.name(), imageUrls.get(ResultQuadrantType.OPEN),
                        ResultQuadrantType.BLIND.name(), imageUrls.get(ResultQuadrantType.BLIND),
                        ResultQuadrantType.HIDDEN.name(), imageUrls.get(ResultQuadrantType.HIDDEN),
                        ResultQuadrantType.UNKNOWN.name(), imageUrls.get(ResultQuadrantType.UNKNOWN)
                )
        );
    }

    private Map<ResultQuadrantType, String> toQuadrantImageUrls(ResultRecord result) {
        Map<ResultQuadrantType, String> imageUrls = new EnumMap<>(ResultQuadrantType.class);
        for (ResultQuadrantRecord quadrant : result.quadrants()) {
            imageUrls.put(quadrant.quadrantType(), quadrant.imageUrl());
        }
        for (ResultQuadrantType type : ResultQuadrantType.values()) {
            String imageUrl = imageUrls.get(type);
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new LookyException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
        return imageUrls;
    }
}
