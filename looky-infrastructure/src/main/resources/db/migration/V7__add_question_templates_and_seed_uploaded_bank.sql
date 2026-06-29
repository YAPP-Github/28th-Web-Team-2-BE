set @content_self_template_column_exists = (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'questions'
      and column_name = 'content_self_template'
);
set @add_content_self_template_column = if(
    @content_self_template_column_exists = 0,
    'alter table questions add column content_self_template text',
    'select 1'
);
prepare add_content_self_template_column_stmt from @add_content_self_template_column;
execute add_content_self_template_column_stmt;
deallocate prepare add_content_self_template_column_stmt;

set @content_peer_template_column_exists = (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'questions'
      and column_name = 'content_peer_template'
);
set @add_content_peer_template_column = if(
    @content_peer_template_column_exists = 0,
    'alter table questions add column content_peer_template text',
    'select 1'
);
prepare add_content_peer_template_column_stmt from @add_content_peer_template_column;
execute add_content_peer_template_column_stmt;
deallocate prepare add_content_peer_template_column_stmt;

update questions
set content_self_template = content_self
where content_self_template is null;

update questions
set content_peer_template = content_peer
where content_peer_template is null;
