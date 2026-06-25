package com.looky.api.survey;

import com.looky.api.LookyApiApplication;
import com.looky.result.application.ResultGenerationService;
import com.looky.result.application.ResultRepository;
import com.looky.result.domain.ResultQuadrantType;
import com.looky.result.client.TestResultImageClient;
import com.looky.survey.application.SurveyService;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.CreateSurveyCommand;
import com.looky.survey.application.dto.QuestionResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import com.looky.survey.application.dto.SubmitAnswersCommand;
import com.looky.survey.domain.ResultStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = LookyApiApplication.class, properties = {
        "looky.survey.result-open-delay-hours=0",
        "looky.result-generation.fixed-delay=3600000",
        "looky.test.fail-blind-image-once=true"
})
@ActiveProfiles("test")
class ResultImageRetryIntegrationTest {

    @Autowired private SurveyService surveyService;
    @Autowired private ResultGenerationService resultGenerationService;
    @Autowired private ResultRepository resultRepository;
    @Autowired private TestResultImageClient imageClient;

    @Test
    void retriesOnlyFailedBlindQuadrantAndEventuallyMarksResultReady() {
        var survey = surveyService.createSurvey(new CreateSurveyCommand("만두"));
        complete(survey.surveyCode());
        complete(survey.surveyCode());
        complete(survey.surveyCode());
        complete(survey.surveyCode());

        assertEquals(0, resultGenerationService.generateReadyResults());
        assertEquals(4, imageClient.generatedPrompts().size());
        assertEquals(1, resultGenerationService.generateReadyResults());
        assertEquals(5, imageClient.generatedPrompts().size());
        assertTrue(imageClient.generatedPrompts().getLast().contains("BLIND image prompt"));
        assertEquals(
                "blind-magnifier",
                resultRepository.findBySurveyId(survey.surveyId()).orElseThrow().quadrants().stream()
                        .filter(quadrant -> quadrant.quadrantType() == ResultQuadrantType.BLIND)
                        .findFirst()
                        .orElseThrow()
                        .selectedVariantKey()
        );
    }

    private void complete(String surveyCode) {
        SubmissionStartedResult submission = surveyService.startSubmission(surveyCode);
        surveyService.submitAnswers(submission.submissionId(), new SubmitAnswersCommand(submission.questions().stream()
                .map(question -> new AnswerCommand(question.questionId(), question.options().getFirst().answerOptionId()))
                .toList()));
    }
}
