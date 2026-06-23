package com.looky.survey.application;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.question.application.QuestionRecord;
import com.looky.submission.application.SubmissionQuestionRecord;
import com.looky.submission.application.SubmissionRecord;
import com.looky.submission.application.SubmissionRepository;
import com.looky.submission.domain.SubmissionStatus;
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
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveyCommandServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-21T18:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final FakeSurveyRepository surveyRepository = new FakeSurveyRepository();
    private final FakeQuestionRepository questionRepository = new FakeQuestionRepository();
    private final FakeSubmissionRepository submissionRepository = new FakeSubmissionRepository();
    private final SurveyCommandService service = new SurveyCommandService(
            surveyRepository,
            questionRepository,
            submissionRepository,
            clock
    );

    @Test
    void createSurveyCreatesSurveyCodeAndResultAvailableAt() {
        SurveyCreatedResult result = service.createSurvey(new CreateSurveyCommand("만두"));

        assertEquals("만두", result.userNickname());
        assertNotNull(result.surveyCode());
        assertEquals(12, result.surveyCode().length());
        assertTrue(result.surveyCode().matches("[A-Za-z0-9_-]{12}"));
        assertFalse(result.surveyCode().contains("_"));
        assertEquals(SurveyStatus.DRAFT, result.surveyStatus());
        assertEquals(ResultStatus.WAITING_SELF_RESPONSE, result.resultStatus());
        assertEquals(3, result.requiredPeerSubmissionCount());
        assertEquals(OffsetDateTime.now(clock).plusHours(24), result.resultAvailableAt());
    }

    @Test
    void startSubmissionStartsSelfWhenNoSelfSubmissionExists() {
        SurveyCreatedResult created = service.createSurvey(new CreateSurveyCommand("만두"));

        SubmissionStartedResult result = service.startSubmission(created.surveyCode());

        assertEquals(SubmitterType.SELF, result.submitterType());
        assertEquals(SubmissionStatus.IN_PROGRESS, result.submissionStatus());
        assertEquals("만두", result.targetNickname());
    }

    @Test
    void startSubmissionFailsWhenSelfSubmissionAlreadyStartedButSurveyIsNotCollecting() {
        SurveyCreatedResult created = service.createSurvey(new CreateSurveyCommand("만두"));
        service.startSubmission(created.surveyCode());

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.startSubmission(created.surveyCode())
        );

        assertEquals(ErrorCode.SURVEY_NOT_COLLECTING, exception.errorCode());
    }

    @Test
    void submitAnswersFailsWhenAnswerCountDoesNotMatchAssignedQuestions() {
        SubmissionRecord submission = submissionRepository.addInProgressSubmission(
                SubmitterType.PEER,
                List.of(
                        new SubmissionQuestionRecord(1L, 10L, Set.of(101L, 102L)),
                        new SubmissionQuestionRecord(2L, 20L, Set.of(201L, 202L))
                )
        );

        LookyException exception = assertThrows(
                LookyException.class,
                () -> service.submitAnswers(
                        submission.id(),
                        new SubmitAnswersCommand(List.of(new AnswerCommand(10L, 101L)))
                )
        );

        assertEquals(ErrorCode.INVALID_ANSWER_COUNT, exception.errorCode());
    }

    @Test
    void getSurveyStatusReturnsWaitingSelfResponseBeforeSelfCompletion() {
        SurveyCreatedResult created = service.createSurvey(new CreateSurveyCommand("만두"));

        SurveyStatusResult result = service.getSurveyStatus(created.surveyCode());

        assertEquals(ResultStatus.WAITING_SELF_RESPONSE, result.resultStatus());
        assertEquals(false, result.selfSubmitted());
        assertEquals(0, result.peerSubmissionCount());
    }

    @Test
    void getSurveyStatusReturnsCollectingPeerResponsesWhenSelfDoneAndPeerCountIsLow() {
        SurveyCreatedResult created = service.createSurvey(new CreateSurveyCommand("만두"));
        submissionRepository.selfCompleted = true;
        submissionRepository.peerCompletedCount = 2;
        surveyRepository.markCollecting(created.surveyId());

        SurveyStatusResult result = service.getSurveyStatus(created.surveyCode());

        assertEquals(ResultStatus.COLLECTING_PEER_RESPONSES, result.resultStatus());
        assertEquals(true, result.selfSubmitted());
        assertEquals(2, result.peerSubmissionCount());
    }

    @Test
    void getSurveyStatusReturnsWaitingResultOpenTimeWhenPeerCountIsEnoughBeforeOpenTime() {
        SurveyCreatedResult created = service.createSurvey(new CreateSurveyCommand("만두"));
        submissionRepository.selfCompleted = true;
        submissionRepository.peerCompletedCount = 3;
        surveyRepository.markCollecting(created.surveyId());

        SurveyStatusResult result = service.getSurveyStatus(created.surveyCode());

        assertEquals(ResultStatus.WAITING_RESULT_OPEN_TIME, result.resultStatus());
        assertEquals(3, result.peerSubmissionCount());
        assertTrue(result.remainingSecondsToResultOpen() > 0);
    }

    private static final class FakeSurveyRepository implements SurveyRepository {
        private long sequence = 1;
        private final Map<Long, SurveyRecord> surveys = new LinkedHashMap<>();

        @Override
        public SurveyRecord saveNewSurvey(String userNickname, String surveyCode, int requiredPeerSubmissionCount, OffsetDateTime now, OffsetDateTime resultAvailableAt) {
            SurveyRecord survey = new SurveyRecord(
                    sequence++,
                    userNickname,
                    surveyCode,
                    SurveyStatus.DRAFT,
                    ResultStatus.WAITING_SELF_RESPONSE,
                    requiredPeerSubmissionCount,
                    resultAvailableAt,
                    now
            );
            surveys.put(survey.id(), survey);
            return survey;
        }

        @Override
        public Optional<SurveyRecord> findBySurveyCode(String surveyCode) {
            return surveys.values().stream()
                    .filter(survey -> survey.surveyCode().equals(surveyCode))
                    .findFirst();
        }

        @Override
        public void markCollecting(Long surveyId) {
            SurveyRecord survey = surveys.get(surveyId);
            surveys.put(surveyId, new SurveyRecord(
                    survey.id(),
                    survey.userNickname(),
                    survey.surveyCode(),
                    SurveyStatus.COLLECTING,
                    survey.resultStatus(),
                    survey.requiredPeerSubmissionCount(),
                    survey.resultAvailableAt(),
                    survey.createdAt()
            ));
        }
    }

    private static final class FakeQuestionRepository implements com.looky.question.application.QuestionRepository {
        @Override
        public List<QuestionRecord> findRandomActiveQuestions(int count, SubmitterType submitterType) {
            List<QuestionRecord> questions = new ArrayList<>();
            for (long i = 1; i <= count; i++) {
                questions.add(new QuestionRecord(
                        i,
                        "질문 " + i,
                        List.of(
                                new QuestionRecord.AnswerOptionRecord(i * 10 + 1, 1, "답변 1"),
                                new QuestionRecord.AnswerOptionRecord(i * 10 + 2, 2, "답변 2"),
                                new QuestionRecord.AnswerOptionRecord(i * 10 + 3, 3, "답변 3"),
                                new QuestionRecord.AnswerOptionRecord(i * 10 + 4, 4, "답변 4"),
                                new QuestionRecord.AnswerOptionRecord(i * 10 + 5, 5, "답변 5")
                        )
                ));
            }
            return questions;
        }
    }

    private static final class FakeSubmissionRepository implements SubmissionRepository {
        private long sequence = 1;
        private boolean selfCompleted;
        private long peerCompletedCount;
        private final Map<Long, SubmissionRecord> submissions = new LinkedHashMap<>();

        @Override
        public boolean existsSelfSubmission(Long surveyId) {
            return submissions.values().stream()
                    .anyMatch(submission -> submission.surveyId().equals(surveyId)
                            && submission.submitterType() == SubmitterType.SELF);
        }

        @Override
        public boolean existsCompletedSelfSubmission(Long surveyId) {
            return selfCompleted || submissions.values().stream()
                    .anyMatch(submission -> submission.surveyId().equals(surveyId)
                            && submission.submitterType() == SubmitterType.SELF
                            && submission.submissionStatus() == SubmissionStatus.COMPLETED);
        }

        @Override
        public long countCompletedPeerSubmissions(Long surveyId) {
            return peerCompletedCount;
        }

        @Override
        public SubmissionStartedResult saveStartedSubmission(Long surveyId, String targetNickname, SubmitterType submitterType, String submitterKey, List<QuestionRecord> questions, OffsetDateTime now) {
            long submissionId = sequence++;
            List<SubmissionQuestionRecord> assigned = questions.stream()
                    .map(question -> new SubmissionQuestionRecord(
                            question.questionId(),
                            question.questionId(),
                            question.options().stream().map(QuestionRecord.AnswerOptionRecord::answerOptionId).collect(java.util.stream.Collectors.toSet())
                    ))
                    .toList();
            submissions.put(submissionId, new SubmissionRecord(submissionId, surveyId, submitterType, SubmissionStatus.IN_PROGRESS, assigned));

            return new SubmissionStartedResult(
                    submissionId,
                    submitterType,
                    SubmissionStatus.IN_PROGRESS,
                    targetNickname,
                    questions.stream()
                            .map(question -> new com.looky.survey.application.dto.QuestionResult(
                                    question.questionId(),
                                    Math.toIntExact(question.questionId()),
                                    question.content(),
                                    question.options().stream()
                                            .map(option -> new com.looky.survey.application.dto.QuestionResult.AnswerOptionResult(option.answerOptionId(), option.sequence(), option.content()))
                                            .toList()
                            ))
                            .toList()
            );
        }

        @Override
        public Optional<SubmissionRecord> findInProgressSubmission(Long submissionId) {
            return Optional.ofNullable(submissions.get(submissionId));
        }

        @Override
        public SubmissionCompletedResult completeSubmission(Long submissionId, List<AnswerCommand> answers, OffsetDateTime now) {
            SubmissionRecord submission = submissions.get(submissionId);
            submissions.put(submissionId, new SubmissionRecord(
                    submission.id(),
                    submission.surveyId(),
                    submission.submitterType(),
                    SubmissionStatus.COMPLETED,
                    submission.questions()
            ));
            if (submission.submitterType() == SubmitterType.SELF) {
                selfCompleted = true;
            } else {
                peerCompletedCount++;
            }
            return new SubmissionCompletedResult(submissionId, submission.submitterType(), SubmissionStatus.COMPLETED, now);
        }

        SubmissionRecord addInProgressSubmission(SubmitterType submitterType, List<SubmissionQuestionRecord> questions) {
            long id = sequence++;
            SubmissionRecord submission = new SubmissionRecord(id, 1L, submitterType, SubmissionStatus.IN_PROGRESS, questions);
            submissions.put(id, submission);
            return submission;
        }
    }
}
