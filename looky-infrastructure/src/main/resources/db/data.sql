insert into questions (id, content_self, content_peer, question_type, trait_code, active, created_at, updated_at) values
(1, '갑자기 비어 있는 주말이 생겼다. 나는 어떻게 할까?', '이 사람이 갑자기 비어 있는 주말이 생기면 어떻게 할 것 같나요?', 'SITUATION_5_CHOICE', 'OPENNESS', true, current_timestamp, current_timestamp),
(2, '친구들과 약속 장소를 정해야 한다. 나는 어떻게 할까?', '친구들과 약속 장소를 정해야 할 때 이 사람은 어떻게 할 것 같나요?', 'SITUATION_5_CHOICE', 'CONSCIENTIOUSNESS', true, current_timestamp, current_timestamp),
(3, '처음 보는 사람들이 많은 자리에 갔다. 나는 어떻게 할까?', '처음 보는 사람들이 많은 자리에서 이 사람은 어떻게 할 것 같나요?', 'SITUATION_5_CHOICE', 'EXTRAVERSION', true, current_timestamp, current_timestamp),
(4, '친구가 고민을 털어놓았다. 나는 어떻게 할까?', '친구가 고민을 털어놓으면 이 사람은 어떻게 할 것 같나요?', 'SITUATION_5_CHOICE', 'AGREEABLENESS', true, current_timestamp, current_timestamp),
(5, '계획이 갑자기 바뀌었다. 나는 어떻게 할까?', '계획이 갑자기 바뀌면 이 사람은 어떻게 할 것 같나요?', 'SITUATION_5_CHOICE', 'OPENNESS', true, current_timestamp, current_timestamp),
(6, '마감이 가까운 일이 생겼다. 나는 어떻게 할까?', '마감이 가까운 일이 생기면 이 사람은 어떻게 할 것 같나요?', 'SITUATION_5_CHOICE', 'CONSCIENTIOUSNESS', true, current_timestamp, current_timestamp),
(7, '모임 분위기가 조용해졌다. 나는 어떻게 할까?', '모임 분위기가 조용해지면 이 사람은 어떻게 할 것 같나요?', 'SITUATION_5_CHOICE', 'EXTRAVERSION', true, current_timestamp, current_timestamp),
(8, '친구가 작은 부탁을 했다. 나는 어떻게 할까?', '친구가 작은 부탁을 하면 이 사람은 어떻게 할 것 같나요?', 'SITUATION_5_CHOICE', 'AGREEABLENESS', true, current_timestamp, current_timestamp);

insert into answer_options (question_id, content_self, content_peer, sequence, active, created_at, updated_at)
select id, '익숙한 방식을 고른다.', '익숙한 방식을 고를 것 같다.', 1, true, current_timestamp, current_timestamp from questions
union all
select id, '조금 새로운 선택을 해본다.', '조금 새로운 선택을 할 것 같다.', 2, true, current_timestamp, current_timestamp from questions
union all
select id, '상황을 보고 중간을 택한다.', '상황을 보고 중간을 택할 것 같다.', 3, true, current_timestamp, current_timestamp from questions
union all
select id, '먼저 움직여본다.', '먼저 움직일 것 같다.', 4, true, current_timestamp, current_timestamp from questions
union all
select id, '주변 사람까지 끌어들여 적극적으로 해본다.', '주변 사람까지 끌어들여 적극적으로 할 것 같다.', 5, true, current_timestamp, current_timestamp from questions;

insert into character_packs (id, pack_key, name, created_at, updated_at) values
(1, 'pomang', 'Pomang', current_timestamp, current_timestamp);

insert into character_pack_versions (id, pack_id, version, base_asset_key, active, created_at, updated_at) values
(1, 1, 'v1', 'character-packs/pomang/v1/base/base.png', true, current_timestamp, current_timestamp);

insert into character_pack_variants (version_id, variant_key, quadrant_type, asset_key, sort_order, created_at, updated_at) values
(1, 'open-cheer', 'OPEN', 'character-packs/pomang/v1/variants/open-cheer.png', 0, current_timestamp, current_timestamp),
(1, 'blind-magnifier', 'BLIND', 'character-packs/pomang/v1/variants/blind-magnifier.png', 0, current_timestamp, current_timestamp),
(1, 'hidden-letter', 'HIDDEN', 'character-packs/pomang/v1/variants/hidden-letter.png', 0, current_timestamp, current_timestamp),
(1, 'unknown-teary', 'UNKNOWN', 'character-packs/pomang/v1/variants/unknown-teary.png', 0, current_timestamp, current_timestamp);
