package com.looky.api.survey;

import com.looky.api.LookyApiApplication;
import com.looky.result.application.ResultGenerationService;
import com.looky.result.application.ResultQueryService;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.application.SurveyService;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.CreateSurveyCommand;
import com.looky.survey.application.dto.QuestionResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import com.looky.survey.application.dto.SurveyCreatedResult;
import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.application.dto.SurveyStatusResult;
import com.looky.survey.application.dto.SubmitAnswersCommand;
import com.looky.survey.domain.ResultStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = LookyApiApplication.class,
        properties = {
                "looky.survey.result-open-delay=24h",
                "looky.result-generation.fixed-delay=3600000"
        }
)
@ActiveProfiles("test")
class SurveyResultOpenDelayIntegrationTest {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private ResultGenerationService resultGenerationService;

    @Autowired
    private ResultQueryService resultQueryService;

    @Test
    void generateReadyResultsUsesPeerThresholdBeforeResultOpenTime() {
        SurveyCreatedResult survey = surveyService.createSurvey(new CreateSurveyCommand("만두"));

        SubmissionStartedResult selfSubmission = surveyService.startSubmission(survey.surveyCode());
        surveyService.submitAnswers(selfSubmission.submissionId(), answersFrom(selfSubmission));
        for (int i = 0; i < 3; i++) {
            SubmissionStartedResult peerSubmission = surveyService.startSubmission(survey.surveyCode());
            assertEquals(SubmitterType.PEER, peerSubmission.submitterType());
            surveyService.submitAnswers(peerSubmission.submissionId(), answersFrom(peerSubmission));
        }

        SurveyStatusResult status = surveyService.getSurveyStatus(survey.surveyCode());
        int generatedCount = resultGenerationService.generateReadyResults();
        SurveyResultResult result = resultQueryService.getSurveyResult(survey.surveyCode());

        assertEquals(ResultStatus.GENERATING, status.resultStatus());
        assertEquals(3, status.peerSubmissionCount());
        assertTrue(status.remainingSecondsToResultOpen() > 0);
        assertEquals(1, generatedCount);
        assertEquals(ResultStatus.READY, result.resultStatus());
    }

    private SubmitAnswersCommand answersFrom(SubmissionStartedResult submission) {
        List<AnswerCommand> answers = submission.questions().stream()
                .map(this::firstAnswer)
                .toList();
        return new SubmitAnswersCommand(answers);
    }

    private AnswerCommand firstAnswer(QuestionResult question) {
        return new AnswerCommand(
                question.questionId(),
                question.options().getFirst().answerOptionId()
        );
    }
}
