# Lost-Found 旧数据到若依业务库迁移方案

## 1. 迁移目标

将原项目 `lost_found` 数据库中的有效业务数据迁移到若依数据库 `ry-vue`，同时完成：

- 旧 `user` 用户到若依 `sys_user` 的身份映射。
- 旧帖子、图片、认领、匹配、评论、通知和私信的业务迁移。
- 旧信用分字段停止参与新系统权限判断。
- 旧数字状态映射到 v2 字符串状态。
- 为历史状态补建审核记录、状态事件和审计记录。

迁移必须是一次性可回滚的离线操作，不直接在当前运行库上试错。

## 2. 数据来源确认

原项目备份：

```text
G:\VibeCodingFiles\Lost-Found\_data\mysql\lost_found.sql
```

原数据库名称：

```text
lost_found
```

原数据库包含：

```text
category
claim
comment
match_record
message
post
post_image
private_message
user
```

当前若依数据库包含：

```text
sys_user
sys_role
sys_menu
lf_post
lf_post_image
lf_claim
lf_match_record
lf_comment
lf_message
lf_private_message
lf_credit_log
```

注意：当前 `backend/sql/lost_found_v1.sql` 不是原项目完整备份，它只定义了若依业务前缀表，并且依赖 `sys_user`。原项目完整用户数据仍在 `_data/mysql/lost_found.sql` 中。

## 3. 迁移环境

迁移不得直接使用正在运行的 `ry-vue` 作为试验库。建议在 Navicat 中创建：

```text
lost_found_legacy_backup
ry_vue_migration_test
```

流程：

1. 将原始 `lost_found.sql` 导入 `lost_found_legacy_backup`。
2. 复制当前 `ry-vue` 为 `ry_vue_migration_test`。
3. 在 `ry_vue_migration_test` 上执行 v2 业务表脚本。
4. 在迁移测试库中完成数据迁移。
5. 通过校验和业务测试后，才考虑迁移正式开发库。

当前正在运行的 `ry-vue` 只用于验证若依平台启动，不作为迁移试验库。

## 4. 用户迁移

### 4.1 用户映射原则

旧用户表：

```text
user.id
user.username
user.password
user.nickname
user.avatar
user.phone
user.email
user.role
user.status
user.create_time
user.update_time
```

新用户表：

```text
sys_user.user_id
sys_user.user_name
sys_user.password
sys_user.nick_name
sys_user.avatar
sys_user.ph_num
sys_user.email
sys_user.status
```

迁移必须维护一张临时映射表：

```text
legacy_user_id
ruoyi_user_id
legacy_username
mapping_status
```

业务数据迁移期间，所有旧用户 ID 都通过该映射表转换，禁止假设新旧 ID 一定相同。

### 4.2 管理员映射

旧系统的 `role = 1` 不能直接写入 `sys_user` 的业务字段。

应当：

1. 将旧用户迁移为 `sys_user`。
2. 根据旧管理员身份选择若依管理员角色。
3. 写入 `sys_user_role`。
4. 普通用户不授予管理员角色。
5. 迁移后抽查管理员账号是否能登录并只能访问授权菜单。

### 4.3 密码处理

旧密码哈希算法和若依密码编码器必须核对。

若无法确认兼容：

- 不直接复制旧密码哈希。
- 使用一次性密码重置流程。
- 要求用户首次登录后修改密码。
- 禁止在迁移脚本或日志中输出明文密码。

## 5. 分类迁移

旧 `category` 到新 `lf_category`：

| 旧字段 | 新字段 |
|---|---|
| `id` | 不直接复用，建立旧 ID 映射 |
| `type` | `category_type` |
| `name` | `category_name` |
| `parent_id` | `parent_id`，经过 ID 映射 |
| `sort_order` | `sort_order` |
| `status` | `status` |
| `deleted` | `del_flag` |
| `create_time` | `create_time` |
| `update_time` | `update_time` |

旧分类类型映射：

```text
item_category -> ITEM
color         -> COLOR
location      -> CAMPUS / AREA / LOCATION
```

旧 `location` 类型需要根据父子层级拆分为新的分类层级，不能简单复制为平面数据。

## 6. 帖子迁移

旧 `post` 到新 `lf_post`：

| 旧字段 | 新字段 | 处理 |
|---|---|---|
| `id` | `post_id` | 建立旧帖子 ID 映射 |
| `user_id` | `publisher_id` | 通过用户映射转换 |
| `type = 0` | `post_type = LOST` | 状态转换 |
| `type = 1` | `post_type = FOUND` | 状态转换 |
| `item_category` | `item_category_id` | 按分类名称匹配 |
| `color` | `color_category_id` | 按分类名称匹配 |
| `location_campus` | `campus_category_id` | 按分类名称匹配 |
| `location_area` | `area_category_id` | 按分类名称匹配 |
| `location_detail` | `location_detail` | 保留文本 |
| `lost_time` | `event_date` | 直接迁移 |
| `title` | `title` | 直接迁移 |
| `description` | `description` | 直接迁移 |
| `view_count` | `view_count` | 直接迁移 |
| `create_time` | `create_time` | 直接迁移 |
| `update_time` | `update_time` | 直接迁移 |
| `deleted` | `del_flag` | 0/1 转换 |

### 6.1 帖子类型转换

```text
0 -> LOST
1 -> FOUND
```

### 6.2 审核状态转换

```text
review_status = 0 -> review_status = PENDING
review_status = 1 -> review_status = APPROVED
review_status = 2 -> review_status = REJECTED
```

### 6.3 生命周期状态转换

旧状态不能机械地与新状态一一对应，建议使用以下规则：

```text
deleted = 1                  -> REMOVED
status = 5                   -> REMOVED
status = 4                   -> EXPIRED
status = 3                   -> RESOLVED
review_status = 2            -> REJECTED
review_status = 0            -> PENDING_REVIEW
review_status = 1, status 0 -> PUBLISHED
review_status = 1, status 1 -> PUBLISHED
review_status = 1, status 2 -> PUBLISHED
```

旧的“已匹配”和“认领中”不再写入帖子生命周期，而是由 `lf_match` 和 `lf_claim` 表表达。

## 7. 图片迁移

旧 `post_image` 到新 `lf_post_image`：

| 旧字段 | 新字段 |
|---|---|
| `id` | 新生成 `image_id` |
| `post_id` | 转换为新 `post_id` |
| `image_url` | `access_url` |
| `sort_order` | `sort_order` |
| `deleted` | `del_flag` |
| `create_time` | `create_time` |

迁移时必须：

- 校验图片文件是否实际存在。
- 计算 SHA-256。
- 识别真实 MIME 类型。
- 不信任原始文件扩展名。
- 对不存在的图片写入迁移异常报告。

## 8. 匹配记录迁移

旧 `match_record` 到新 `lf_match`：

| 旧字段 | 新字段 |
|---|---|
| `lost_post_id` | 新帖子 ID |
| `found_post_id` | 新帖子 ID |
| `score` | `total_score` |
| `item_score` | `item_score` |
| `color_score` | `color_score` |
| `location_score` | `location_score` |
| `time_score` | `time_score` |
| `create_time` | `create_time` |

历史匹配记录统一迁移为：

```text
status = CONFIRMED
source = AUTO
```

但历史匹配不会自动把当前帖子标记为 `RESOLVED`，是否已经完成由认领记录决定。

## 9. 认领记录迁移

旧 `claim` 到新 `lf_claim`：

| 旧字段 | 新字段 |
|---|---|
| `id` | 新生成 `claim_id` |
| `post_id` | 新帖子 ID |
| `claim_user_id` | 新用户 ID |
| `post_owner_id` | 新用户 ID |
| `match_id` | 新匹配 ID |
| `owner_confirmed` | `owner_confirmed` |
| `claimer_confirmed` | `applicant_confirmed` |
| `remark` | `owner_remark` |
| `create_time` | `create_time` |
| `update_time` | `update_time` |

旧认领状态：

```text
0 -> PENDING
1 -> HANDOFF
2 -> COMPLETED
3 -> CANCELLED
```

对于旧状态为 2 的记录：

- `completed_time` 使用旧 `update_time`。
- 生成一条 `COMPLETED` 的 `lf_claim_event`。
- 生成一条历史审计事件。
- 不重新计算或补发信用分。

## 10. 评论、通知和私信迁移

### 10.1 评论

旧 `comment` 迁移到 `lf_comment`：

- 转换帖子 ID和用户 ID。
- `deleted = 1` 的记录迁移为 `REMOVED`。
- 其他历史内容迁移为 `VISIBLE`。
- 旧评论不自动进入待审核，保留历史数据并记录迁移来源。

### 10.2 系统通知

旧 `message` 迁移到 `lf_message`：

- `type` 转换为 `message_type`。
- `related_post_id` 转换为 `related_type = POST` 和 `related_id`。
- `related_match_id` 转换为 `related_type = MATCH` 和 `related_id`。
- `is_read` 转换为 `read_flag`。
- 通知正文保留，但不得在日志中输出。

### 10.3 私信

旧 `private_message` 迁移到 `lf_private_message`：

- 转换发送者和接收者 ID。
- 根据两个用户 ID 生成稳定的 `conversation_key`。
- `is_read` 转换为 `read_flag`。
- 历史内容迁移为 `VISIBLE`，后续新消息进入内容治理流程。

## 11. 信用分历史处理

旧用户的以下字段不迁移为新权限字段：

```text
credit_score
success_count
```

处理原则：

1. 不在 `sys_user` 中新增信用分字段。
2. 不根据旧信用分阻止发布或认领。
3. 不把历史分数直接转换为风险等级。
4. 若确有历史处罚证据，再人工创建违规案件。
5. 对旧数据保留迁移说明，不伪造新的处罚记录。

## 12. 迁移校验

迁移完成后至少执行以下校验：

### 12.1 数量校验

```text
旧用户总数 = 新用户映射成功数 + 明确失败数
旧帖子总数 = 新帖子数 + 明确失败数
旧图片总数 = 新图片数 + 文件缺失数
旧认领总数 = 新认领数
旧匹配总数 = 新匹配数
```

### 12.2 关联校验

- 每个新帖子都能关联一个有效 `sys_user`。
- 每张图片都能关联一个新帖子。
- 每条认领都能关联一个新帖子、申请人和发布者。
- 每条匹配都能关联一条寻物帖和一条招领帖。
- 每条通知和私信的接收者都存在。
- 分类父子关系没有环。

### 12.3 状态校验

- 公开帖子必须是 `PUBLISHED + APPROVED`。
- `RESOLVED` 帖子必须存在完成认领或人工解决依据。
- `COMPLETED` 认领必须有双方确认或历史完成证据。
- `REMOVED` 帖子必须存在审核或治理记录。

## 13. 迁移异常处理

迁移脚本不得遇到单条异常就静默跳过。应输出异常清单：

```text
migration_error_id
source_table
source_id
target_table
error_type
error_message
created_time
```

异常数据先进入隔离表或报告文件，不直接删除。

## 14. 执行顺序

1. 备份原 `lost_found` 数据库。
2. 创建 `lost_found_legacy_backup`。
3. 创建 `ry_vue_migration_test`。
4. 导入若依系统表。
5. 执行 `lost_found_v2_fresh.sql`。
6. 建立用户、分类、帖子 ID 映射。
7. 迁移用户和分类。
8. 迁移帖子和图片。
9. 迁移匹配、认领、评论、通知和私信。
10. 生成审核、状态历史和审计历史。
11. 执行数量、关联和状态校验。
12. 启动后端进行只读业务验证。
13. 通过回归测试后再允许写操作。

## 15. 当前结论

当前不应把原 `lost_found.sql` 直接导入 `ry-vue`，也不应把旧后端直接连接到若依数据库。

正确路线是：

```text
原 lost_found 备份
        ↓
迁移测试库
        ↓
用户和分类映射
        ↓
业务数据迁移
        ↓
状态和审计补建
        ↓
只读验证
        ↓
新若依业务模块接入
```
