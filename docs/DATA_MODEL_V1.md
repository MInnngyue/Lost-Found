# 校园失物招领系统数据模型 v1

## 1. 设计目标

本模型服务于《业务需求与状态流转规范 v1》，目标是：

- 使用 `sys_user` 作为统一用户身份来源。
- 将帖子、匹配、认领、举报、违规、申诉和审计拆成独立业务对象。
- 保留状态变更历史，避免只依赖当前状态字段。
- 支持逻辑删除、幂等处理、分页查询和审计追踪。
- 不把用户信用分作为业务约束。
- 不通过数据库物理外键强耦合若依系统表，统一由业务层校验关联关系。

## 2. 数据库约定

### 2.1 表命名

所有失物招领业务表使用 `lf_` 前缀。若依平台表继续使用 `sys_` 前缀。

### 2.2 主键

- 主键统一使用 `BIGINT` 自增 ID。
- 接口中使用主键定位资源，但每次操作都必须进行对象级权限校验。
- 对外展示可增加不可枚举的公开编号，避免直接暴露连续自增 ID。

### 2.3 用户关联

业务表中的 `user_id`、`operator_id`、`reporter_id` 等字段逻辑关联 `sys_user.user_id`。

初版不创建跨模块物理外键，原因：

- 若依系统表使用逻辑删除和平台级用户管理。
- 业务模块需要支持用户禁用、归档和数据留存。
- 物理外键会增加初始化、迁移和测试数据库的耦合。

业务层必须校验用户是否存在、是否可用以及是否有权操作目标资源。

### 2.4 通用字段

需要审计的业务表优先包含：

```text
create_by
create_time
update_by
update_time
remark
del_flag
```

状态历史表和审计表以追加为主，不使用逻辑删除。

## 3. 表总览

### 3.1 核心业务表

| 表 | 责任 |
|---|---|
| `lf_post` | 寻物或招领帖子 |
| `lf_post_image` | 帖子图片和图片审核信息 |
| `lf_post_review` | 帖子审核记录 |
| `lf_post_event` | 帖子状态变化历史 |
| `lf_match` | 寻物与招领之间的候选匹配 |
| `lf_claim` | 认领申请 |
| `lf_claim_event` | 认领状态变化历史 |
| `lf_comment` | 帖子评论 |
| `lf_message` | 系统通知 |
| `lf_private_message` | 用户私信 |

### 3.2 治理和安全表

| 表 | 责任 |
|---|---|
| `lf_report` | 用户举报 |
| `lf_violation_case` | 违规案件 |
| `lf_violation_action` | 处罚动作历史 |
| `lf_appeal` | 用户申诉 |
| `lf_audit_event` | 管理员和关键业务操作审计 |

### 3.3 配置和分类表

| 表 | 责任 |
|---|---|
| `lf_category` | 物品分类、颜色、校区、区域和地点 |

简单枚举仍使用若依 `sys_dict_type` 和 `sys_dict_data`，层级明显且需要业务管理的分类使用 `lf_category`。

## 4. `lf_post` 帖子表

### 4.1 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `post_id` | BIGINT | 主键 |
| `publisher_id` | BIGINT | 发布用户，逻辑关联 `sys_user` |
| `post_type` | VARCHAR(16) | `LOST` 或 `FOUND` |
| `lifecycle_status` | VARCHAR(32) | 帖子生命周期状态 |
| `review_status` | VARCHAR(32) | 审核状态 |
| `title` | VARCHAR(128) | 标题 |
| `description` | TEXT | 描述 |
| `item_category_id` | BIGINT | 物品分类 |
| `color_category_id` | BIGINT | 颜色分类 |
| `campus_category_id` | BIGINT | 校区 |
| `area_category_id` | BIGINT | 区域 |
| `location_detail` | VARCHAR(255) | 具体地点 |
| `event_date` | DATE | 遗失或拾取日期 |
| `expire_time` | DATETIME | 过期时间 |
| `view_count` | INT | 浏览次数 |
| `version` | INT | 乐观锁版本 |
| `create_by` | VARCHAR(64) | 创建者 |
| `create_time` | DATETIME | 创建时间 |
| `update_by` | VARCHAR(64) | 修改者 |
| `update_time` | DATETIME | 修改时间 |
| `remark` | VARCHAR(500) | 备注 |
| `del_flag` | CHAR(1) | 逻辑删除 |

### 4.2 状态

`lifecycle_status`：

```text
DRAFT
PENDING_REVIEW
PUBLISHED
RESOLVED
EXPIRED
WITHDRAWN
REJECTED
REMOVED
```

`review_status`：

```text
PENDING
APPROVED
REJECTED
```

### 4.3 索引

```text
(lifecycle_status, review_status, create_time)
(post_type, lifecycle_status, event_date)
(publisher_id, lifecycle_status, create_time)
(item_category_id, campus_category_id, event_date)
```

帖子公开列表查询必须同时限定：

```text
lifecycle_status = 'PUBLISHED'
review_status = 'APPROVED'
del_flag = '0'
```

已解决帖子不再进入公开列表，但允许通过详情接口查看，用于保留认领完成后的业务记录。

## 5. `lf_post_image` 帖子图片表

| 字段 | 类型 | 说明 |
|---|---|---|
| `image_id` | BIGINT | 主键 |
| `post_id` | BIGINT | 帖子 ID |
| `object_key` | VARCHAR(512) | 文件存储键 |
| `access_url` | VARCHAR(512) | 访问地址或相对路径 |
| `sha256` | CHAR(64) | 文件摘要，用于去重 |
| `sort_order` | INT | 展示顺序 |
| `review_status` | VARCHAR(32) | `PENDING`、`APPROVED`、`REJECTED` |
| `file_size` | BIGINT | 文件大小 |
| `mime_type` | VARCHAR(128) | 实际 MIME 类型 |
| `create_by` | VARCHAR(64) | 上传者 |
| `create_time` | DATETIME | 上传时间 |
| `del_flag` | CHAR(1) | 逻辑删除 |

图片不能直接使用用户提供的文件名作为存储文件名，上传目录必须与可执行代码目录隔离。

## 6. `lf_post_review` 帖子审核记录

| 字段 | 类型 | 说明 |
|---|---|---|
| `review_id` | BIGINT | 主键 |
| `post_id` | BIGINT | 帖子 ID |
| `reviewer_id` | BIGINT | 审核人 |
| `action` | VARCHAR(32) | `APPROVE`、`REJECT`、`REMOVE` |
| `before_status` | VARCHAR(32) | 处理前状态 |
| `after_status` | VARCHAR(32) | 处理后状态 |
| `reason` | VARCHAR(500) | 原因 |
| `create_time` | DATETIME | 处理时间 |

审核记录只追加，不允许管理员修改历史记录。

## 7. `lf_post_event` 帖子状态历史

| 字段 | 类型 | 说明 |
|---|---|---|
| `event_id` | BIGINT | 主键 |
| `post_id` | BIGINT | 帖子 ID |
| `event_type` | VARCHAR(32) | 事件类型 |
| `from_status` | VARCHAR(32) | 原状态 |
| `to_status` | VARCHAR(32) | 新状态 |
| `operator_id` | BIGINT | 操作人 |
| `source` | VARCHAR(32) | `USER`、`ADMIN`、`SYSTEM` |
| `reason` | VARCHAR(500) | 原因 |
| `request_id` | VARCHAR(64) | 请求追踪 ID |
| `create_time` | DATETIME | 发生时间 |

## 8. `lf_match` 匹配表

| 字段 | 类型 | 说明 |
|---|---|---|
| `match_id` | BIGINT | 主键 |
| `lost_post_id` | BIGINT | 寻物帖子 |
| `found_post_id` | BIGINT | 招领帖子 |
| `status` | VARCHAR(32) | 匹配状态 |
| `total_score` | INT | 总分 |
| `item_score` | INT | 分类得分 |
| `color_score` | INT | 颜色得分 |
| `location_score` | INT | 地点得分 |
| `time_score` | INT | 时间得分 |
| `source` | VARCHAR(32) | `AUTO` 或 `MANUAL` |
| `expire_time` | DATETIME | 匹配失效时间 |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 修改时间 |

匹配状态：

```text
SUGGESTED
VIEWED
CONFIRMED
IGNORED
EXPIRED
```

约束：

- `lost_post_id` 和 `found_post_id` 不能相同。
- 一对帖子只能存在一条有效匹配关系。
- 只有双方帖子均已审核通过且处于 `PUBLISHED` 时才可自动匹配。
- 匹配不能直接修改帖子为“已解决”。

## 9. `lf_claim` 认领申请表

| 字段 | 类型 | 说明 |
|---|---|---|
| `claim_id` | BIGINT | 主键 |
| `post_id` | BIGINT | 被认领帖子 |
| `applicant_id` | BIGINT | 申请人 |
| `post_owner_id` | BIGINT | 帖子发布者 |
| `match_id` | BIGINT | 可选的匹配来源 |
| `status` | VARCHAR(32) | 认领状态 |
| `evidence_text` | VARCHAR(2000) | 申请说明或验证信息 |
| `owner_remark` | VARCHAR(1000) | 发布者处理意见 |
| `applicant_confirmed` | CHAR(1) | 申请人是否确认交接 |
| `owner_confirmed` | CHAR(1) | 发布者是否确认交接 |
| `accepted_time` | DATETIME | 接受时间 |
| `completed_time` | DATETIME | 完成时间 |
| `create_by` | VARCHAR(64) | 创建者 |
| `create_time` | DATETIME | 创建时间 |
| `update_by` | VARCHAR(64) | 修改者 |
| `update_time` | DATETIME | 修改时间 |
| `version` | INT | 乐观锁版本 |
| `del_flag` | CHAR(1) | 逻辑删除 |

认领状态：

```text
PENDING
ACCEPTED
REJECTED
CANCELLED
HANDOFF
COMPLETED
DISPUTED
```

有效申请的唯一性建议由业务层保证：

```text
同一 applicant_id + post_id 只能有一条未结束申请
```

## 10. `lf_claim_event` 认领状态历史

| 字段 | 类型 | 说明 |
|---|---|---|
| `event_id` | BIGINT | 主键 |
| `claim_id` | BIGINT | 认领申请 |
| `event_type` | VARCHAR(32) | 事件类型 |
| `from_status` | VARCHAR(32) | 原状态 |
| `to_status` | VARCHAR(32) | 新状态 |
| `operator_id` | BIGINT | 操作人 |
| `reason` | VARCHAR(1000) | 原因 |
| `request_id` | VARCHAR(64) | 请求追踪 ID |
| `create_time` | DATETIME | 发生时间 |

## 11. `lf_comment` 评论表

| 字段 | 类型 | 说明 |
|---|---|---|
| `comment_id` | BIGINT | 主键 |
| `post_id` | BIGINT | 帖子 ID |
| `user_id` | BIGINT | 评论用户 |
| `parent_id` | BIGINT | 父评论，顶级评论为 0 |
| `content` | VARCHAR(1000) | 内容 |
| `content_status` | VARCHAR(32) | `PENDING`、`VISIBLE`、`REMOVED` |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 修改时间 |
| `del_flag` | CHAR(1) | 逻辑删除 |

评论必须进行长度限制、频率限制和举报处理。

## 12. `lf_message` 系统通知表

| 字段 | 类型 | 说明 |
|---|---|---|
| `message_id` | BIGINT | 主键 |
| `from_user_id` | BIGINT | 发送者，系统消息可为 0 |
| `to_user_id` | BIGINT | 接收者 |
| `message_type` | VARCHAR(32) | 通知类型 |
| `title` | VARCHAR(255) | 标题 |
| `content` | VARCHAR(2000) | 内容 |
| `related_type` | VARCHAR(32) | 关联对象类型 |
| `related_id` | BIGINT | 关联对象 ID |
| `read_flag` | CHAR(1) | `0` 未读、`1` 已读 |
| `create_time` | DATETIME | 创建时间 |
| `del_flag` | CHAR(1) | 逻辑删除 |

## 13. `lf_private_message` 私信表

| 字段 | 类型 | 说明 |
|---|---|---|
| `message_id` | BIGINT | 主键 |
| `conversation_key` | VARCHAR(128) | 会话键 |
| `from_user_id` | BIGINT | 发送者 |
| `to_user_id` | BIGINT | 接收者 |
| `content` | VARCHAR(2000) | 内容 |
| `content_status` | VARCHAR(32) | `VISIBLE`、`BLOCKED`、`REMOVED` |
| `read_flag` | CHAR(1) | `0` 未读、`1` 已读 |
| `create_time` | DATETIME | 创建时间 |
| `del_flag` | CHAR(1) | 逻辑删除 |

私信查询必须同时校验当前用户是发送者或接收者。

## 14. `lf_category` 分类表

| 字段 | 类型 | 说明 |
|---|---|---|
| `category_id` | BIGINT | 主键 |
| `category_type` | VARCHAR(32) | `ITEM`、`COLOR`、`CAMPUS`、`AREA`、`LOCATION` |
| `parent_id` | BIGINT | 父节点 |
| `category_code` | VARCHAR(64) | 稳定编码 |
| `category_name` | VARCHAR(128) | 展示名称 |
| `sort_order` | INT | 排序 |
| `status` | CHAR(1) | `0` 正常、`1` 停用 |
| `create_by` | VARCHAR(64) | 创建者 |
| `create_time` | DATETIME | 创建时间 |
| `update_by` | VARCHAR(64) | 修改者 |
| `update_time` | DATETIME | 修改时间 |
| `del_flag` | CHAR(1) | 逻辑删除 |

分类不直接删除已有业务引用，停用后不能用于新帖子，但历史帖子仍可显示。

## 15. `lf_report` 举报表

| 字段 | 类型 | 说明 |
|---|---|---|
| `report_id` | BIGINT | 主键 |
| `reporter_id` | BIGINT | 举报人 |
| `target_type` | VARCHAR(32) | `POST`、`COMMENT`、`MESSAGE`、`USER`、`CLAIM` |
| `target_id` | BIGINT | 被举报对象 |
| `reason_code` | VARCHAR(64) | 举报原因 |
| `description` | VARCHAR(2000) | 补充说明 |
| `status` | VARCHAR(32) | 举报处理状态 |
| `case_id` | BIGINT | 关联违规案件 |
| `handler_id` | BIGINT | 处理人 |
| `handle_result` | VARCHAR(2000) | 处理结果 |
| `handled_time` | DATETIME | 处理时间 |
| `create_time` | DATETIME | 举报时间 |

同一用户对同一对象的重复举报需要限流和合并，不能让重复点击制造大量案件。

## 16. `lf_violation_case` 违规案件表

| 字段 | 类型 | 说明 |
|---|---|---|
| `case_id` | BIGINT | 主键 |
| `subject_user_id` | BIGINT | 被处理用户 |
| `source_report_id` | BIGINT | 主要来源举报 |
| `violation_type` | VARCHAR(64) | 违规类型 |
| `severity` | VARCHAR(16) | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` |
| `status` | VARCHAR(32) | `OPEN`、`INVESTIGATING`、`RESOLVED`、`CLOSED` |
| `description` | VARCHAR(2000) | 案件说明 |
| `evidence` | JSON | 证据摘要，不保存不必要敏感数据 |
| `handler_id` | BIGINT | 处理人 |
| `resolved_time` | DATETIME | 解决时间 |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |

## 17. `lf_violation_action` 处罚动作表

| 字段 | 类型 | 说明 |
|---|---|---|
| `action_id` | BIGINT | 主键 |
| `case_id` | BIGINT | 违规案件 |
| `user_id` | BIGINT | 被处罚用户 |
| `action_type` | VARCHAR(64) | 警告、限制、封禁、内容下架等 |
| `scope` | VARCHAR(64) | `POST`、`CLAIM`、`MESSAGE`、`ALL` |
| `start_time` | DATETIME | 开始时间 |
| `end_time` | DATETIME | 结束时间 |
| `reason` | VARCHAR(2000) | 处罚原因 |
| `operator_id` | BIGINT | 操作人 |
| `create_time` | DATETIME | 创建时间 |

用户当前是否受限由有效处罚动作计算，不通过修改一个“信用分”字段实现。

## 18. `lf_appeal` 申诉表

| 字段 | 类型 | 说明 |
|---|---|---|
| `appeal_id` | BIGINT | 主键 |
| `case_id` | BIGINT | 违规案件 |
| `action_id` | BIGINT | 被申诉处罚 |
| `appellant_id` | BIGINT | 申诉用户 |
| `reason` | VARCHAR(3000) | 申诉理由 |
| `evidence` | JSON | 证据摘要 |
| `status` | VARCHAR(32) | `SUBMITTED`、`REVIEWING`、`APPROVED`、`REJECTED`、`WITHDRAWN` |
| `reviewer_id` | BIGINT | 审核人 |
| `review_result` | VARCHAR(2000) | 审核结果 |
| `review_time` | DATETIME | 审核时间 |
| `create_time` | DATETIME | 创建时间 |

## 19. `lf_audit_event` 审计事件表

| 字段 | 类型 | 说明 |
|---|---|---|
| `audit_id` | BIGINT | 主键 |
| `actor_id` | BIGINT | 操作人 |
| `actor_type` | VARCHAR(32) | `USER`、`ADMIN`、`SYSTEM` |
| `event_type` | VARCHAR(64) | 事件类型 |
| `target_type` | VARCHAR(64) | 目标类型 |
| `target_id` | BIGINT | 目标 ID |
| `before_data` | JSON | 变更前摘要 |
| `after_data` | JSON | 变更后摘要 |
| `request_id` | VARCHAR(64) | 请求追踪 ID |
| `client_ip` | VARCHAR(64) | 客户端 IP |
| `user_agent` | VARCHAR(512) | 客户端信息 |
| `create_time` | DATETIME | 事件时间 |

审计日志不能记录密码、JWT、完整私信正文或其他不必要的敏感内容。

## 20. 现有表迁移映射

| 旧表 | 新设计 |
|---|---|
| `lf_post` | 保留名称，增加生命周期、版本和分类 ID |
| `lf_post_image` | 增加文件安全和审核字段 |
| `lf_claim` | 重构状态，增加证据、接受和交接字段 |
| `lf_match_record` | 改为 `lf_match` |
| `lf_comment` | 增加内容状态 |
| `lf_message` | 统一关联对象字段 |
| `lf_private_message` | 增加会话键和内容状态 |
| `lf_credit_log` | 不再作为信用分核心模型，迁移为历史数据或废弃 |
| 旧 `category` | 改为 `lf_category` |
| 无对应表 | 新增审核、状态历史、举报、违规、处罚、申诉、审计表 |

## 21. 实施顺序

1. 创建新业务表，不删除旧表。
2. 先实现 `lf_category` 和 `lf_post`。
3. 完成帖子审核、公开查询和状态历史。
4. 实现 `lf_claim` 和认领状态历史。
5. 实现举报、违规案件、处罚和审计。
6. 实现通知、评论和私信。
7. 最后迁移自动匹配和统计。
8. 通过业务回归后，再归档旧表和旧代码。

## 22. 验收标准

- 任何公开帖子都能追溯到审核记录。
- 任何认领完成都能追溯到双方确认和状态历史。
- 任何用户限制都能追溯到违规案件、处罚动作和操作人。
- 管理员无法无记录地修改关键业务状态。
- 普通用户不能通过修改 ID 访问或修改他人的业务资源。
- 旧信用分字段不再参与发布、认领和权限判断。
- 现有 `ry-vue` 数据库可以继续运行，新增表通过独立迁移脚本创建。
