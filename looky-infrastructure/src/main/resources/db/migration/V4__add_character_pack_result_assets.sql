create table character_packs (id bigint auto_increment primary key,pack_key varchar(80) not null,name varchar(120) not null,created_at datetime(6) not null,updated_at datetime(6) not null,unique(pack_key));
create table character_pack_versions (id bigint auto_increment primary key,pack_id bigint not null,version varchar(40) not null,base_asset_key varchar(255) not null,active boolean not null,created_at datetime(6) not null,updated_at datetime(6) not null,unique(pack_id,version),foreign key(pack_id) references character_packs(id));
create table character_pack_variants (id bigint auto_increment primary key,version_id bigint not null,variant_key varchar(120) not null,quadrant_type varchar(32) not null,asset_key varchar(255) not null,sort_order int not null,created_at datetime(6) not null,updated_at datetime(6) not null,unique(version_id,variant_key),unique(version_id,quadrant_type,sort_order),foreign key(version_id) references character_pack_versions(id));
alter table surveys add column character_pack_key varchar(80);
alter table surveys add column character_pack_version varchar(40);
alter table result_quadrants add column selected_variant_key varchar(120);
insert into character_packs (pack_key,name,created_at,updated_at) values ('pomang','Pomang',current_timestamp,current_timestamp);
insert into character_pack_versions (pack_id,version,base_asset_key,active,created_at,updated_at) values ((select id from character_packs where pack_key='pomang'),'v1','character-packs/pomang/v1/base/base.png',true,current_timestamp,current_timestamp);
insert into character_pack_variants (version_id,variant_key,quadrant_type,asset_key,sort_order,created_at,updated_at) values
((select id from character_pack_versions where version='v1' and pack_id=(select id from character_packs where pack_key='pomang')),'open-cheer','OPEN','character-packs/pomang/v1/variants/open-cheer.png',0,current_timestamp,current_timestamp),
((select id from character_pack_versions where version='v1' and pack_id=(select id from character_packs where pack_key='pomang')),'blind-magnifier','BLIND','character-packs/pomang/v1/variants/blind-magnifier.png',0,current_timestamp,current_timestamp),
((select id from character_pack_versions where version='v1' and pack_id=(select id from character_packs where pack_key='pomang')),'hidden-letter','HIDDEN','character-packs/pomang/v1/variants/hidden-letter.png',0,current_timestamp,current_timestamp),
((select id from character_pack_versions where version='v1' and pack_id=(select id from character_packs where pack_key='pomang')),'unknown-teary','UNKNOWN','character-packs/pomang/v1/variants/unknown-teary.png',0,current_timestamp,current_timestamp);
update surveys set character_pack_key='pomang', character_pack_version='v1' where character_pack_key is null or character_pack_version is null;
alter table surveys modify character_pack_key varchar(80) not null;
alter table surveys modify character_pack_version varchar(40) not null;
