-- Lost-Found business schema v2
-- Purpose: initialize a NEW empty business schema.
-- Warning: do not run this script against a database containing v1 business data.
-- The migration from v1 is intentionally separate and must be backed up first.

CREATE TABLE IF NOT EXISTS lf_schema_version (
    version_id BIGINT NOT NULL AUTO_INCREMENT,
    version_code VARCHAR(32) NOT NULL,
    description VARCHAR(255) NOT NULL,
    applied_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version_id),
    UNIQUE KEY uk_lf_schema_version_code (version_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领业务模式版本';

CREATE TABLE IF NOT EXISTS lf_category (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_type VARCHAR(32) NOT NULL COMMENT 'ITEM/COLOR/CAMPUS/AREA/LOCATION',
    parent_id BIGINT NOT NULL DEFAULT 0,
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_lf_category_code (category_type, category_code),
    KEY idx_lf_category_parent (category_type, parent_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领分类和地点';

CREATE TABLE IF NOT EXISTS lf_post (
    post_id BIGINT NOT NULL AUTO_INCREMENT,
    publisher_id BIGINT NOT NULL,
    post_type VARCHAR(16) NOT NULL COMMENT 'LOST寻物 FOUND招领',
    lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    title VARCHAR(128) NOT NULL,
    description TEXT,
    item_category_id BIGINT DEFAULT NULL,
    color_category_id BIGINT DEFAULT NULL,
    campus_category_id BIGINT DEFAULT NULL,
    area_category_id BIGINT DEFAULT NULL,
    location_detail VARCHAR(255) DEFAULT NULL,
    event_date DATE DEFAULT NULL,
    expire_time DATETIME DEFAULT NULL,
    view_count INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    remark VARCHAR(500) DEFAULT NULL,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (post_id),
    KEY idx_lf_post_public (lifecycle_status, review_status, create_time),
    KEY idx_lf_post_type_date (post_type, lifecycle_status, event_date),
    KEY idx_lf_post_publisher (publisher_id, lifecycle_status, create_time),
    KEY idx_lf_post_category (item_category_id, campus_category_id, event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领帖子';

CREATE TABLE IF NOT EXISTS lf_post_image (
    image_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    access_url VARCHAR(512) NOT NULL,
    sha256 CHAR(64) DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(128) NOT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (image_id),
    KEY idx_lf_post_image_post (post_id, del_flag, sort_order),
    KEY idx_lf_post_image_hash (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子图片';

CREATE TABLE IF NOT EXISTS lf_post_review (
    review_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL COMMENT 'APPROVE/REJECT/REMOVE',
    before_status VARCHAR(32) NOT NULL,
    after_status VARCHAR(32) NOT NULL,
    reason VARCHAR(500) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    KEY idx_lf_post_review_post (post_id, create_time),
    KEY idx_lf_post_review_reviewer (reviewer_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子审核记录';

CREATE TABLE IF NOT EXISTS lf_post_event (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    from_status VARCHAR(32) DEFAULT NULL,
    to_status VARCHAR(32) NOT NULL,
    operator_id BIGINT DEFAULT NULL,
    source VARCHAR(32) NOT NULL COMMENT 'USER/ADMIN/SYSTEM',
    reason VARCHAR(500) DEFAULT NULL,
    request_id VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    KEY idx_lf_post_event_post (post_id, create_time),
    KEY idx_lf_post_event_operator (operator_id, create_time),
    KEY idx_lf_post_event_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子状态历史';

CREATE TABLE IF NOT EXISTS lf_match (
    match_id BIGINT NOT NULL AUTO_INCREMENT,
    lost_post_id BIGINT NOT NULL,
    found_post_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUGGESTED',
    total_score INT NOT NULL DEFAULT 0,
    item_score INT NOT NULL DEFAULT 0,
    color_score INT NOT NULL DEFAULT 0,
    location_score INT NOT NULL DEFAULT 0,
    time_score INT NOT NULL DEFAULT 0,
    source VARCHAR(32) NOT NULL DEFAULT 'AUTO',
    expire_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id),
    UNIQUE KEY uk_lf_match_pair (lost_post_id, found_post_id),
    KEY idx_lf_match_lost (lost_post_id, status, create_time),
    KEY idx_lf_match_found (found_post_id, status, create_time),
    KEY idx_lf_match_score (status, total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寻物招领匹配关系';

CREATE TABLE IF NOT EXISTS lf_claim (
    claim_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    post_owner_id BIGINT NOT NULL,
    match_id BIGINT DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    evidence_text VARCHAR(2000) DEFAULT NULL,
    owner_remark VARCHAR(1000) DEFAULT NULL,
    applicant_confirmed CHAR(1) NOT NULL DEFAULT '0',
    owner_confirmed CHAR(1) NOT NULL DEFAULT '0',
    accepted_time DATETIME DEFAULT NULL,
    completed_time DATETIME DEFAULT NULL,
    create_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (claim_id),
    KEY idx_lf_claim_post (post_id, status, create_time),
    KEY idx_lf_claim_applicant (applicant_id, status, create_time),
    KEY idx_lf_claim_owner (post_owner_id, status, create_time),
    KEY idx_lf_claim_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认领申请';

CREATE TABLE IF NOT EXISTS lf_claim_event (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    claim_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    from_status VARCHAR(32) DEFAULT NULL,
    to_status VARCHAR(32) NOT NULL,
    operator_id BIGINT DEFAULT NULL,
    reason VARCHAR(1000) DEFAULT NULL,
    request_id VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    KEY idx_lf_claim_event_claim (claim_id, create_time),
    KEY idx_lf_claim_event_operator (operator_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认领状态历史';

CREATE TABLE IF NOT EXISTS lf_comment (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    content VARCHAR(1000) NOT NULL,
    content_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (comment_id),
    KEY idx_lf_comment_post (post_id, content_status, create_time),
    KEY idx_lf_comment_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子评论';

CREATE TABLE IF NOT EXISTS lf_message (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    from_user_id BIGINT NOT NULL DEFAULT 0,
    to_user_id BIGINT NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) DEFAULT NULL,
    content VARCHAR(2000) DEFAULT NULL,
    related_type VARCHAR(32) DEFAULT NULL,
    related_id BIGINT DEFAULT NULL,
    read_flag CHAR(1) NOT NULL DEFAULT '0',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (message_id),
    KEY idx_lf_message_user_read (to_user_id, read_flag, create_time),
    KEY idx_lf_message_related (related_type, related_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知';

CREATE TABLE IF NOT EXISTS lf_private_message (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_key VARCHAR(128) NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    content_status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    read_flag CHAR(1) NOT NULL DEFAULT '0',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (message_id),
    KEY idx_lf_pm_conversation (conversation_key, create_time),
    KEY idx_lf_pm_receiver (to_user_id, read_flag, create_time),
    KEY idx_lf_pm_sender (from_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户私信';

CREATE TABLE IF NOT EXISTS lf_report (
    report_id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    description VARCHAR(2000) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    case_id BIGINT DEFAULT NULL,
    handler_id BIGINT DEFAULT NULL,
    handle_result VARCHAR(2000) DEFAULT NULL,
    handled_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    KEY idx_lf_report_status (status, create_time),
    KEY idx_lf_report_target (target_type, target_id, create_time),
    KEY idx_lf_report_reporter (reporter_id, create_time),
    KEY idx_lf_report_case (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户举报';

CREATE TABLE IF NOT EXISTS lf_violation_case (
    case_id BIGINT NOT NULL AUTO_INCREMENT,
    subject_user_id BIGINT NOT NULL,
    source_report_id BIGINT DEFAULT NULL,
    violation_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'LOW',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    description VARCHAR(2000) DEFAULT NULL,
    evidence JSON DEFAULT NULL,
    handler_id BIGINT DEFAULT NULL,
    resolved_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (case_id),
    KEY idx_lf_case_subject (subject_user_id, status, create_time),
    KEY idx_lf_case_status (status, severity, create_time),
    KEY idx_lf_case_report (source_report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规案件';

CREATE TABLE IF NOT EXISTS lf_violation_action (
    action_id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    scope VARCHAR(64) NOT NULL DEFAULT 'ALL',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME DEFAULT NULL,
    reason VARCHAR(2000) NOT NULL,
    operator_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (action_id),
    KEY idx_lf_action_user (user_id, scope, start_time, end_time),
    KEY idx_lf_action_case (case_id, create_time),
    KEY idx_lf_action_operator (operator_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处罚动作历史';

CREATE TABLE IF NOT EXISTS lf_appeal (
    appeal_id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    action_id BIGINT NOT NULL,
    appellant_id BIGINT NOT NULL,
    reason VARCHAR(3000) NOT NULL,
    evidence JSON DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    reviewer_id BIGINT DEFAULT NULL,
    review_result VARCHAR(2000) DEFAULT NULL,
    review_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (appeal_id),
    KEY idx_lf_appeal_user (appellant_id, status, create_time),
    KEY idx_lf_appeal_case (case_id, status, create_time),
    KEY idx_lf_appeal_reviewer (reviewer_id, review_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户申诉';

CREATE TABLE IF NOT EXISTS lf_audit_event (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT DEFAULT NULL,
    actor_type VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT DEFAULT NULL,
    before_data JSON DEFAULT NULL,
    after_data JSON DEFAULT NULL,
    request_id VARCHAR(64) DEFAULT NULL,
    client_ip VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(512) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_id),
    KEY idx_lf_audit_target (target_type, target_id, create_time),
    KEY idx_lf_audit_actor (actor_id, create_time),
    KEY idx_lf_audit_event (event_type, create_time),
    KEY idx_lf_audit_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务审计事件';

INSERT INTO lf_schema_version (version_code, description)
VALUES ('LF_V2_BASELINE', '业务需求与状态流转规范 v1 对应的数据模型')
ON DUPLICATE KEY UPDATE description = VALUES(description);
