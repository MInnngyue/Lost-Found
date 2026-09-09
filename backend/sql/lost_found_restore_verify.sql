-- Read-only restore verification for the isolated Lost-Found database.
-- Run this file after selecting the restore-test database in Navicat.
-- It must not be executed against the active database with write privileges.

SELECT DATABASE() AS selected_database;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'sys_user',
    'sys_role',
    'sys_menu',
    'sys_role_menu',
    'lf_schema_version',
    'lf_category',
    'lf_post',
    'lf_post_image',
    'lf_post_review',
    'lf_post_event',
    'lf_match',
    'lf_claim',
    'lf_claim_event',
    'lf_comment',
    'lf_message',
    'lf_private_message',
    'lf_report',
    'lf_violation_case',
    'lf_violation_action',
    'lf_appeal',
    'lf_audit_event'
  )
ORDER BY table_name;

SELECT 'sys_user' AS table_name, COUNT(*) AS row_count FROM sys_user
UNION ALL
SELECT 'sys_role', COUNT(*) FROM sys_role
UNION ALL
SELECT 'sys_menu', COUNT(*) FROM sys_menu
UNION ALL
SELECT 'sys_role_menu', COUNT(*) FROM sys_role_menu
UNION ALL
SELECT 'lf_schema_version', COUNT(*) FROM lf_schema_version
UNION ALL
SELECT 'lf_category', COUNT(*) FROM lf_category
UNION ALL
SELECT 'lf_post', COUNT(*) FROM lf_post
UNION ALL
SELECT 'lf_post_image', COUNT(*) FROM lf_post_image
UNION ALL
SELECT 'lf_post_review', COUNT(*) FROM lf_post_review
UNION ALL
SELECT 'lf_post_event', COUNT(*) FROM lf_post_event
UNION ALL
SELECT 'lf_match', COUNT(*) FROM lf_match
UNION ALL
SELECT 'lf_claim', COUNT(*) FROM lf_claim
UNION ALL
SELECT 'lf_claim_event', COUNT(*) FROM lf_claim_event
UNION ALL
SELECT 'lf_comment', COUNT(*) FROM lf_comment
UNION ALL
SELECT 'lf_message', COUNT(*) FROM lf_message
UNION ALL
SELECT 'lf_private_message', COUNT(*) FROM lf_private_message
UNION ALL
SELECT 'lf_report', COUNT(*) FROM lf_report
UNION ALL
SELECT 'lf_violation_case', COUNT(*) FROM lf_violation_case
UNION ALL
SELECT 'lf_violation_action', COUNT(*) FROM lf_violation_action
UNION ALL
SELECT 'lf_appeal', COUNT(*) FROM lf_appeal
UNION ALL
SELECT 'lf_audit_event', COUNT(*) FROM lf_audit_event;

SELECT version_code, description, applied_time
FROM lf_schema_version
ORDER BY version_id;

SELECT category_type, COUNT(*) AS category_count
FROM lf_category
WHERE del_flag = '0'
GROUP BY category_type
ORDER BY category_type;

SELECT post_id, publisher_id, post_type, lifecycle_status, review_status,
       title, event_date, create_time
FROM lf_post
ORDER BY post_id;

SELECT image_id, post_id, object_key, sha256, file_size, mime_type,
       review_status, create_time
FROM lf_post_image
ORDER BY post_id, image_id;

SELECT role_key, role_name, status, del_flag
FROM sys_role
WHERE role_key IN (
    'common',
    'lostfound_reviewer',
    'lostfound_operator',
    'admin'
)
ORDER BY role_key;
