package com.looky.result.application;

import com.looky.submission.application.SubmissionRepository;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.domain.ResultStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.looky.result.domain.QuadrantWorkStatus;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ResultGenerationService {

    private static final Logger LOGGER = Logger.getLogger(ResultGenerationService.class.getName());

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
        this(surveyRepository, submissionRepository, resultRepository, resultGeneratorClient, sourceReader, narrativeClient, resultGenerationPolicy, clock, null, null);
    }

    @Autowired
    public ResultGenerationService(
            SurveyRepository surveyRepository, SubmissionRepository submissionRepository, ResultRepository resultRepository,
            ResultGeneratorClient resultGeneratorClient, ResultGenerationSourceReader sourceReader, ResultNarrativeClient narrativeClient,
            ResultGenerationPolicy resultGenerationPolicy, Clock clock, ResultImageClient resultImageClient, ResultImageStorage resultImageStorage
    ) {
        this.surveyRepository = surveyRepository;
        this.submissionRepository = submissionRepository;
        this.resultRepository = resultRepository;
        this.resultGeneratorClient = resultGeneratorClient;
        this.sourceReader = sourceReader;
        this.narrativeClient = narrativeClient;
        this.resultGenerationPolicy = resultGenerationPolicy;
        this.clock = clock;
        this.resultImageClient = resultImageClient;
        this.resultImageStorage = resultImageStorage;
    }

    public int generateReadyResults() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<SurveyRecord> candidates = surveyRepository.findResultGenerationCandidates(now);
        int generatedCount = 0;

        for (SurveyRecord survey : candidates) {
            if (!isReadyToGenerate(survey, now)) {
                continue;
            }
            if (!surveyRepository.markGenerating(survey.id(), resultGenerationPolicy.maxAttempts())) {
                continue;
            }
            int attemptNumber = survey.resultGenerationAttemptCount() + 1;

            try {
                if (!resultRepository.hasNarrative(survey.id())) {
                    List<ResultAnswerAdjectiveRecord> answers = sourceReader.readCompletedAnswers(survey.id());
                    ResultNarrative narrative = narrativeClient.generate(answers);
                    resultRepository.saveNarrative(survey.id(), answers, narrative, now);
                }
                if (resultImageClient == null) {
                    GeneratedResult generatedResult = resultGeneratorClient.generate(new ResultGenerationRequest(survey, submissionRepository.countCompletedPeerSubmissions(survey.id())));
                    resultRepository.saveReadyResult(survey.id(), generatedResult.toQuadrants(), now);
                    generatedCount++;
                } else {
                    for (ResultQuadrantRecord quadrant : resultRepository.findImageWorkCandidates(survey.id(), resultGenerationPolicy.maxAttempts())) {
                        try {
                            String key = resultImageStorage.upload(survey.surveyCode(), quadrant.quadrantType(), resultImageClient.generate(quadrant.imagePrompt()));
                            resultRepository.markQuadrantImageReady(survey.id(), quadrant.quadrantType(), key);
                        } catch (RuntimeException imageException) {
                            resultRepository.markQuadrantImageFailed(survey.id(), quadrant.quadrantType(), imageException.getMessage());
                        }
                    }
                    ResultRecord result = resultRepository.findBySurveyId(survey.id()).orElseThrow();
                    if (result.quadrants().stream().allMatch(quadrant -> quadrant.workStatus() == QuadrantWorkStatus.IMAGE_READY)) {
                        resultRepository.saveReadyResult(survey.id(), result.quadrants(), now);
                        generatedCount++;
                    } else if (result.quadrants().stream().anyMatch(quadrant -> quadrant.workStatus() == QuadrantWorkStatus.FAILED
                            && quadrant.attemptCount() >= resultGenerationPolicy.maxAttempts())) {
                        markFailed(survey.id());
                    }
                }
            } catch (RuntimeException exception) {
                LOGGER.log(
                        Level.WARNING,
                        "Result generation failed. surveyId=" + survey.id()
                                + ", attempt=" + attemptNumber,
                        exception
                );
                if (attemptNumber >= resultGenerationPolicy.maxAttempts()) {
                    markFailed(survey.id());
                }
            }
        }

        return generatedCount;
    }

    private boolean isReadyToGenerate(SurveyRecord survey, OffsetDateTime now) {
        return submissionRepository.existsCompletedSelfSubmission(survey.id())
                && submissionRepository.countCompletedPeerSubmissions(survey.id()) >= survey.requiredPeerSubmissionCount()
                && !survey.resultAvailableAt().isAfter(now);
    }

    private void markFailed(Long surveyId) {
        try {
            surveyRepository.updateResultStatus(surveyId, ResultStatus.FAILED);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Failed to mark result generation failure. surveyId=" + surveyId, exception);
        }
    }
}
