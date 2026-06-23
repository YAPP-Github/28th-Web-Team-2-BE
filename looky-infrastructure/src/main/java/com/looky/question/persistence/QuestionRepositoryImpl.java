package com.looky.question.persistence;

import com.looky.question.application.QuestionRecord;
import com.looky.question.application.QuestionRepository;
import com.looky.submission.domain.SubmitterType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class QuestionRepositoryImpl implements QuestionRepository {

    private final QuestionJpaRepository questionJpaRepository;
    private final AnswerOptionJpaRepository answerOptionJpaRepository;

    public QuestionRepositoryImpl(QuestionJpaRepository questionJpaRepository, AnswerOptionJpaRepository answerOptionJpaRepository) {
        this.questionJpaRepository = questionJpaRepository;
        this.answerOptionJpaRepository = answerOptionJpaRepository;
    }

    @Override
    public List<QuestionRecord> findRandomActiveQuestions(int count, SubmitterType submitterType) {
        List<QuestionJpaEntity> questions = questionJpaRepository.findByActiveTrueOrderByIdAsc();
        Collections.shuffle(questions);
        List<QuestionJpaEntity> selectedQuestions = questions.stream().limit(count).toList();
        List<Long> questionIds = selectedQuestions.stream().map(QuestionJpaEntity::getId).toList();
        Map<Long, List<AnswerOptionJpaEntity>> optionsByQuestionId = answerOptionJpaRepository
                .findByQuestion_IdInAndActiveTrueOrderByQuestion_IdAscSequenceAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(AnswerOptionJpaEntity::getQuestionId));

        return selectedQuestions.stream()
                .map(question -> toRecord(question, optionsByQuestionId.getOrDefault(question.getId(), List.of()), submitterType))
                .toList();
    }

    private QuestionRecord toRecord(QuestionJpaEntity question, List<AnswerOptionJpaEntity> options, SubmitterType submitterType) {
        return new QuestionRecord(
                question.getId(),
                submitterType == SubmitterType.SELF ? question.getContentSelf() : question.getContentPeer(),
                options.stream()
                        .sorted(Comparator.comparingInt(AnswerOptionJpaEntity::getSequence))
                        .map(option -> new QuestionRecord.AnswerOptionRecord(
                                option.getId(),
                                option.getSequence(),
                                submitterType == SubmitterType.SELF ? option.getContentSelf() : option.getContentPeer()
                        ))
                        .toList()
        );
    }
}
