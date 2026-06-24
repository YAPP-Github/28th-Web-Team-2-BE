package com.looky.result.application;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.result.domain.ResultQuadrantType;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.application.ResultStatusResolver;
import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.domain.ResultStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
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
            return new SurveyResultResult(
                    survey.surveyCode(),
                    resultStatus,
                    null,
                    null
            );
        }

        ResultRecord result = resultRepository.findBySurveyId(survey.id())
                .orElseThrow(() -> new LookyException(ErrorCode.INTERNAL_SERVER_ERROR));
        Map<ResultQuadrantType, String> imageUrls = toQuadrantImageUrls(result);
        Map<ResultQuadrantType, String> interpretations = toQuadrantInterpretations(result);

        return new SurveyResultResult(
                survey.surveyCode(),
                resultStatus,
                Map.of(
                        ResultQuadrantType.OPEN.name(), imageUrls.get(ResultQuadrantType.OPEN),
                        ResultQuadrantType.BLIND.name(), imageUrls.get(ResultQuadrantType.BLIND),
                        ResultQuadrantType.HIDDEN.name(), imageUrls.get(ResultQuadrantType.HIDDEN),
                        ResultQuadrantType.UNKNOWN.name(), imageUrls.get(ResultQuadrantType.UNKNOWN)
                ),
                interpretations == null ? null : Map.of(
                        ResultQuadrantType.OPEN.name(), interpretations.get(ResultQuadrantType.OPEN),
                        ResultQuadrantType.BLIND.name(), interpretations.get(ResultQuadrantType.BLIND),
                        ResultQuadrantType.HIDDEN.name(), interpretations.get(ResultQuadrantType.HIDDEN),
                        ResultQuadrantType.UNKNOWN.name(), interpretations.get(ResultQuadrantType.UNKNOWN)
                )
        );
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
        for (ResultQuadrantRecord quadrant : result.quadrants()) interpretations.put(quadrant.quadrantType(), quadrant.interpretation());
        for (ResultQuadrantType type : ResultQuadrantType.values()) if (interpretations.get(type) == null || interpretations.get(type).isBlank()) return null;
        return interpretations;
    }
}
