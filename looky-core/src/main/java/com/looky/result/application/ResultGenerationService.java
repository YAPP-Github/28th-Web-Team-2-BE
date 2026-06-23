package com.looky.result.application;

import com.looky.submission.application.SubmissionRepository;
import com.looky.survey.application.SurveyRecord;
import com.looky.survey.application.SurveyRepository;
import com.looky.survey.domain.ResultStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ResultGenerationService {

    private final SurveyRepository surveyRepository;
    private final SubmissionRepository submissionRepository;
    private final ResultRepository resultRepository;
    private final ResultGeneratorClient resultGeneratorClient;
    private final Clock clock;

    public ResultGenerationService(
            SurveyRepository surveyRepository,
            SubmissionRepository submissionRepository,
            ResultRepository resultRepository,
            ResultGeneratorClient resultGeneratorClient,
            Clock clock
    ) {
        this.surveyRepository = surveyRepository;
        this.submissionRepository = submissionRepository;
        this.resultRepository = resultRepository;
        this.resultGeneratorClient = resultGeneratorClient;
        this.clock = clock;
    }

    public int generateReadyResults() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<SurveyRecord> candidates = surveyRepository.findResultGenerationCandidates(now);
        int generatedCount = 0;

        for (SurveyRecord survey : candidates) {
            if (!isReadyToGenerate(survey, now)) {
                continue;
            }
            if (resultRepository.existsBySurveyId(survey.id())) {
                continue;
            }

            surveyRepository.updateResultStatus(survey.id(), ResultStatus.GENERATING);
            try {
                GeneratedResult generatedResult = resultGeneratorClient.generate(survey);
                resultRepository.saveResult(survey.id(), generatedResult.toQuadrants(), now);
            } catch (RuntimeException exception) {
                surveyRepository.updateResultStatus(survey.id(), ResultStatus.FAILED);
                continue;
            }
            surveyRepository.updateResultStatus(survey.id(), ResultStatus.READY);
            generatedCount++;
        }

        return generatedCount;
    }

    private boolean isReadyToGenerate(SurveyRecord survey, OffsetDateTime now) {
        return submissionRepository.existsCompletedSelfSubmission(survey.id())
                && submissionRepository.countCompletedPeerSubmissions(survey.id()) >= survey.requiredPeerSubmissionCount()
                && !survey.resultAvailableAt().isAfter(now);
    }
}
