-- ============================================================
-- 校享 CampusShare 迁移 003：XIA-5/XIA-6 预约核心结构与 V1.2 字段
-- 适用：已应用 001_schema.sql、002_demo_data.sql 的 campusshare 数据库。
-- 不重写 001/002；本脚本只做新增/扩展。
-- ============================================================

USE campusshare;

-- 1. 账号角色与启用状态（XIA-1 演示登录所需）
ALTER TABLE app_user
    ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'STUDENT' AFTER password_hash,
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE AFTER role;

-- 2. 最小预约单位标识（座位号、场地号、教室号）
ALTER TABLE resource
    ADD COLUMN unit_label VARCHAR(50) NOT NULL DEFAULT '' AFTER resource_type;

-- 3. 预约幂等键、到馆期限、离场时间
ALTER TABLE reservation
    ADD COLUMN client_request_id VARCHAR(100) NULL AFTER resource_id,
    ADD COLUMN arrival_deadline DATETIME(3) NULL AFTER accepted_at,
    ADD COLUMN check_out_at DATETIME(3) NULL AFTER check_in_at;

-- 兼容 002 中的旧演示预约：补齐非空字段，不删除历史
UPDATE reservation
SET start_at = accepted_at
WHERE start_at IS NULL;

UPDATE reservation
SET end_at = DATE_ADD(start_at, INTERVAL 1 HOUR)
WHERE end_at IS NULL;

UPDATE reservation
SET client_request_id = CONCAT('legacy-', id)
WHERE client_request_id IS NULL;

UPDATE reservation r
JOIN resource s ON s.id = r.resource_id
SET r.arrival_deadline = CASE
    WHEN s.resource_type = 'LIBRARY_SEAT' THEN DATE_ADD(r.accepted_at, INTERVAL 30 MINUTE)
    ELSE DATE_ADD(r.start_at, INTERVAL 15 MINUTE)
END
WHERE r.arrival_deadline IS NULL;

ALTER TABLE reservation
    MODIFY COLUMN start_at DATETIME(3) NOT NULL,
    MODIFY COLUMN end_at DATETIME(3) NOT NULL,
    MODIFY COLUMN client_request_id VARCHAR(100) NOT NULL,
    MODIFY COLUMN arrival_deadline DATETIME(3) NOT NULL,
    ADD UNIQUE KEY uq_reservation_request (user_id, client_request_id),
    ADD KEY ix_reservation_resource_time (resource_id, start_at, end_at),
    ADD KEY ix_reservation_user_time (user_id, start_at),
    ADD KEY ix_reservation_expiry (status, accepted_at, id);

-- 4. 违规记录时间与幂等约束
ALTER TABLE violation_record
    ADD COLUMN occurred_at DATETIME(3) NULL AFTER violation_type;

UPDATE violation_record
SET occurred_at = created_at
WHERE occurred_at IS NULL;

ALTER TABLE violation_record
    MODIFY COLUMN occurred_at DATETIME(3) NOT NULL,
    ADD UNIQUE KEY uq_violation_reservation_type (reservation_id, violation_type);

-- 5. 开放安排
CREATE TABLE IF NOT EXISTS resource_opening (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    KEY ix_opening_resource_time (resource_id, start_at, end_at),
    CONSTRAINT fk_opening_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '允许预约的开放安排';

-- 6. 模拟教务/体育占用、停用等不可约安排
CREATE TABLE IF NOT EXISTS resource_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    block_type VARCHAR(32) NOT NULL COMMENT 'TEACHING / EXTERNAL / DISABLED',
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    KEY ix_block_resource_time (resource_id, start_at, end_at),
    CONSTRAINT fk_block_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教学/外部占用和停用安排';

-- 7. 预约操作与处理历史
CREATE TABLE IF NOT EXISTS reservation_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL COMMENT 'CREATED / CANCELLED / COMPLETED / NO_SHOW / RESOURCE_DISABLED',
    operator_id BIGINT NULL COMMENT 'NULL 表示系统任务',
    occurred_at DATETIME(3) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    KEY ix_event_reservation (reservation_id, occurred_at),
    CONSTRAINT fk_event_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_event_operator FOREIGN KEY (operator_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预约历史';
