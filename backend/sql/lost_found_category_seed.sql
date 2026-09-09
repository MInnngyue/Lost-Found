-- Development data only. Do not run against the original ry-vue database.
-- Run once in Navicat with "stop on error" enabled; rerunning is idempotent.
-- Existing codes, names, disabled entries and user-created records are preserved.
-- This script inserts categories only; it never resets users, passwords or posts.
SET NAMES utf8mb4;
USE `ry-vue-lostfound`;

START TRANSACTION;

INSERT INTO `ry-vue-lostfound`.lf_category
    (category_type, parent_id, category_code, category_name, sort_order, status, create_by, update_by)
SELECT seed.category_type, 0, seed.category_code, seed.category_name, seed.sort_order, '0',
       'category_seed', 'category_seed'
FROM (
    SELECT 'ITEM' AS category_type, 'ELECTRONICS' AS category_code, '电子设备' AS category_name, 10 AS sort_order
    UNION ALL SELECT 'ITEM', 'DOCUMENTS', '证件卡片', 20
    UNION ALL SELECT 'ITEM', 'KEYS', '钥匙', 30
    UNION ALL SELECT 'ITEM', 'BAGS', '书包箱包', 40
    UNION ALL SELECT 'ITEM', 'BOOKS', '书籍文具', 50
    UNION ALL SELECT 'ITEM', 'CLOTHING', '衣物饰品', 60
    UNION ALL SELECT 'ITEM', 'DAILY_ITEMS', '生活用品', 70
    UNION ALL SELECT 'ITEM', 'OTHER', '其他物品', 99
    UNION ALL SELECT 'COLOR', 'BLACK', '黑色', 10
    UNION ALL SELECT 'COLOR', 'WHITE', '白色', 20
    UNION ALL SELECT 'COLOR', 'GRAY', '灰色', 30
    UNION ALL SELECT 'COLOR', 'RED', '红色', 40
    UNION ALL SELECT 'COLOR', 'BLUE', '蓝色', 50
    UNION ALL SELECT 'COLOR', 'GREEN', '绿色', 60
    UNION ALL SELECT 'COLOR', 'YELLOW', '黄色', 70
    UNION ALL SELECT 'COLOR', 'PINK', '粉色', 80
    UNION ALL SELECT 'COLOR', 'PURPLE', '紫色', 90
    UNION ALL SELECT 'COLOR', 'MULTICOLOR', '多色', 100
    UNION ALL SELECT 'COLOR', 'UNKNOWN', '颜色不详', 110
) seed
LEFT JOIN `ry-vue-lostfound`.lf_category existing
  ON existing.category_type = seed.category_type
 AND (existing.category_code = seed.category_code
      OR (existing.category_name = seed.category_name AND existing.del_flag = '0'))
WHERE existing.category_id IS NULL;

-- The location below is deliberately named "test"; it is not real campus data.
-- Do not introduce a test campus when an enabled real campus already exists.
INSERT INTO `ry-vue-lostfound`.lf_category
    (category_type, parent_id, category_code, category_name, sort_order, status, create_by, update_by)
SELECT 'CAMPUS', 0, 'TEST_CAMPUS', '测试校区（非真实校区）', 999, '0', 'category_seed', 'category_seed'
FROM (SELECT 1 AS seed) seed
LEFT JOIN `ry-vue-lostfound`.lf_category existing
  ON existing.category_type = 'CAMPUS'
 AND (existing.category_code = 'TEST_CAMPUS' OR (existing.status = '0' AND existing.del_flag = '0'))
WHERE existing.category_id IS NULL;

INSERT INTO `ry-vue-lostfound`.lf_category
    (category_type, parent_id, category_code, category_name, sort_order, status, create_by, update_by)
SELECT 'AREA', campus.category_id, seed.category_code, seed.category_name, seed.sort_order, '0',
       'category_seed', 'category_seed'
FROM `ry-vue-lostfound`.lf_category campus
CROSS JOIN (
    SELECT 'TEST_TEACHING' AS category_code, '测试教学区' AS category_name, 10 AS sort_order
    UNION ALL SELECT 'TEST_LIVING', '测试生活区', 20
) seed
LEFT JOIN `ry-vue-lostfound`.lf_category existing
  ON existing.category_type = 'AREA' AND existing.category_code = seed.category_code
WHERE campus.category_type = 'CAMPUS' AND campus.category_code = 'TEST_CAMPUS'
  AND campus.status = '0' AND campus.del_flag = '0' AND existing.category_id IS NULL;

INSERT INTO `ry-vue-lostfound`.lf_category
    (category_type, parent_id, category_code, category_name, sort_order, status, create_by, update_by)
SELECT 'LOCATION', area.category_id, seed.category_code, seed.category_name, 10, '0',
       'category_seed', 'category_seed'
FROM (
    SELECT 'TEST_TEACHING' AS area_code, 'TEST_LIBRARY' AS category_code, '测试图书馆入口' AS category_name
    UNION ALL SELECT 'TEST_LIVING', 'TEST_DORMITORY', '测试宿舍值班室'
) seed
JOIN `ry-vue-lostfound`.lf_category area
  ON area.category_type = 'AREA' AND area.category_code = seed.area_code
 AND area.status = '0' AND area.del_flag = '0'
JOIN `ry-vue-lostfound`.lf_category campus
  ON campus.category_id = area.parent_id AND campus.category_type = 'CAMPUS'
 AND campus.category_code = 'TEST_CAMPUS' AND campus.status = '0' AND campus.del_flag = '0'
LEFT JOIN `ry-vue-lostfound`.lf_category existing
  ON existing.category_type = 'LOCATION' AND existing.category_code = seed.category_code
WHERE existing.category_id IS NULL;

COMMIT;

-- ITEM, COLOR and CAMPUS must each have enabled_count > 0.
SELECT expected.category_type, COUNT(category.category_id) AS enabled_count
FROM (
    SELECT 'ITEM' AS category_type
    UNION ALL SELECT 'COLOR'
    UNION ALL SELECT 'CAMPUS'
    UNION ALL SELECT 'AREA'
    UNION ALL SELECT 'LOCATION'
) expected
LEFT JOIN `ry-vue-lostfound`.lf_category category
  ON category.category_type = expected.category_type
 AND category.status = '0' AND category.del_flag = '0'
GROUP BY expected.category_type;

-- Confirm parent-child relationships; no passwords or private user data are read.
SELECT child.category_type, child.category_name, parent.category_name AS parent_name
FROM `ry-vue-lostfound`.lf_category child
LEFT JOIN `ry-vue-lostfound`.lf_category parent ON parent.category_id = child.parent_id
WHERE child.create_by = 'category_seed' AND child.del_flag = '0'
ORDER BY child.category_type, child.sort_order, child.category_id;
