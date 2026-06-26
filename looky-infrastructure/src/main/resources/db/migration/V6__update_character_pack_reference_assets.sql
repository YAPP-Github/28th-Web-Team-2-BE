update character_pack_versions
set base_asset_key = 'character-packs/pomang/qa-20260625/base/base.png'
where version = 'v1'
  and pack_id in (
    select id
    from character_packs
    where pack_key = 'pomang'
  );

update character_pack_variants
set variant_key = 'blind-magnifier',
    asset_key = 'character-packs/pomang/qa-20260625/variants/blind-magnifier.png',
    sort_order = 0
where quadrant_type = 'BLIND'
  and version_id in (
    select ver.id
    from character_pack_versions ver
    join character_packs cp on cp.id = ver.pack_id
    where cp.pack_key = 'pomang'
      and ver.version = 'v1'
  );

update character_pack_variants
set variant_key = 'hidden-letter',
    asset_key = 'character-packs/pomang/qa-20260625/variants/hidden-letter.png',
    sort_order = 1
where quadrant_type = 'HIDDEN'
  and version_id in (
    select ver.id
    from character_pack_versions ver
    join character_packs cp on cp.id = ver.pack_id
    where cp.pack_key = 'pomang'
      and ver.version = 'v1'
  );

update character_pack_variants
set variant_key = 'open-stars',
    asset_key = 'character-packs/pomang/qa-20260625/variants/open-stars.png',
    sort_order = 2
where quadrant_type = 'OPEN'
  and version_id in (
    select ver.id
    from character_pack_versions ver
    join character_packs cp on cp.id = ver.pack_id
    where cp.pack_key = 'pomang'
      and ver.version = 'v1'
  );

update character_pack_variants
set variant_key = 'unknown-clock',
    asset_key = 'character-packs/pomang/qa-20260625/variants/unknown-clock.png',
    sort_order = 3
where quadrant_type = 'UNKNOWN'
  and version_id in (
    select ver.id
    from character_pack_versions ver
    join character_packs cp on cp.id = ver.pack_id
    where cp.pack_key = 'pomang'
      and ver.version = 'v1'
  );
