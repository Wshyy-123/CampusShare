-- ==========================================
-- 校享 CampusShare 变更脚本
-- 文件名：003_sports_attendance.sql
-- 适用：XIA-7 体育预约取消、XIA-8 体育扫码签到签退
-- 说明：在已应用 001、002 的数据库上顺序执行一次；不要重复执行
-- ==========================================

USE campusshare;

-- 1. 签退时间（XIA-8 记录使用结束时间）
ALTER TABLE reservation
    ADD COLUMN check_out_at DATETIME(3) NULL COMMENT '实际签退时间' AFTER check_in_at;

-- 2. 预约操作历史（取消、签到、签退、超时处理可追溯操作者与原因）
CREATE TABLE IF NOT EXISTS reservation_event
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT      NOT NULL COMMENT '关联预约ID',
    event_type     VARCHAR(32) NOT NULL COMMENT 'CANCEL / CHECK_IN / CHECK_OUT / NO_SHOW',
    operator_id    BIGINT      NULL COMMENT '操作者用户ID；后台任务为NULL',
    occurred_at    DATETIME(3) NOT NULL COMMENT '服务端发生时间',
    request_id     VARCHAR(64) NULL COMMENT '接口追踪号；后台任务为NULL',
    reason         VARCHAR(50) NULL COMMENT 'USER_CANCEL / NO_SHOW 等',
    KEY idx_reservation_event_reservation (reservation_id, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='预约操作历史';

-- 3. 查询索引：我的预约、按资源查时段
CREATE INDEX idx_reservation_user_status ON reservation (user_id, status);
CREATE INDEX idx_reservation_resource_time ON reservation (resource_id, start_at, end_at);

-- 4. 同一预约同类违规只记一次：后台重跑不重复记违规
ALTER TABLE violation_record
    ADD CONSTRAINT uk_violation_reservation_type UNIQUE (reservation_id, violation_type);
