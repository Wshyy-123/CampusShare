-- 校享 CampusShare 数据库初始化脚本 V1
-- 适用：MySQL 8.4 LTS，字符集 utf8mb4
-- 执行方式：mysql -u root -p < V1__create_schema.sql

CREATE DATABASE IF NOT EXISTS campus_share
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE campus_share;

CREATE TABLE IF NOT EXISTS app_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(64)  NOT NULL,
    role          VARCHAR(32)  NOT NULL COMMENT 'STUDENT / TEACHER / RESOURCE_ADMIN',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '平台登录账号';

CREATE TABLE IF NOT EXISTS venue_resource (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    location     VARCHAR(200) NOT NULL,
    type         VARCHAR(32)  NOT NULL COMMENT 'PING_PONG_TABLE / BADMINTON_COURT / TENNIS_COURT / LIBRARY_SEAT / DISCUSSION_ROOM / CLASSROOM',
    booking_unit VARCHAR(32)  NOT NULL COMMENT 'TABLE / COURT / SEAT / ROOM',
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_venue_resource_type (type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '可预约场地、座位与房间';
