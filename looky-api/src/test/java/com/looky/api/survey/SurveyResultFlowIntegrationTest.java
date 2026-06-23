package com.looky.api.survey;

import com.looky.api.LookyApiApplication;
import com.looky.result.application.ResultGenerationService;
import com.looky.result.application.ResultQueryService;
import com.looky.survey.application.SurveyService;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.CreateSurveyCommand;
import com.looky.survey.application.dto.QuestionResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import com.looky.survey.application.dto.SubmitAnswersCommand;
import com.looky.survey.application.dto.SurveyCreatedResult;
import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.domain.ResultStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = LookyApiApplication.class,
        properties = {
                "looky.survey.result-open-delay-hours=0",
                "looky.result-generation.fixed-delay=3600000"
        }
)
class SurveyResultFlowIntegrationTest {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private ResultGenerationService resultGenerationService;

    @Autowired
    private ResultQueryService resultQueryService;

    @Test
    void surveyFlowGeneratesAndReadsFakeResult() {
        SurveyCreatedResult survey = surveyService.createSurvey(new CreateSurveyCommand("만두"));

        SubmissionStartedResult selfSubmission = surveyService.startSubmission(survey.surveyCode());
        surveyService.submitAnswers(selfSubmission.submissionId(), answersFrom(selfSubmission));
        for (int i = 0; i < 3; i++) {
            SubmissionStartedResult peerSubmission = surveyService.startSubmission(survey.surveyCode());
            surveyService.submitAnswers(peerSubmission.submissionId(), answersFrom(peerSubmission));
        }

        int generatedCount = resultGenerationService.generateReadyResults();
        SurveyResultResult result = resultQueryService.getSurveyResult(survey.surveyCode());

        assertEquals(1, generatedCount);
        assertEquals(survey.surveyCode(), result.surveyCode());
        assertEquals(ResultStatus.READY, result.resultStatus());
        assertEquals("https://cdn.looky.my/results/" + survey.surveyCode() + "/open.png", result.quadrantImageUrls().get("OPEN"));
        assertEquals("https://cdn.looky.my/results/" + survey.surveyCode() + "/blind.png", result.quadrantImageUrls().get("BLIND"));
        assertEquals("https://cdn.looky.my/results/" + survey.surveyCode() + "/hidden.png", result.quadrantImageUrls().get("HIDDEN"));
        assertEquals("https://cdn.looky.my/results/" + survey.surveyCode() + "/unknown.png", result.quadrantImageUrls().get("UNKNOWN"));
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
