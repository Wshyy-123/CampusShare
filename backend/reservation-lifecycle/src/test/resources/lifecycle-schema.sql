-- Isolated integration-test fixture only; not the team's production schema.
CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY,
  account VARCHAR(100) NOT NULL UNIQUE,
  role VARCHAR(32) NOT NULL,
  enabled BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS resource (
  id BIGINT PRIMARY KEY,
  resource_type VARCHAR(32) NOT NULL,
  enabled BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS resource_block (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  block_type VARCHAR(32) NOT NULL,
  start_at DATETIME(3) NOT NULL,
  end_at DATETIME(3) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  INDEX ix_block_resource_time(resource_id, start_at, end_at),
  FOREIGN KEY(resource_id) REFERENCES resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  resource_id BIGINT NOT NULL,
  client_request_id VARCHAR(100) NOT NULL,
  start_at DATETIME(3) NOT NULL,
  end_at DATETIME(3) NOT NULL,
  accepted_at DATETIME(3) NOT NULL,
  arrival_deadline DATETIME(3) NOT NULL,
  status VARCHAR(32) NOT NULL,
  cancel_reason VARCHAR(32),
  check_in_at DATETIME(3),
  check_out_at DATETIME(3),
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uq_reservation_request(user_id, client_request_id),
  INDEX ix_reservation_resource_time(resource_id, start_at, end_at),
  INDEX ix_reservation_expiry(status, accepted_at, id),
  FOREIGN KEY(resource_id) REFERENCES resource(id),
  FOREIGN KEY(user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS reservation_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  operator_id BIGINT NULL,
  occurred_at DATETIME(3) NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  FOREIGN KEY(reservation_id) REFERENCES reservation(id),
  FOREIGN KEY(operator_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS violation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  violation_type VARCHAR(32) NOT NULL,
  occurred_at DATETIME(3) NOT NULL,
  UNIQUE KEY uq_violation_reservation_type(reservation_id, violation_type),
  FOREIGN KEY(reservation_id) REFERENCES reservation(id),
  FOREIGN KEY(user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
