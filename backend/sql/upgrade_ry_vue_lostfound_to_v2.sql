-- Upgrade the isolated RuoYi clone from the old empty lf_* schema to v2.
-- Target database: ry-vue-lostfound
-- Verified on 2026-09-05: conflicting v1 tables contain 0 rows.
-- This script keeps renamed v1 tables as an archive and does not drop data.

USE `ry-vue-lostfound`;

RENAME TABLE
    `lf_post` TO `legacy_lf_post_v1_20260905`,
    `lf_post_image` TO `legacy_lf_post_image_v1_20260905`,
    `lf_claim` TO `legacy_lf_claim_v1_20260905`,
    `lf_match_record` TO `legacy_lf_match_record_v1_20260905`,
    `lf_comment` TO `legacy_lf_comment_v1_20260905`,
    `lf_message` TO `legacy_lf_message_v1_20260905`,
    `lf_private_message` TO `legacy_lf_private_message_v1_20260905`,
    `lf_credit_log` TO `legacy_lf_credit_log_v1_20260905`;

-- Recreate missing v2 tables. Existing v2 tables are preserved by IF NOT EXISTS.
SOURCE lost_found_v2_fresh.sql;
