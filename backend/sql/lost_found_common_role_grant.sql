-- Grant the minimum lost-found permissions to RuoYi's common role.
-- This script is idempotent and removes the broad system-menu grants that
-- RuoYi's base SQL assigns to the common role.

USE `ry-vue-lostfound`;

DELETE existing
FROM sys_role_menu existing
JOIN sys_role role_info ON role_info.role_id = existing.role_id
JOIN sys_menu menu_info ON menu_info.menu_id = existing.menu_id
WHERE role_info.role_key = 'common'
  AND role_info.del_flag = '0'
  AND menu_info.perms NOT LIKE 'lostfound:%'
  AND menu_info.perms <> '';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_info.role_id, menu_info.menu_id
FROM sys_role role_info
JOIN sys_menu menu_info
  ON menu_info.perms IN (
      'lostfound:post:add',
      'lostfound:post:edit',
      'lostfound:post:publish',
      'lostfound:post:mine',
      'lostfound:comment:add',
      'lostfound:claim:add',
      'lostfound:claim:query',
      'lostfound:claim:edit',
      'lostfound:match:query',
      'lostfound:message:query',
      'lostfound:message:add',
      'lostfound:message:edit',
      'lostfound:report:add',
      'lostfound:appeal:query',
      'lostfound:appeal:add',
      'lostfound:action:query'
  )
WHERE role_info.role_key = 'common'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existing
      WHERE existing.role_id = role_info.role_id
        AND existing.menu_id = menu_info.menu_id
  );
