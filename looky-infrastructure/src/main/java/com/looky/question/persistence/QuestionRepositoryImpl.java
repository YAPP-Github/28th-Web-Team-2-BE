package com.looky.question.persistence;

import com.looky.question.application.QuestionRecord;
import com.looky.question.application.QuestionRepository;
import com.looky.question.domain.TraitCode;
import com.looky.submission.domain.SubmitterType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public List<QuestionRecord> findRandomActiveQuestionsByTrait(int countPerTrait, SubmitterType submitterType) {
        List<QuestionJpaEntity> selectedQuestions = new ArrayList<>();
        for (TraitCode traitCode : TraitCode.values()) {
            List<QuestionJpaEntity> questions = new ArrayList<>(questionJpaRepository.findByActiveTrueAndTraitCode(traitCode));
            Collections.shuffle(questions);
            selectedQuestions.addAll(questions.stream().limit(countPerTrait).toList());
        }
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
        String content = submitterType == SubmitterType.SELF ? question.getContentSelf() : question.getContentPeer();
        String contentTemplate = submitterType == SubmitterType.SELF
                ? question.getContentSelfTemplate()
                : question.getContentPeerTemplate();

        return new QuestionRecord(
                question.getId(),
                question.getTraitCode(),
                content,
                contentTemplate,
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
