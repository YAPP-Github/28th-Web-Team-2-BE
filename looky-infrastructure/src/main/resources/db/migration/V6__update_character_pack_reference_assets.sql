update character_pack_versions cpv
join character_packs cp on cp.id = cpv.pack_id
set cpv.base_asset_key = 'character-packs/pomang/qa-20260625/base/base.png'
where cp.pack_key = 'pomang'
  and cpv.version = 'v1';

update character_pack_variants cpv
join character_pack_versions ver on ver.id = cpv.version_id
join character_packs cp on cp.id = ver.pack_id
set cpv.variant_key = 'blind-magnifier',
    cpv.asset_key = 'character-packs/pomang/qa-20260625/variants/blind-magnifier.png',
    cpv.sort_order = 0
where cp.pack_key = 'pomang'
  and ver.version = 'v1'
  and cpv.quadrant_type = 'BLIND';

update character_pack_variants cpv
join character_pack_versions ver on ver.id = cpv.version_id
join character_packs cp on cp.id = ver.pack_id
set cpv.variant_key = 'hidden-letter',
    cpv.asset_key = 'character-packs/pomang/qa-20260625/variants/hidden-letter.png',
    cpv.sort_order = 1
where cp.pack_key = 'pomang'
  and ver.version = 'v1'
  and cpv.quadrant_type = 'HIDDEN';

update character_pack_variants cpv
join character_pack_versions ver on ver.id = cpv.version_id
join character_packs cp on cp.id = ver.pack_id
set cpv.variant_key = 'open-stars',
    cpv.asset_key = 'character-packs/pomang/qa-20260625/variants/open-stars.png',
    cpv.sort_order = 2
where cp.pack_key = 'pomang'
  and ver.version = 'v1'
  and cpv.quadrant_type = 'OPEN';

update character_pack_variants cpv
join character_pack_versions ver on ver.id = cpv.version_id
join character_packs cp on cp.id = ver.pack_id
set cpv.variant_key = 'unknown-clock',
    cpv.asset_key = 'character-packs/pomang/qa-20260625/variants/unknown-clock.png',
    cpv.sort_order = 3
where cp.pack_key = 'pomang'
  and ver.version = 'v1'
  and cpv.quadrant_type = 'UNKNOWN';
