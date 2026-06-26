update character_pack_versions cpv
set cpv.base_asset_key = 'character-packs/pomang/qa-20260625/base/base.png'
where cpv.version = 'v1'
  and exists (
      select 1
      from character_packs cp
      where cp.id = cpv.pack_id
        and cp.pack_key = 'pomang'
  );

update character_pack_variants cpv
set cpv.variant_key = 'blind-magnifier',
    cpv.asset_key = 'character-packs/pomang/qa-20260625/variants/blind-magnifier.png',
    cpv.sort_order = 0
where cpv.quadrant_type = 'BLIND'
  and exists (
      select 1
      from character_pack_versions ver
      join character_packs cp on cp.id = ver.pack_id
      where ver.id = cpv.version_id
        and cp.pack_key = 'pomang'
        and ver.version = 'v1'
  );

update character_pack_variants cpv
set cpv.variant_key = 'hidden-letter',
    cpv.asset_key = 'character-packs/pomang/qa-20260625/variants/hidden-letter.png',
    cpv.sort_order = 1
where cpv.quadrant_type = 'HIDDEN'
  and exists (
      select 1
      from character_pack_versions ver
      join character_packs cp on cp.id = ver.pack_id
      where ver.id = cpv.version_id
        and cp.pack_key = 'pomang'
        and ver.version = 'v1'
  );

update character_pack_variants cpv
set cpv.variant_key = 'open-stars',
    cpv.asset_key = 'character-packs/pomang/qa-20260625/variants/open-stars.png',
    cpv.sort_order = 2
where cpv.quadrant_type = 'OPEN'
  and exists (
      select 1
      from character_pack_versions ver
      join character_packs cp on cp.id = ver.pack_id
      where ver.id = cpv.version_id
        and cp.pack_key = 'pomang'
        and ver.version = 'v1'
  );

update character_pack_variants cpv
set cpv.variant_key = 'unknown-clock',
    cpv.asset_key = 'character-packs/pomang/qa-20260625/variants/unknown-clock.png',
    cpv.sort_order = 3
where cpv.quadrant_type = 'UNKNOWN'
  and exists (
      select 1
      from character_pack_versions ver
      join character_packs cp on cp.id = ver.pack_id
      where ver.id = cpv.version_id
        and cp.pack_key = 'pomang'
        and ver.version = 'v1'
  );
