-- Claim and matching management permissions for the isolated Lost-Found database.
-- Idempotent: safe to run after lost_found_menu_seed.sql.
-- This script changes menu metadata and grants only the common role permissions.

USE `ry-vue-lostfound`;

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 203, '认领申请', 200, 3, 'claim', 'lostfound/claim/index', '', 'LostFoundClaim',
       1, 0, 'C', '0', '0', 'lostfound:claim:list', 'check', 'admin', SYSDATE(), '', NULL,
       '认领申请处理和状态追踪'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 203);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 204, '匹配建议', 200, 4, 'match', 'lostfound/match/index', '', 'LostFoundMatch',
       1, 0, 'C', '0', '0', 'lostfound:match:list', 'connection', 'admin', SYSDATE(), '', NULL,
       '寻物和招领帖子匹配建议'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 204);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT menu_id, menu_name, parent_id, order_num, '', '', '', '', 1, 0, 'F', '0', '0',
       perms, '#', 'admin', SYSDATE(), '', NULL, ''
FROM (
    SELECT 1220 menu_id, '认领查询' menu_name, 203 parent_id, 1 order_num, 'lostfound:claim:query' perms
    UNION ALL SELECT 1221, '认领新增', 203, 2, 'lostfound:claim:add'
    UNION ALL SELECT 1222, '认领修改', 203, 3, 'lostfound:claim:edit'
    UNION ALL SELECT 1230, '匹配查询', 204, 1, 'lostfound:match:query'
) menu_seed
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE sys_menu.menu_id = menu_seed.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_info.role_id, menu_info.menu_id
FROM sys_role role_info
JOIN sys_menu menu_info
  ON menu_info.perms IN (
      'lostfound:claim:add',
      'lostfound:claim:query',
      'lostfound:claim:edit',
      'lostfound:match:query'
  )
WHERE role_info.role_key = 'common'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existing
      WHERE existing.role_id = role_info.role_id
        AND existing.menu_id = menu_info.menu_id
  );
