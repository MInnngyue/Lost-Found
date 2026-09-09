-- Ordinary-user permissions for comments, claims, matching and messaging.
-- Idempotent. Run after the Lost-Found menu/permission seed scripts.

USE `ry-vue-lostfound`;

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT menu_id, menu_name, 200, order_num, '', '', '', '', 1, 0, 'F', '1', '0',
       perms, '#', 'admin', SYSDATE(), '', NULL, '校园用户业务接口权限'
FROM (
    SELECT 1240 menu_id, '评论新增' menu_name, 1 order_num, 'lostfound:comment:add' perms
    UNION ALL SELECT 1241, '认领新增', 2, 'lostfound:claim:add'
    UNION ALL SELECT 1242, '认领查询', 3, 'lostfound:claim:query'
    UNION ALL SELECT 1243, '认领修改', 4, 'lostfound:claim:edit'
    UNION ALL SELECT 1244, '匹配查询', 5, 'lostfound:match:query'
    UNION ALL SELECT 1245, '消息查询', 6, 'lostfound:message:query'
    UNION ALL SELECT 1246, '消息新增', 7, 'lostfound:message:add'
    UNION ALL SELECT 1247, '消息修改', 8, 'lostfound:message:edit'
) menu_seed
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE sys_menu.menu_id = menu_seed.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_info.role_id, menu_info.menu_id
FROM sys_role role_info
JOIN sys_menu menu_info
  ON menu_info.perms IN (
      'lostfound:comment:add',
      'lostfound:claim:add',
      'lostfound:claim:query',
      'lostfound:claim:edit',
      'lostfound:match:query',
      'lostfound:message:query',
      'lostfound:message:add',
      'lostfound:message:edit'
  )
WHERE role_info.role_key = 'common'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu existing
      WHERE existing.role_id = role_info.role_id
        AND existing.menu_id = menu_info.menu_id
  );
