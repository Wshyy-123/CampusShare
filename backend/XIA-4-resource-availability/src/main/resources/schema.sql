CREATE TABLE IF NOT EXISTS campus_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(200) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resource_unit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    unit_code VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_resource_unit_resource
        FOREIGN KEY (resource_id) REFERENCES campus_resource(id),
    CONSTRAINT uk_resource_unit_code UNIQUE (unit_code),
    INDEX idx_resource_unit_resource (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS schedule_period (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_unit_id BIGINT NOT NULL,
    period_type VARCHAR(20) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    note VARCHAR(200) NULL,
    CONSTRAINT fk_schedule_unit
        FOREIGN KEY (resource_unit_id) REFERENCES resource_unit(id),
    INDEX idx_schedule_unit_time (resource_unit_id, start_at, end_at),
    INDEX idx_schedule_unit_type_time (resource_unit_id, period_type, start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_unit_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    status VARCHAR(24) NOT NULL,
    CONSTRAINT fk_reservation_unit
        FOREIGN KEY (resource_unit_id) REFERENCES resource_unit(id),
    INDEX idx_reservation_unit_time (resource_unit_id, start_at, end_at, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resource_block (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_unit_id BIGINT NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    reason VARCHAR(200) NULL,
    CONSTRAINT fk_block_unit
        FOREIGN KEY (resource_unit_id) REFERENCES resource_unit(id),
    INDEX idx_block_unit_time (resource_unit_id, start_at, end_at, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
