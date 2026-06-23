package com.looky.survey.application.dto;

import java.util.List;

public record SubmitAnswersCommand(List<AnswerCommand> answers) {
}
