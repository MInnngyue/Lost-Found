-- Complete the category-management permissions of the Lost-Found operator role.
-- Execute only in ry-vue-lostfound after the role and menu seed scripts.
-- Idempotent: existing role assignments are preserved.

USE `ry-vue-lostfound`;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_info.role_id, menu_info.menu_id
FROM sys_role role_info
JOIN sys_menu menu_info
  ON menu_info.perms IN (
      'lostfound:category:add',
      'lostfound:category:edit',
      'lostfound:category:remove'
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
VALUES ('LF_ROLE_MATRIX_FIX_V1', '补齐运营管理员分类管理权限')
ON DUPLICATE KEY UPDATE description = VALUES(description);

SELECT r.role_key, m.perms
FROM sys_role r
JOIN sys_role_menu rm ON rm.role_id = r.role_id
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE r.role_key = 'lostfound_operator'
  AND m.perms IN (
      'lostfound:category:list',
      'lostfound:category:query',
      'lostfound:category:add',
      'lostfound:category:edit',
      'lostfound:category:remove'
  )
ORDER BY m.perms;
