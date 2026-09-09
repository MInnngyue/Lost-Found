-- Lost-Found role matrix for the isolated database.
-- Run after lost_found_menu_seed.sql, lost_found_claim_match_seed.sql,
-- lost_found_interaction_seed.sql and lost_found_moderation_seed.sql.
-- Idempotent: existing users, roles and role assignments are preserved.

USE `ry-vue-lostfound`;

INSERT INTO sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly,
     dept_check_strictly, status, del_flag, create_by, create_time, update_by,
     update_time, remark)
SELECT '失物招领审核员', 'lostfound_reviewer', 10, '1', 1, 1, '0', '0',
       'admin', SYSDATE(), '', NULL, '仅负责帖子审核和匹配查看'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_key = 'lostfound_reviewer' AND del_flag = '0'
);

INSERT INTO sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly,
     dept_check_strictly, status, del_flag, create_by, create_time, update_by,
     update_time, remark)
SELECT '失物招领运营管理员', 'lostfound_operator', 11, '1', 1, 1, '0', '0',
       'admin', SYSDATE(), '', NULL, '负责举报、案件、处罚和申诉处理'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_key = 'lostfound_operator' AND del_flag = '0'
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_info.role_id, menu_info.menu_id
FROM sys_role role_info
JOIN sys_menu menu_info
  ON menu_info.perms IN (
      'lostfound:post:list',
      'lostfound:post:query',
      'lostfound:post:review',
      'lostfound:match:list',
      'lostfound:match:query'
  )
WHERE role_info.role_key = 'lostfound_reviewer'
  AND role_info.del_flag = '0'
  AND menu_info.status = '0'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existing
      WHERE existing.role_id = role_info.role_id
        AND existing.menu_id = menu_info.menu_id
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_info.role_id, menu_info.menu_id
FROM sys_role role_info
JOIN sys_menu menu_info
  ON menu_info.perms IN (
      'lostfound:category:list',
      'lostfound:category:query',
      'lostfound:post:list',
      'lostfound:post:query',
      'lostfound:post:review',
      'lostfound:claim:list',
      'lostfound:claim:query',
      'lostfound:match:list',
      'lostfound:match:query',
      'lostfound:report:list',
      'lostfound:report:handle',
      'lostfound:case:list',
      'lostfound:case:handle',
      'lostfound:action:list',
      'lostfound:appeal:list',
      'lostfound:appeal:handle'
  )
WHERE role_info.role_key = 'lostfound_operator'
  AND role_info.del_flag = '0'
  AND menu_info.status = '0'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existing
      WHERE existing.role_id = role_info.role_id
        AND existing.menu_id = menu_info.menu_id
  );

INSERT INTO lf_schema_version (version_code, description)
VALUES ('LF_ROLE_MATRIX_V1', '审核员和运营管理员角色权限矩阵')
ON DUPLICATE KEY UPDATE description = VALUES(description);

SELECT role_key, role_name, status
FROM sys_role
WHERE role_key IN ('common', 'lostfound_reviewer', 'lostfound_operator', 'admin')
ORDER BY role_sort, role_id;
