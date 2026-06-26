package com.looky.result.application;

import com.looky.characterpack.application.CharacterPackRepository;
import com.looky.characterpack.application.CharacterPackVariantRecord;
import com.looky.submission.application.SubmissionRepository;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.domain.ResultStatus;
import com.looky.result.domain.QuadrantWorkStatus;
import com.looky.result.domain.ResultQuadrantType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ResultGenerationService {

    private final SurveyRepository surveyRepository;
    private final SubmissionRepository submissionRepository;
    private final ResultRepository resultRepository;
    private final ResultGeneratorClient resultGeneratorClient;
    private final ResultGenerationSourceReader sourceReader;
    private final ResultNarrativeClient narrativeClient;
    private final ResultGenerationPolicy resultGenerationPolicy;
    private final Clock clock;
    private final ResultImageClient resultImageClient;
    private final ResultImageStorage resultImageStorage;
    private final CharacterPackRepository characterPackRepository;

    public ResultGenerationService(
            SurveyRepository surveyRepository,
            SubmissionRepository submissionRepository,
            ResultRepository resultRepository,
            ResultGeneratorClient resultGeneratorClient,
            ResultGenerationSourceReader sourceReader,
            ResultNarrativeClient narrativeClient,
            ResultGenerationPolicy resultGenerationPolicy,
            Clock clock
    ) {
        this(surveyRepository, submissionRepository, resultRepository, resultGeneratorClient, sourceReader, narrativeClient, resultGenerationPolicy, clock, null, null, null);
    }

    public int generateReadyResults() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<SurveyRecord> candidates = surveyRepository.findResultGenerationCandidates(now);
        int generatedCount = 0;

        if (!candidates.isEmpty()) {
            log.info("result.generation.batch.started candidateCount={}", candidates.size());
        }
        for (SurveyRecord survey : candidates) {
            if (!isReadyToGenerate(survey, now)) {
                continue;
            }
            if (!surveyRepository.markGenerating(survey.id(), resultGenerationPolicy.maxAttempts())) {
                continue;
            }
            int attemptNumber = survey.resultGenerationAttemptCount() + 1;
            long peerSubmissionCount = submissionRepository.countCompletedPeerSubmissions(survey.id());
            log.info(
                    "result.generation.started surveyId={} surveyCode={} attempt={} peerSubmissionCount={} requiredPeerSubmissionCount={} resultAvailableAt={} narrativeExists={} imageClient={} imageModel={}",
                    survey.id(),
                    survey.surveyCode(),
                    attemptNumber,
                    peerSubmissionCount,
                    survey.requiredPeerSubmissionCount(),
                    survey.resultAvailableAt(),
                    resultRepository.hasNarrative(survey.id()),
                    resultImageClient == null ? "legacy-generator" : resultImageClient.getClass().getSimpleName(),
                    resultImageClient == null ? null : resultImageClient.modelName()
            );

            try {
                if (!resultRepository.hasNarrative(survey.id())) {
                    List<ResultAnswerAdjectiveRecord> answers = sourceReader.readCompletedAnswers(survey.id());
                    String narrativePrompt = ResultNarrativePromptComposer.compose(answers);
                    log.info(
                            "ai.narrative.request surveyId={} surveyCode={} source={} model={} reason=narrative_missing answerCount={} prompt=\n{}",
                            survey.id(),
                            survey.surveyCode(),
                            narrativeClient.getClass().getSimpleName(),
                            narrativeClient.modelName(),
                            answers.size(),
                            narrativePrompt
                    );
                    ResultNarrative narrative = narrativeClient.generate(answers);
                    log.info(
                            "ai.narrative.response surveyId={} surveyCode={} source={} model={} overallKeyword={} response={}",
                            survey.id(),
                            survey.surveyCode(),
                            narrativeClient.getClass().getSimpleName(),
                            narrativeClient.modelName(),
                            narrative.overview().keyword(),
                            narrative
                    );
                    resultRepository.saveNarrative(survey.id(), answers, narrative, now);
                    log.info(
                            "result.narrative.saved surveyId={} surveyCode={} answerCount={} overallKeyword={} quadrantCount={}",
                            survey.id(),
                            survey.surveyCode(),
                            answers.size(),
                            narrative.overview().keyword(),
                            narrative.quadrants().size()
                    );
                }
                if (resultImageClient == null) {
                    GeneratedResult generatedResult = resultGeneratorClient.generate(new ResultGenerationRequest(survey, submissionRepository.countCompletedPeerSubmissions(survey.id())));
                    resultRepository.saveReadyResult(survey.id(), generatedResult.toQuadrants(), now);
                    ResultRecord readyResult = resultRepository.findBySurveyId(survey.id()).orElseThrow();
                    log.info(
                            "result.ready surveyId={} surveyCode={} overviewKeyword={} quadrantStatuses={} selectedVariantKeys={}",
                            survey.id(),
                            survey.surveyCode(),
                            readyResult.overview() == null ? null : readyResult.overview().keyword(),
                            ResultLogSummary.quadrantStatuses(readyResult),
                            ResultLogSummary.selectedVariantKeys(readyResult)
                    );
                    generatedCount++;
                } else {
                    List<CharacterPackVariantRecord> referenceVariants = characterPackRepository.findReferenceVariants(
                            survey.characterPackKey(),
                            survey.characterPackVersion()
                    );
                    Map<ResultQuadrantType, CharacterPackVariantRecord> variantsByQuadrant = indexVariantsByQuadrant(
                            survey,
                            referenceVariants
                    );
                    List<String> referenceAssetKeys = buildReferenceAssetKeys(
                            survey,
                            referenceVariants,
                            variantsByQuadrant
                    );
                    for (ResultQuadrantRecord quadrant : resultRepository.findImageWorkCandidates(survey.id(), resultGenerationPolicy.maxAttempts())) {
                        try {
                            CharacterPackVariantRecord variant = variantsByQuadrant.get(quadrant.quadrantType());
                            if (variant == null) {
                                throw new IllegalStateException("Character pack variant not found for quadrant=" + quadrant.quadrantType());
                            }
                            ResultImageRequest request = new ResultImageRequest(
                                    buildReferenceAwareImagePrompt(quadrant),
                                    referenceAssetKeys
                            );
                            log.info(
                                    "ai.image.request surveyId={} surveyCode={} quadrant={} source={} model={} reason=quadrant_image_generation selectedVariantKey={} referenceAssetKeys={} prompt=\n{}",
                                    survey.id(),
                                    survey.surveyCode(),
                                    quadrant.quadrantType(),
                                    resultImageClient.getClass().getSimpleName(),
                                    resultImageClient.modelName(),
                                    variant.variantKey(),
                                    request.referenceAssetKeys(),
                                    request.imagePrompt()
                            );
                            byte[] imageBytes = resultImageClient.generate(request);
                            log.info(
                                    "ai.image.response surveyId={} surveyCode={} quadrant={} source={} model={} selectedVariantKey={} generatedBytes={}",
                                    survey.id(),
                                    survey.surveyCode(),
                                    quadrant.quadrantType(),
                                    resultImageClient.getClass().getSimpleName(),
                                    resultImageClient.modelName(),
                                    variant.variantKey(),
                                    imageBytes.length
                            );
                            String key = resultImageStorage.upload(
                                    survey.surveyCode(),
                                    quadrant.quadrantType(),
                                    imageBytes
                            );
                            resultRepository.markQuadrantImageReady(survey.id(), quadrant.quadrantType(), key, variant.variantKey());
                            log.info(
                                    "result.image.ready surveyId={} surveyCode={} quadrant={} selectedVariantKey={} s3ObjectKey={}",
                                    survey.id(),
                                    survey.surveyCode(),
                                    quadrant.quadrantType(),
                                    variant.variantKey(),
                                    key
                            );
                        } catch (RuntimeException imageException) {
                            log.warn(
                                    "result.image.failed surveyId={} surveyCode={} quadrant={} reason={}",
                                    survey.id(),
                                    survey.surveyCode(),
                                    quadrant.quadrantType(),
                                    imageException.getMessage(),
                                    imageException
                            );
                            resultRepository.markQuadrantImageFailed(survey.id(), quadrant.quadrantType(), imageException.getMessage());
                        }
                    }
                    ResultRecord result = resultRepository.findBySurveyId(survey.id()).orElseThrow();
                    if (result.quadrants().stream().allMatch(quadrant -> quadrant.workStatus() == QuadrantWorkStatus.IMAGE_READY)) {
                        resultRepository.saveReadyResult(survey.id(), result.quadrants(), now);
                        ResultRecord readyResult = resultRepository.findBySurveyId(survey.id()).orElseThrow();
                        log.info(
                                "result.ready surveyId={} surveyCode={} overviewKeyword={} quadrantStatuses={} selectedVariantKeys={}",
                                survey.id(),
                                survey.surveyCode(),
                                readyResult.overview() == null ? null : readyResult.overview().keyword(),
                                ResultLogSummary.quadrantStatuses(readyResult),
                                ResultLogSummary.selectedVariantKeys(readyResult)
                        );
                        generatedCount++;
                    } else if (result.quadrants().stream().anyMatch(quadrant -> quadrant.workStatus() == QuadrantWorkStatus.FAILED
                            && quadrant.attemptCount() >= resultGenerationPolicy.maxAttempts())) {
                        markFailed(survey, attemptNumber, "quadrant image retries exhausted");
                    }
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "result.generation.failed surveyId={} surveyCode={} attempt={} reason={}",
                        survey.id(),
                        survey.surveyCode(),
                        attemptNumber,
                        exception.getMessage(),
                        exception
                );
                if (attemptNumber >= resultGenerationPolicy.maxAttempts()) {
                    markFailed(survey, attemptNumber, exception.getMessage());
                }
            }
        }

        if (generatedCount > 0) {
            log.info("result.generation.batch.completed generatedCount={}", generatedCount);
        }
        return generatedCount;
    }

    private boolean isReadyToGenerate(SurveyRecord survey, OffsetDateTime now) {
        return submissionRepository.existsCompletedSelfSubmission(survey.id())
                && submissionRepository.countCompletedPeerSubmissions(survey.id()) >= survey.requiredPeerSubmissionCount()
                && !survey.resultAvailableAt().isAfter(now);
    }

    private void markFailed(SurveyRecord survey, int attemptNumber, String reason) {
        try {
            surveyRepository.updateResultStatus(survey.id(), ResultStatus.FAILED);
            log.error(
                    "result.failed surveyId={} surveyCode={} attempt={} reason={}",
                    survey.id(),
                    survey.surveyCode(),
                    attemptNumber,
                    reason
            );
        } catch (RuntimeException exception) {
            log.error("result.failed.marking-error surveyId={} surveyCode={}", survey.id(), survey.surveyCode(), exception);
        }
    }

    private String buildReferenceAwareImagePrompt(ResultQuadrantRecord quadrant) {
        return """
                Use every attached image only as a reference for the same character and illustration style.
                Do not assign separate roles, priority, or hierarchy to the reference images.
                Blend the shared character identity, props, mood, and visual cues across all references into one final illustration.
                Additional scene guidance:
                %s
                """.formatted(quadrant.imagePrompt());
    }

    private Map<ResultQuadrantType, CharacterPackVariantRecord> indexVariantsByQuadrant(
            SurveyRecord survey,
            List<CharacterPackVariantRecord> referenceVariants
    ) {
        if (referenceVariants.isEmpty()) {
            throw new IllegalStateException("Character pack reference variants not found. packKey=%s packVersion=%s"
                    .formatted(survey.characterPackKey(), survey.characterPackVersion()));
        }
        EnumMap<ResultQuadrantType, CharacterPackVariantRecord> variantsByQuadrant = new EnumMap<>(ResultQuadrantType.class);
        for (CharacterPackVariantRecord referenceVariant : referenceVariants) {
            CharacterPackVariantRecord duplicate = variantsByQuadrant.putIfAbsent(referenceVariant.quadrantType(), referenceVariant);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate character pack reference variant. packKey=%s packVersion=%s quadrant=%s"
                        .formatted(survey.characterPackKey(), survey.characterPackVersion(), referenceVariant.quadrantType()));
            }
        }
        EnumSet<ResultQuadrantType> missingQuadrants = EnumSet.allOf(ResultQuadrantType.class);
        missingQuadrants.removeAll(variantsByQuadrant.keySet());
        if (!missingQuadrants.isEmpty()) {
            throw new IllegalStateException("Character pack reference variants are incomplete. packKey=%s packVersion=%s missingQuadrants=%s"
                    .formatted(survey.characterPackKey(), survey.characterPackVersion(), missingQuadrants));
        }
        return variantsByQuadrant;
    }

    private List<String> buildReferenceAssetKeys(
            SurveyRecord survey,
            List<CharacterPackVariantRecord> referenceVariants,
            Map<ResultQuadrantType, CharacterPackVariantRecord> variantsByQuadrant
    ) {
        String baseAssetKey = referenceVariants.getFirst().baseAssetKey();
        for (CharacterPackVariantRecord referenceVariant : referenceVariants) {
            if (!baseAssetKey.equals(referenceVariant.baseAssetKey())) {
                throw new IllegalStateException("Character pack reference variants disagree on base asset. packKey=%s packVersion=%s"
                        .formatted(survey.characterPackKey(), survey.characterPackVersion()));
            }
        }
        ArrayList<String> orderedVariantAssetKeys = new ArrayList<>();
        orderedVariantAssetKeys.add(variantsByQuadrant.get(ResultQuadrantType.BLIND).assetKey());
        orderedVariantAssetKeys.add(variantsByQuadrant.get(ResultQuadrantType.HIDDEN).assetKey());
        orderedVariantAssetKeys.add(variantsByQuadrant.get(ResultQuadrantType.OPEN).assetKey());
        orderedVariantAssetKeys.add(variantsByQuadrant.get(ResultQuadrantType.UNKNOWN).assetKey());

        LinkedHashSet<String> referenceAssetKeys = new LinkedHashSet<>();
        referenceAssetKeys.add(baseAssetKey);
        referenceAssetKeys.addAll(orderedVariantAssetKeys);

        int expectedReferenceAssetCount = ResultQuadrantType.values().length + 1;
        if (referenceAssetKeys.size() != expectedReferenceAssetCount) {
            throw new IllegalStateException("Expected exactly %s reference assets. packKey=%s packVersion=%s actualKeys=%s"
                    .formatted(expectedReferenceAssetCount, survey.characterPackKey(), survey.characterPackVersion(), referenceAssetKeys));
        }
        return List.copyOf(referenceAssetKeys);
    }
}
