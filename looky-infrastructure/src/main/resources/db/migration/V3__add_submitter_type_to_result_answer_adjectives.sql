alter table result_answer_adjectives add column submitter_type varchar(20) null after question_id;
alter table result_answer_adjectives add column respondent_label varchar(32) null after submitter_type;
alter table results add column overall_keyword varchar(120) null;
alter table results add column overall_analysis text null;
alter table results add column action_tip text null;
alter table result_quadrants add column definition_keyword varchar(120) null;
alter table result_quadrants add column adjective_keywords_json text null;

update result_answer_adjectives result_answer
set submitter_type = (
    select submission.submitter_type
    from submission_answers submission_answer
    join submission_questions submission_question on submission_question.id = submission_answer.submission_question_id
    join submissions submission on submission.id = submission_question.submission_id
    where submission_answer.id = result_answer.submission_answer_id
);

alter table result_answer_adjectives modify column submitter_type varchar(20) not null;
