package com.looky.result.application;

import com.looky.survey.application.SurveyRecord;

public interface ResultGeneratorClient {
    GeneratedResult generate(SurveyRecord survey);
}
