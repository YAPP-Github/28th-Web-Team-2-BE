package com.looky.survey.application;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.question.application.QuestionRecord;
import com.looky.question.application.QuestionRepository;
import com.looky.question.domain.TraitCode;
import com.looky.submission.application.SubmissionQuestionRecord;
import com.looky.submission.application.SubmissionRecord;
import com.looky.submission.application.SubmissionRepository;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.CreateSurveyCommand;
import com.looky.survey.application.dto.SubmissionCompletedResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import com.looky.survey.application.dto.SubmitAnswersCommand;
import com.looky.survey.application.dto.SurveyCreatedResult;
import com.looky.survey.application.dto.SurveyStatusResult;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class SurveyCommandService implements SurveyService {

    private static final int QUESTION_COUNT_PER_TRAIT = 2;
    private static final int REQUIRED_PEER_SUBMISSION_COUNT = 3;
    private static final char[] CODE_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;
    private final Clock clock;
    private final SurveyPolicy surveyPolicy;
    private final ResultStatusResolver resultStatusResolver;
    private final SecureRandom random = new SecureRandom();

    @Override
    public SurveyCreatedResult createSurvey(CreateSurveyCommand command) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        SurveyRecord survey = surveyRepository.saveNewSurvey(
                command.userNickname(),
                generateCode(),
                REQUIRED_PEER_SUBMISSION_COUNT,
                now,
                now.plus(surveyPolicy.resultOpenDelay())
        );

        return new SurveyCreatedResult(
                survey.id(),
                survey.userNickname(),
                survey.surveyCode(),
                survey.surveyStatus(),
                survey.resultStatus(),
                survey.requiredPeerSubmissionCount(),
                survey.resultAvailableAt(),
                survey.createdAt()
        );
    }

    @Override
    public SubmissionStartedResult startSubmission(String surveyCode) {
        SurveyRecord survey = surveyRepository.findBySurveyCode(surveyCode)
                .orElseThrow(() -> new LookyException(ErrorCode.INVALID_SURVEY_CODE));
        if (!submissionRepository.existsSelfSubmission(survey.id())) {
            List<QuestionRecord> questions = pickQuestions(SubmitterType.SELF);
            return submissionRepository.saveStartedSubmission(
                    survey.id(),
                    survey.userNickname(),
                    SubmitterType.SELF,
                    "SELF",
                    questions,
                    OffsetDateTime.now(clock)
            );
        }
        if (survey.surveyStatus() != SurveyStatus.COLLECTING) {
            throw new LookyException(ErrorCode.SURVEY_NOT_COLLECTING);
        }

        List<QuestionRecord> questions = pickQuestions(SubmitterType.PEER);
        return submissionRepository.saveStartedSubmission(
                survey.id(),
                survey.userNickname(),
                SubmitterType.PEER,
                generateCode(),
                questions,
                OffsetDateTime.now(clock)
        );
    }

    @Override
    public SubmissionCompletedResult submitAnswers(Long submissionId, SubmitAnswersCommand command) {
        SubmissionRecord submission = submissionRepository.findInProgressSubmission(submissionId)
                .orElseThrow(() -> new LookyException(ErrorCode.SUBMISSION_NOT_FOUND));
        validateAnswers(submission, command.answers());

        SubmissionCompletedResult result = submissionRepository.completeSubmission(
                submissionId,
                command.answers(),
                OffsetDateTime.now(clock)
        );
        if (submission.submitterType() == SubmitterType.SELF) {
            surveyRepository.markCollecting(submission.surveyId());
        }
        return result;
    }

    @Override
    public SurveyStatusResult getSurveyStatus(String surveyCode) {
        SurveyRecord survey = surveyRepository.findBySurveyCode(surveyCode)
                .orElseThrow(() -> new LookyException(ErrorCode.INVALID_SURVEY_CODE));
        boolean selfSubmitted = submissionRepository.existsCompletedSelfSubmission(survey.id());
        long peerSubmissionCount = submissionRepository.countCompletedPeerSubmissions(survey.id());
        ResultStatus resultStatus = resultStatusResolver.resolve(survey);
        long remainingSeconds = Math.max(0, Duration.between(OffsetDateTime.now(clock), survey.resultAvailableAt()).toSeconds());

        return new SurveyStatusResult(
                survey.id(),
                survey.userNickname(),
                survey.surveyStatus(),
                resultStatus,
                selfSubmitted,
                peerSubmissionCount,
                survey.requiredPeerSubmissionCount(),
                survey.resultAvailableAt(),
                remainingSeconds,
                survey.surveyCode()
        );
    }

    private List<QuestionRecord> pickQuestions(SubmitterType submitterType) {
        List<QuestionRecord> questions = questionRepository.findRandomActiveQuestionsByTrait(QUESTION_COUNT_PER_TRAIT, submitterType);
        if (questions.size() != TraitCode.values().length * QUESTION_COUNT_PER_TRAIT
                || Arrays.stream(TraitCode.values()).anyMatch(traitCode -> questions.stream()
                .filter(question -> question.traitCode() == traitCode)
                .count() != QUESTION_COUNT_PER_TRAIT)) {
            throw new LookyException(ErrorCode.NOT_ENOUGH_ACTIVE_QUESTIONS);
        }
        return questions;
    }

    private void validateAnswers(SubmissionRecord submission, List<AnswerCommand> answers) {
        if (answers == null || answers.size() != submission.questions().size()) {
            throw new LookyException(ErrorCode.INVALID_ANSWER_COUNT);
        }

        Map<Long, SubmissionQuestionRecord> assignedQuestions = new HashMap<>();
        for (SubmissionQuestionRecord question : submission.questions()) {
            assignedQuestions.put(question.questionId(), question);
        }

        HashSet<Long> seenQuestionIds = new HashSet<>();
        for (AnswerCommand answer : answers) {
            if (answer.questionId() == null || answer.answerOptionId() == null) {
                throw new LookyException(ErrorCode.ANSWER_REQUIRED);
            }
            if (!seenQuestionIds.add(answer.questionId())) {
                throw new LookyException(ErrorCode.DUPLICATED_QUESTION);
            }

            SubmissionQuestionRecord assignedQuestion = assignedQuestions.get(answer.questionId());
            if (assignedQuestion == null) {
                throw new LookyException(ErrorCode.QUESTION_NOT_IN_SUBMISSION);
            }
            if (!assignedQuestion.answerOptionIds().contains(answer.answerOptionId())) {
                throw new LookyException(ErrorCode.INVALID_ANSWER_OPTION);
            }
        }
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        }
        return builder.toString();
    }
}
