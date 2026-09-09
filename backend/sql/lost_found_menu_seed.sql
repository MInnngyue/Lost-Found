-- Lost-Found management menu seed.
-- Run this after the RuoYi base tables and lost_found_v2 tables exist.
-- This script does not grant permissions to ordinary roles automatically.

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 200, '失物招领', 0, 5, 'lostfound', NULL, '', '', 1, 0, 'M', '0', '0', '',
       'guide', 'admin', SYSDATE(), '', NULL, '失物招领业务目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 200);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 201, '分类管理', 200, 1, 'category', 'lostfound/category/index', '', 'LostFoundCategory',
       1, 0, 'C', '0', '0', 'lostfound:category:list', 'tree', 'admin', SYSDATE(), '', NULL,
       '失物招领分类和地点管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 201);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT 202, '帖子审核', 200, 2, 'post', 'lostfound/post/index', '', 'LostFoundPost',
       1, 0, 'C', '0', '0', 'lostfound:post:list', 'message', 'admin', SYSDATE(), '', NULL,
       '失物招领帖子审核'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 202);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by,
     create_time, update_by, update_time, remark)
SELECT menu_id, menu_name, parent_id, order_num, '', '', '', '', 1, 0, 'F', '0', '0',
       perms, '#', 'admin', SYSDATE(), '', NULL, ''
FROM (
    SELECT 1200 menu_id, '分类查询' menu_name, 201 parent_id, 1 order_num, 'lostfound:category:query' perms
    UNION ALL SELECT 1201, '分类新增', 201, 2, 'lostfound:category:add'
    UNION ALL SELECT 1202, '分类修改', 201, 3, 'lostfound:category:edit'
    UNION ALL SELECT 1203, '分类删除', 201, 4, 'lostfound:category:remove'
    UNION ALL SELECT 1210, '帖子查询', 202, 1, 'lostfound:post:query'
    UNION ALL SELECT 1211, '帖子新增', 202, 2, 'lostfound:post:add'
    UNION ALL SELECT 1212, '帖子修改', 202, 3, 'lostfound:post:edit'
    UNION ALL SELECT 1213, '帖子发布', 202, 4, 'lostfound:post:publish'
    UNION ALL SELECT 1214, '帖子审核', 202, 5, 'lostfound:post:review'
    UNION ALL SELECT 1215, '我的帖子', 202, 6, 'lostfound:post:mine'
) menu_seed
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE sys_menu.menu_id = menu_seed.menu_id);
