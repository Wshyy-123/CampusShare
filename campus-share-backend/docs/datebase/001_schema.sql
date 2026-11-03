-- ==========================================
-- 校享 CampusShare 数据库初始化脚本
-- 文件名：001_schema.sql
-- ==========================================

CREATE DATABASE IF NOT EXISTS campusshare DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE campusshare;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS app_user (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        account VARCHAR(50) UNIQUE NOT NULL COMMENT '登录账号',
    display_name VARCHAR(50) COMMENT '显示名称',
    password_hash VARCHAR(100) COMMENT '密码摘要',
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 资源表（座位、体育场地等）
CREATE TABLE IF NOT EXISTS resource (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        name VARCHAR(100) NOT NULL COMMENT '资源名称',
    location VARCHAR(100) COMMENT '位置',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型：LIBRARY_SEAT, BADMINTON 等',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源表';

-- 3. 预约表（核心业务表）
CREATE TABLE IF NOT EXISTS reservation (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           user_id BIGINT NOT NULL COMMENT '预约用户ID',
                                           resource_id BIGINT NOT NULL COMMENT '关联资源ID',
                                           accepted_at DATETIME(3) NOT NULL COMMENT '预约成功时间(XIA-10用于计算30分钟超时)',
    start_at DATETIME(3) COMMENT '预约开始使用时间(XIA-9用于计算15分钟体育超时)',
    end_at DATETIME(3) COMMENT '预约结束时间',
    status VARCHAR(50) NOT NULL DEFAULT 'RESERVED' COMMENT '状态: RESERVED, CHECKED_IN, CANCELLED, COMPLETED',
    check_in_at DATETIME(3) COMMENT '实际到馆签到时间',
    cancel_reason VARCHAR(50) COMMENT '取消原因: USER_CANCEL, NO_SHOW 等',
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- 4. 违规记录表（XIA-9 体育超时未到产生）
CREATE TABLE IF NOT EXISTS violation_record (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                user_id BIGINT NOT NULL COMMENT '违规用户ID',
                                                reservation_id BIGINT NOT NULL COMMENT '关联的预约ID',
                                                violation_type VARCHAR(50) NOT NULL COMMENT '违规类型，如 SPORTS_NO_SHOW',
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规记录表';