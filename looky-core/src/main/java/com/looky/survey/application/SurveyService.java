package com.looky.survey.application;

import com.looky.survey.application.dto.CreateSurveyCommand;
import com.looky.survey.application.dto.SubmissionCompletedResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import com.looky.survey.application.dto.SubmitAnswersCommand;
import com.looky.survey.application.dto.SurveyCreatedResult;
import com.looky.survey.application.dto.SurveyStatusResult;

public interface SurveyService {
    SurveyCreatedResult createSurvey(CreateSurveyCommand command);

    SubmissionStartedResult startSubmission(String surveyCode);

    SubmissionCompletedResult submitAnswers(Long submissionId, SubmitAnswersCommand command);

    SurveyStatusResult getSurveyStatus(String surveyCode);
}
