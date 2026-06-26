package com.looky.survey.application;

import com.looky.characterpack.application.CharacterPackRepository;
import com.looky.characterpack.application.CharacterPackSnapshot;
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
import lombok.extern.slf4j.Slf4j;
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
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class SurveyCommandService implements SurveyService {

    private static final int QUESTION_COUNT_PER_TRAIT = 2;
    private static final int REQUIRED_PEER_SUBMISSION_COUNT = 3;
    private static final int CODE_LENGTH = 6;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;
    private static final char[] CODE_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int HANGUL_SYLLABLE_BASE = 0xAC00;
    private static final int HANGUL_SYLLABLE_LAST = 0xD7A3;
    private static final int HANGUL_JONGSEONG_COUNT = 28;

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;
    private final CharacterPackRepository characterPackRepository;
    private final Clock clock;
    private final SurveyPolicy surveyPolicy;
    private final ResultStatusResolver resultStatusResolver;
    private final SecureRandom random = new SecureRandom();

    @Override
    public SurveyCreatedResult createSurvey(CreateSurveyCommand command) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        SurveyRecord survey = saveNewSurveyWithRetry(command.userNickname(), now);
        log.info(
                "survey.created surveyId={} surveyCode={} ownerNickname={} resultStatus={} surveyStatus={} requiredPeerSubmissionCount={} resultAvailableAt={} characterPackKey={} characterPackVersion={}",
                survey.id(),
                survey.surveyCode(),
                survey.userNickname(),
                survey.resultStatus(),
                survey.surveyStatus(),
                survey.requiredPeerSubmissionCount(),
                survey.resultAvailableAt(),
                survey.characterPackKey(),
                survey.characterPackVersion()
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
            List<QuestionRecord> questions = personalizeQuestions(survey.userNickname(), pickQuestions(SubmitterType.SELF));
            SubmissionStartedResult result = submissionRepository.saveStartedSubmission(
                    survey.id(),
                    survey.userNickname(),
                    SubmitterType.SELF,
                    "SELF",
                    questions,
                    OffsetDateTime.now(clock)
            );
            log.info(
                    "submission.started surveyId={} surveyCode={} submissionId={} submitterType={} targetNickname={} questionCount={}",
                    survey.id(),
                    survey.surveyCode(),
                    result.submissionId(),
                    result.submitterType(),
                    result.targetNickname(),
                    result.questions().size()
            );
            return result;
        }
        if (survey.surveyStatus() != SurveyStatus.COLLECTING) {
            throw new LookyException(ErrorCode.SURVEY_NOT_COLLECTING);
        }

        List<QuestionRecord> questions = personalizeQuestions(survey.userNickname(), pickQuestions(SubmitterType.PEER));
        SubmissionStartedResult result = savePeerSubmissionWithRetry(survey, questions);
        log.info(
                "submission.started surveyId={} surveyCode={} submissionId={} submitterType={} targetNickname={} questionCount={}",
                survey.id(),
                survey.surveyCode(),
                result.submissionId(),
                result.submitterType(),
                result.targetNickname(),
                result.questions().size()
        );
        return result;
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
        SurveyRecord survey = surveyRepository.findById(submission.surveyId())
                .orElseThrow(() -> new LookyException(ErrorCode.INTERNAL_SERVER_ERROR));
        long peerSubmissionCount = submissionRepository.countCompletedPeerSubmissions(submission.surveyId());
        log.info(
                "submission.completed surveyId={} surveyCode={} submissionId={} submitterType={} peerSubmissionCount={} requiredPeerSubmissionCount={} resultStatus={} submittedAt={}",
                survey.id(),
                survey.surveyCode(),
                submissionId,
                submission.submitterType(),
                peerSubmissionCount,
                survey.requiredPeerSubmissionCount(),
                resultStatusResolver.resolve(survey),
                result.submittedAt()
        );
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

    private List<QuestionRecord> personalizeQuestions(String targetNickname, List<QuestionRecord> questions) {
        return questions.stream()
                .map(question -> new QuestionRecord(
                        question.questionId(),
                        question.traitCode(),
                        personalizeQuestionContent(question.content(), targetNickname),
                        question.options()
                ))
                .toList();
    }

    private String personalizeQuestionContent(String content, String targetNickname) {
        if (content == null || targetNickname == null) {
            return content;
        }
        String nickname = targetNickname.trim();
        if (nickname.isEmpty() || !content.contains("나는?")) {
            return content;
        }
        return content.replace("나는?", nickname + topicParticle(nickname) + "?");
    }

    private String topicParticle(String nickname) {
        char lastCharacter = nickname.charAt(nickname.length() - 1);
        if (lastCharacter < HANGUL_SYLLABLE_BASE || lastCharacter > HANGUL_SYLLABLE_LAST) {
            return "는";
        }
        return ((lastCharacter - HANGUL_SYLLABLE_BASE) % HANGUL_JONGSEONG_COUNT) == 0 ? "는" : "은";
    }

    private SurveyRecord saveNewSurveyWithRetry(String userNickname, OffsetDateTime now) {
        OffsetDateTime resultAvailableAt = now.plus(surveyPolicy.resultOpenDelay());
        CharacterPackSnapshot snapshot = characterPackRepository.findActiveSnapshot()
                .orElseThrow(() -> new LookyException(ErrorCode.INTERNAL_SERVER_ERROR));
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            try {
                return surveyRepository.saveNewSurvey(
                        userNickname,
                        generateCode(),
                        REQUIRED_PEER_SUBMISSION_COUNT,
                        now,
                        resultAvailableAt,
                        snapshot.packKey(),
                        snapshot.packVersion()
                );
            } catch (RuntimeException exception) {
                if (!isDuplicateCodeException(exception, "survey_code", "uk_surveys_survey_code")) {
                    throw exception;
                }
            }
        }
        throw new LookyException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private SubmissionStartedResult savePeerSubmissionWithRetry(SurveyRecord survey, List<QuestionRecord> questions) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            try {
                return submissionRepository.saveStartedSubmission(
                        survey.id(),
                        survey.userNickname(),
                        SubmitterType.PEER,
                        generateCode(),
                        questions,
                        now
                );
            } catch (RuntimeException exception) {
                if (!isDuplicateCodeException(exception, "submitter_key", "uk_submissions_submitter_key")) {
                    throw exception;
                }
            }
        }
        throw new LookyException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private boolean isDuplicateCodeException(RuntimeException exception, String... keywords) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                for (String keyword : keywords) {
                    if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
            }
            cause = cause.getCause();
        }
        return false;
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

    protected String generateCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        }
        return builder.toString();
    }
}
