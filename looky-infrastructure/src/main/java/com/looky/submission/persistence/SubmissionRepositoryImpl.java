package com.looky.submission.persistence;

import com.looky.question.application.QuestionRecord;
import com.looky.submission.application.SubmissionQuestionRecord;
import com.looky.submission.application.SubmissionRecord;
import com.looky.submission.application.SubmissionRepository;
import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.QuestionResult;
import com.looky.survey.application.dto.SubmissionCompletedResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import com.looky.question.persistence.AnswerOptionJpaEntity;
import com.looky.question.persistence.AnswerOptionJpaRepository;
import com.looky.question.persistence.QuestionJpaEntity;
import com.looky.question.persistence.QuestionJpaRepository;
import com.looky.survey.persistence.SurveyJpaEntity;
import com.looky.survey.persistence.SurveyJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@Slf4j
@Transactional
public class SubmissionRepositoryImpl implements SubmissionRepository {

    private final SurveyJpaRepository surveyJpaRepository;
    private final QuestionJpaRepository questionJpaRepository;
    private final AnswerOptionJpaRepository answerOptionJpaRepository;
    private final SubmissionJpaRepository submissionJpaRepository;
    private final SubmissionQuestionJpaRepository submissionQuestionJpaRepository;
    private final SubmissionAnswerJpaRepository submissionAnswerJpaRepository;

    public SubmissionRepositoryImpl(
            SurveyJpaRepository surveyJpaRepository,
            QuestionJpaRepository questionJpaRepository,
            AnswerOptionJpaRepository answerOptionJpaRepository,
            SubmissionJpaRepository submissionJpaRepository,
            SubmissionQuestionJpaRepository submissionQuestionJpaRepository,
            SubmissionAnswerJpaRepository submissionAnswerJpaRepository
    ) {
        this.surveyJpaRepository = surveyJpaRepository;
        this.questionJpaRepository = questionJpaRepository;
        this.answerOptionJpaRepository = answerOptionJpaRepository;
        this.submissionJpaRepository = submissionJpaRepository;
        this.submissionQuestionJpaRepository = submissionQuestionJpaRepository;
        this.submissionAnswerJpaRepository = submissionAnswerJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSelfSubmission(Long surveyId) {
        return submissionJpaRepository.existsBySurvey_IdAndSubmitterType(surveyId, SubmitterType.SELF);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsCompletedSelfSubmission(Long surveyId) {
        return submissionJpaRepository.existsBySurvey_IdAndSubmitterTypeAndSubmissionStatus(surveyId, SubmitterType.SELF, SubmissionStatus.COMPLETED);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompletedPeerSubmissions(Long surveyId) {
        return submissionJpaRepository.countBySurvey_IdAndSubmitterTypeAndSubmissionStatus(surveyId, SubmitterType.PEER, SubmissionStatus.COMPLETED);
    }

    @Override
    public SubmissionStartedResult saveStartedSubmission(Long surveyId, String targetNickname, SubmitterType submitterType, String submitterKey, List<QuestionRecord> questions, OffsetDateTime now) {
        SurveyJpaEntity survey = surveyJpaRepository.findById(surveyId).orElseThrow();
        SubmissionJpaEntity submission = submissionJpaRepository.save(new SubmissionJpaEntity(survey, submitterType, submitterKey, now));
        Map<Long, QuestionJpaEntity> questionEntities = questionJpaRepository.findAllById(questions.stream().map(QuestionRecord::questionId).toList())
                .stream()
                .collect(Collectors.toMap(QuestionJpaEntity::getId, question -> question));

        List<SubmissionQuestionJpaEntity> submissionQuestions = IntStream.range(0, questions.size())
                .mapToObj(index -> new SubmissionQuestionJpaEntity(
                        submission,
                        questionEntities.get(questions.get(index).questionId()),
                        index + 1,
                        questions.get(index).content(),
                        now
                ))
                .toList();
        submissionQuestionJpaRepository.saveAll(submissionQuestions);
        log.info(
                "submission.persistence.started surveyId={} submissionId={} submitterType={} submitterKey={} questionCount={}",
                surveyId,
                submission.getId(),
                submitterType,
                submitterKey,
                questions.size()
        );

        return new SubmissionStartedResult(
                submission.getId(),
                submitterType,
                SubmissionStatus.IN_PROGRESS,
                targetNickname,
                IntStream.range(0, questions.size())
                        .mapToObj(index -> toQuestionResult(questions.get(index), index + 1))
                        .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubmissionRecord> findInProgressSubmission(Long submissionId) {
        return submissionJpaRepository.findByIdAndSubmissionStatus(submissionId, SubmissionStatus.IN_PROGRESS)
                .map(submission -> {
                    List<SubmissionQuestionJpaEntity> submissionQuestions = submissionQuestionJpaRepository.findBySubmission_IdOrderBySequenceAsc(submission.getId());
                    List<Long> questionIds = submissionQuestions.stream().map(SubmissionQuestionJpaEntity::getQuestionId).toList();
                    Map<Long, Set<Long>> answerOptionIdsByQuestionId = answerOptionJpaRepository
                            .findByQuestion_IdInAndActiveTrueOrderByQuestion_IdAscSequenceAsc(questionIds)
                            .stream()
                            .collect(Collectors.groupingBy(
                                    AnswerOptionJpaEntity::getQuestionId,
                                    Collectors.mapping(AnswerOptionJpaEntity::getId, Collectors.toSet())
                            ));

                    List<SubmissionQuestionRecord> questionRecords = submissionQuestions.stream()
                            .map(question -> new SubmissionQuestionRecord(
                                    question.getId(),
                                    question.getQuestionId(),
                                    answerOptionIdsByQuestionId.getOrDefault(question.getQuestionId(), Set.of())
                            ))
                            .toList();

                    return new SubmissionRecord(
                            submission.getId(),
                            submission.getSurvey().getId(),
                            submission.getSubmitterType(),
                            submission.getSubmissionStatus(),
                            questionRecords
                    );
                });
    }

    @Override
    public SubmissionCompletedResult completeSubmission(Long submissionId, List<AnswerCommand> answers, OffsetDateTime now) {
        SubmissionJpaEntity submission = submissionJpaRepository.findByIdAndSubmissionStatus(submissionId, SubmissionStatus.IN_PROGRESS).orElseThrow();
        Map<Long, SubmissionQuestionJpaEntity> submissionQuestionByQuestionId = submissionQuestionJpaRepository
                .findBySubmission_IdAndQuestion_IdIn(submissionId, answers.stream().map(AnswerCommand::questionId).toList())
                .stream()
                .collect(Collectors.toMap(SubmissionQuestionJpaEntity::getQuestionId, question -> question));
        Map<Long, AnswerOptionJpaEntity> answerOptionById = answerOptionJpaRepository.findAllById(answers.stream().map(AnswerCommand::answerOptionId).toList())
                .stream()
                .collect(Collectors.toMap(AnswerOptionJpaEntity::getId, option -> option));

        List<SubmissionAnswerJpaEntity> answerEntities = answers.stream()
                .map(answer -> {
                    SubmissionQuestionJpaEntity submissionQuestion = submissionQuestionByQuestionId.get(answer.questionId());
                    AnswerOptionJpaEntity answerOption = answerOptionById.get(answer.answerOptionId());
                    String snapshot = submission.getSubmitterType() == SubmitterType.SELF
                            ? answerOption.getContentSelf()
                            : answerOption.getContentPeer();
                    return new SubmissionAnswerJpaEntity(submissionQuestion, answerOption, snapshot, now);
                })
                .toList();
        submissionAnswerJpaRepository.saveAll(answerEntities);
        submission.complete(now);
        log.info(
                "submission.persistence.completed surveyId={} submissionId={} submitterType={} submitterKey={} answerCount={} submittedAt={}",
                submission.getSurvey().getId(),
                submission.getId(),
                submission.getSubmitterType(),
                submission.getSubmitterKey(),
                answers.size(),
                now
        );

        return new SubmissionCompletedResult(submission.getId(), submission.getSubmitterType(), SubmissionStatus.COMPLETED, now);
    }

    private QuestionResult toQuestionResult(QuestionRecord question, int sequence) {
        return new QuestionResult(
                question.questionId(),
                sequence,
                question.content(),
                question.options().stream()
                        .map(option -> new QuestionResult.AnswerOptionResult(option.answerOptionId(), option.sequence(), option.content()))
                        .toList()
        );
    }
}
