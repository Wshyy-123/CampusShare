-- 隔离测试夹具：H2(MySQL 模式) 与 MySQL 8.4 均可执行。
-- 不创建/切换数据库，避免影响共享库；测试库由 CI 或本地环境准备。
DROP TABLE IF EXISTS violation_record;
DROP TABLE IF EXISTS reservation_event;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS resource_block;
DROP TABLE IF EXISTS resource_opening;
DROP TABLE IF EXISTS resource;
DROP TABLE IF EXISTS app_user;

CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    unit_label VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE resource_opening (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    start_at TIMESTAMP(3) NOT NULL,
    end_at TIMESTAMP(3) NOT NULL,
    note VARCHAR(200) NULL,
    CONSTRAINT fk_test_opening_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
);

CREATE TABLE resource_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    block_type VARCHAR(32) NOT NULL,
    start_at TIMESTAMP(3) NOT NULL,
    end_at TIMESTAMP(3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    CONSTRAINT fk_test_block_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
);

CREATE TABLE reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    client_request_id VARCHAR(100) NOT NULL,
    start_at TIMESTAMP(3) NOT NULL,
    end_at TIMESTAMP(3) NOT NULL,
    accepted_at TIMESTAMP(3) NOT NULL,
    arrival_deadline TIMESTAMP(3) NULL,
    status VARCHAR(32) NOT NULL,
    cancel_reason VARCHAR(32),
    check_in_at TIMESTAMP(3),
    check_out_at TIMESTAMP(3),
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT uq_test_reservation_request UNIQUE (user_id, client_request_id),
    CONSTRAINT fk_test_reservation_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_test_reservation_resource FOREIGN KEY (resource_id) REFERENCES resource (id)
);

CREATE INDEX ix_test_reservation_resource_time ON reservation (resource_id, start_at, end_at);
CREATE INDEX ix_test_reservation_user_time ON reservation (user_id, start_at);

CREATE TABLE reservation_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    operator_id BIGINT,
    occurred_at TIMESTAMP(3) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    CONSTRAINT fk_test_event_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_test_event_operator FOREIGN KEY (operator_id) REFERENCES app_user (id)
);

CREATE TABLE violation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    violation_type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT uq_test_violation UNIQUE (reservation_id, violation_type),
    CONSTRAINT fk_test_violation_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_test_violation_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);
