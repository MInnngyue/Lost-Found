-- Lost Found business schema v1
-- Depends on the RuoYi system schema, especially sys_user.

CREATE TABLE IF NOT EXISTS lf_post (
    post_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子主键',
    user_id BIGINT NOT NULL COMMENT '发布用户，关联 sys_user.user_id',
    post_type CHAR(1) NOT NULL COMMENT '0寻物启事 1失物招领',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    item_category VARCHAR(64) NOT NULL COMMENT '物品分类',
    color VARCHAR(32) DEFAULT NULL COMMENT '颜色',
    campus VARCHAR(64) DEFAULT NULL COMMENT '校区',
    location_area VARCHAR(64) DEFAULT NULL COMMENT '区域',
    location_detail VARCHAR(128) DEFAULT NULL COMMENT '具体地点',
    event_date DATE DEFAULT NULL COMMENT '遗失或捡到日期',
    description TEXT COMMENT '详细描述',
    status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0进行中 1已匹配 2认领中 3已完结 4已过期 5已下架',
    review_status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0待审核 1已通过 2已拒绝',
    review_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (post_id),
    KEY idx_lf_post_user (user_id),
    KEY idx_lf_post_status (post_type, status, review_status),
    KEY idx_lf_post_category (item_category),
    KEY idx_lf_post_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领帖子';

CREATE TABLE IF NOT EXISTS lf_post_image (
    image_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (image_id),
    KEY idx_lf_post_image_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子图片';

CREATE TABLE IF NOT EXISTS lf_claim (
    claim_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    claim_user_id BIGINT NOT NULL,
    post_owner_id BIGINT NOT NULL,
    match_id BIGINT DEFAULT NULL,
    status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0待确认 1部分确认 2已完成 3已取消',
    owner_confirmed CHAR(1) NOT NULL DEFAULT '0',
    claimer_confirmed CHAR(1) NOT NULL DEFAULT '0',
    remark VARCHAR(500) DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (claim_id),
    KEY idx_lf_claim_post (post_id),
    KEY idx_lf_claim_user (claim_user_id, post_owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认领记录';

CREATE TABLE IF NOT EXISTS lf_match_record (
    match_id BIGINT NOT NULL AUTO_INCREMENT,
    lost_post_id BIGINT NOT NULL,
    found_post_id BIGINT NOT NULL,
    total_score INT NOT NULL DEFAULT 0,
    item_score INT NOT NULL DEFAULT 0,
    color_score INT NOT NULL DEFAULT 0,
    location_score INT NOT NULL DEFAULT 0,
    time_score INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id),
    UNIQUE KEY uk_lf_match_pair (lost_post_id, found_post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子匹配记录';

CREATE TABLE IF NOT EXISTS lf_comment (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    content VARCHAR(1000) NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (comment_id),
    KEY idx_lf_comment_post (post_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子评论';

CREATE TABLE IF NOT EXISTS lf_message (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    from_user_id BIGINT NOT NULL DEFAULT 0 COMMENT '0表示系统消息',
    to_user_id BIGINT NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) DEFAULT NULL,
    content VARCHAR(2000) DEFAULT NULL,
    related_post_id BIGINT DEFAULT NULL,
    related_claim_id BIGINT DEFAULT NULL,
    read_flag CHAR(1) NOT NULL DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (message_id),
    KEY idx_lf_message_user_read (to_user_id, read_flag, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息';

CREATE TABLE IF NOT EXISTS lf_private_message (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    read_flag CHAR(1) NOT NULL DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (message_id),
    KEY idx_lf_private_conversation (from_user_id, to_user_id, create_time),
    KEY idx_lf_private_to_user (to_user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户私信';

CREATE TABLE IF NOT EXISTS lf_credit_log (
    credit_log_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    change_value INT NOT NULL,
    balance_value INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    related_claim_id BIGINT DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (credit_log_id),
    KEY idx_lf_credit_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分变更记录';
