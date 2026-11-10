-- ============================================================
-- 校享 CampusShare 数据库初始化脚本
-- 适用：MySQL 8.0/8.4；数据库名 campusshare；InnoDB + utf8mb4
-- 说明：本脚本只创建结构，演示数据见 002_demo_data.sql。
--       已被团队应用过的脚本不要静默重写，后续变化新建 003_*.sql。
-- ============================================================

CREATE DATABASE IF NOT EXISTS campusshare
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE campusshare;

-- 1. 登录账号与角色
CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    role VARCHAR(32) NOT NULL COMMENT 'STUDENT / TEACHER / RESOURCE_ADMIN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_app_user_account (account)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录账号和角色';

-- 2. 最小可预约单位
CREATE TABLE IF NOT EXISTS resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    resource_type VARCHAR(32) NOT NULL COMMENT 'TABLE_TENNIS / BADMINTON / TENNIS / LIBRARY_SEAT / DISCUSSION_ROOM / CLASSROOM',
    unit_label VARCHAR(50) NOT NULL COMMENT '座位号、场地号等最小单位标识',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资源（桌、场、座位、房间）';

-- 3. 开放安排
CREATE TABLE IF NOT EXISTS resource_opening (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    KEY ix_opening_resource_time (resource_id, start_at, end_at),
    CONSTRAINT fk_opening_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '允许预约的开放安排';

-- 4. 教学占用、停用等不可约安排
CREATE TABLE IF NOT EXISTS resource_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    block_type VARCHAR(32) NOT NULL COMMENT 'TEACHING / DISABLED',
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    KEY ix_block_resource_time (resource_id, start_at, end_at),
    CONSTRAINT fk_block_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教学占用和停用安排';

-- 5. 本人预约及当前状态
CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    client_request_id VARCHAR(100) NOT NULL,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    accepted_at DATETIME(3) NOT NULL COMMENT '服务端确认预约成功的时间，计时基准',
    arrival_deadline DATETIME(3) NOT NULL COMMENT '座位 accepted_at+30 分钟；体育 start_at+15 分钟',
    status VARCHAR(32) NOT NULL COMMENT 'RESERVED / CHECKED_IN / COMPLETED / CANCELLED',
    cancel_reason VARCHAR(32) NULL COMMENT 'USER_CANCEL / NO_SHOW / RESOURCE_DISABLED',
    check_in_at DATETIME(3) NULL,
    check_out_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uq_reservation_request (user_id, client_request_id),
    KEY ix_reservation_resource_time (resource_id, start_at, end_at),
    KEY ix_reservation_user_time (user_id, start_at),
    KEY ix_reservation_expiry (status, accepted_at, id),
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_reservation_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预约记录';

-- 6. 预约操作与处理历史
CREATE TABLE IF NOT EXISTS reservation_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL COMMENT 'CREATED / CANCELLED / NO_SHOW / RESOURCE_DISABLED',
    operator_id BIGINT NULL COMMENT 'NULL 表示系统任务',
    occurred_at DATETIME(3) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    KEY ix_event_reservation (reservation_id, occurred_at),
    CONSTRAINT fk_event_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_event_operator FOREIGN KEY (operator_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预约历史';

-- 7. 违规记录
CREATE TABLE IF NOT EXISTS violation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    violation_type VARCHAR(32) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    UNIQUE KEY uq_violation_reservation_type (reservation_id, violation_type),
    KEY ix_violation_user (user_id, occurred_at),
    CONSTRAINT fk_violation_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_violation_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '违规记录';
