package com.looky.api.survey;

import com.looky.api.LookyApiApplication;
import com.looky.question.application.QuestionRecord;
import com.looky.question.application.QuestionRepository;
import com.looky.question.domain.TraitCode;
import com.looky.result.application.ResultGenerationService;
import com.looky.result.application.ResultGenerationSourceReader;
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
import com.looky.submission.domain.SubmitterType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = LookyApiApplication.class,
        properties = {
                "looky.survey.result-open-delay=0m",
                "looky.result-generation.fixed-delay=3600000"
        }
)
@ActiveProfiles("test")
class SurveyResultFlowIntegrationTest {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private ResultGenerationService resultGenerationService;

    @Autowired
    private ResultQueryService resultQueryService;

    @Autowired
    private ResultGenerationSourceReader resultGenerationSourceReader;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void questionSelectionAssignsTwoActiveQuestionsForEachTrait() {
        List<QuestionRecord> questions = questionRepository.findRandomActiveQuestionsByTrait(2, SubmitterType.SELF);

        assertEquals(8, questions.size());
        for (TraitCode traitCode : TraitCode.values()) {
            assertEquals(2, questions.stream().filter(question -> question.traitCode() == traitCode).count());
        }
    }

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
        assertEquals("https://signed.test/surveys/" + survey.surveyCode() + "/results/OPEN.png", result.quadrantImageUrls().get("OPEN"));
        assertEquals("https://signed.test/surveys/" + survey.surveyCode() + "/results/BLIND.png", result.quadrantImageUrls().get("BLIND"));
        assertEquals("https://signed.test/surveys/" + survey.surveyCode() + "/results/HIDDEN.png", result.quadrantImageUrls().get("HIDDEN"));
        assertEquals("https://signed.test/surveys/" + survey.surveyCode() + "/results/UNKNOWN.png", result.quadrantImageUrls().get("UNKNOWN"));
    }

    @Test
    void resultQueryReturnsCollectingPeerResponsesAfterSelfAndTwoPeerSubmissions() {
        SurveyCreatedResult survey = surveyService.createSurvey(new CreateSurveyCommand("만두"));

        SubmissionStartedResult selfSubmission = surveyService.startSubmission(survey.surveyCode());
        surveyService.submitAnswers(selfSubmission.submissionId(), answersFrom(selfSubmission));
        for (int i = 0; i < 2; i++) {
            SubmissionStartedResult peerSubmission = surveyService.startSubmission(survey.surveyCode());
            surveyService.submitAnswers(peerSubmission.submissionId(), answersFrom(peerSubmission));
        }

        SurveyResultResult result = resultQueryService.getSurveyResult(survey.surveyCode());

        assertEquals(ResultStatus.COLLECTING_PEER_RESPONSES, result.resultStatus());
        assertEquals(null, result.quadrantImageUrls());
    }

    @Test
    void resultGenerationSourceReaderKeepsSelfAndPeerAnswerSourcesSeparate() {
        SurveyCreatedResult survey = surveyService.createSurvey(new CreateSurveyCommand("만두"));

        SubmissionStartedResult selfSubmission = surveyService.startSubmission(survey.surveyCode());
        surveyService.submitAnswers(selfSubmission.submissionId(), answersFrom(selfSubmission));
        for (int i = 0; i < 3; i++) {
            SubmissionStartedResult peerSubmission = surveyService.startSubmission(survey.surveyCode());
            surveyService.submitAnswers(peerSubmission.submissionId(), answersFrom(peerSubmission));
        }

        var answers = resultGenerationSourceReader.readCompletedAnswers(survey.surveyId());

        assertEquals(32, answers.size());
        assertEquals(32, answers.stream().map(answer -> answer.submissionAnswerId()).distinct().count());
        assertEquals(8, answers.stream().filter(answer -> answer.submitterType() == SubmitterType.SELF).count());
        assertEquals(24, answers.stream().filter(answer -> answer.submitterType() == SubmitterType.PEER).count());
        assertEquals(8, answers.stream().filter(answer -> answer.respondentLabel().equals("SELF")).count());
        assertEquals(8, answers.stream().filter(answer -> answer.respondentLabel().equals("PEER_1")).count());
        assertEquals(8, answers.stream().filter(answer -> answer.respondentLabel().equals("PEER_2")).count());
        assertEquals(8, answers.stream().filter(answer -> answer.respondentLabel().equals("PEER_3")).count());
        assertEquals(8, answers.stream().filter(answer -> answer.traitCode() == TraitCode.OPENNESS).count());
        assertEquals(8, answers.stream().filter(answer -> answer.traitCode() == TraitCode.CONSCIENTIOUSNESS).count());
        assertEquals(8, answers.stream().filter(answer -> answer.traitCode() == TraitCode.EXTRAVERSION).count());
        assertEquals(8, answers.stream().filter(answer -> answer.traitCode() == TraitCode.AGREEABLENESS).count());
        assertEquals(false, answers.stream().anyMatch(answer -> answer.questionSnapshot().isBlank()));
        assertEquals(false, answers.stream().anyMatch(answer -> answer.answerSnapshot().isBlank()));
        assertEquals(false, answers.stream().anyMatch(answer -> !answer.adjectives().isEmpty()));
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
