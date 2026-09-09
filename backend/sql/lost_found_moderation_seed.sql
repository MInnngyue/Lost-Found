-- Moderation permissions for the isolated Lost-Found database.
-- Run after lost_found_menu_seed.sql. Idempotent.

USE `ry-vue-lostfound`;

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 205, '举报处理', 200, 5, 'report', 'lostfound/report/index', '', 'LostFoundReport',
       1, 0, 'C', '0', '0', 'lostfound:report:list', 'warning', 'admin', SYSDATE(), '', NULL,
       '用户举报受理和驳回'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 205);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 206, '违规案件', 200, 6, 'case', 'lostfound/case/index', '', 'LostFoundCase',
       1, 0, 'C', '0', '0', 'lostfound:case:list', 'documentation', 'admin', SYSDATE(), '', NULL,
       '违规案件处理和结案'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 206);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 207, '处罚记录', 200, 7, 'action', 'lostfound/action/index', '', 'LostFoundAction',
       1, 0, 'C', '0', '0', 'lostfound:action:list', 'lock', 'admin', SYSDATE(), '', NULL,
       '用户处罚动作记录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 207);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 208, '申诉审核', 200, 8, 'appeal', 'lostfound/appeal/index', '', 'LostFoundAppeal',
       1, 0, 'C', '0', '0', 'lostfound:appeal:list', 'edit', 'admin', SYSDATE(), '', NULL,
       '用户处罚申诉审核'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 208);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT menu_id, menu_name, parent_id, order_num, '', '', '', '', 1, 0, 'F', '0', '0',
       perms, '#', 'admin', SYSDATE(), '', NULL, ''
FROM (
    SELECT 1250 menu_id, '举报查询' menu_name, 205 parent_id, 1 order_num, 'lostfound:report:list' perms
    UNION ALL SELECT 1251, '举报处理', 205, 2, 'lostfound:report:handle'
    UNION ALL SELECT 1260, '案件查询', 206, 1, 'lostfound:case:list'
    UNION ALL SELECT 1261, '案件处理', 206, 2, 'lostfound:case:handle'
    UNION ALL SELECT 1270, '处罚查询', 207, 1, 'lostfound:action:list'
    UNION ALL SELECT 1271, '申诉查询', 208, 1, 'lostfound:appeal:list'
    UNION ALL SELECT 1272, '申诉处理', 208, 2, 'lostfound:appeal:handle'
    UNION ALL SELECT 1280, '用户举报', 200, 90, 'lostfound:report:add'
    UNION ALL SELECT 1281, '用户申诉查询', 200, 91, 'lostfound:appeal:query'
    UNION ALL SELECT 1282, '用户申诉新增', 200, 92, 'lostfound:appeal:add'
    UNION ALL SELECT 1283, '用户处罚查询', 200, 93, 'lostfound:action:query'
) menu_seed
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE sys_menu.menu_id = menu_seed.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_info.role_id, menu_info.menu_id
FROM sys_role role_info
JOIN sys_menu menu_info
  ON menu_info.perms IN (
      'lostfound:report:add',
      'lostfound:appeal:query',
      'lostfound:appeal:add',
      'lostfound:action:query'
  )
WHERE role_info.role_key = 'common'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu existing
      WHERE existing.role_id = role_info.role_id AND existing.menu_id = menu_info.menu_id
  );
