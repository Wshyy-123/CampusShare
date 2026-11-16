INSERT IGNORE INTO campus_resource (id, name, location, type, status) VALUES
(1, '北体育馆乒乓球区', '北体育馆一层', 'PING_PONG', 'ACTIVE'),
(6, '教学楼教室', '教学楼A区', 'CLASSROOM', 'ACTIVE');

INSERT IGNORE INTO resource_unit (id, resource_id, name, unit_code, status) VALUES
(1, 1, '乒乓球桌1号', 'PP-001', 'ACTIVE'),
(9, 6, 'A101教室', 'CR-A101', 'ACTIVE');

INSERT INTO schedule_period (resource_unit_id, period_type, start_at, end_at, note)
SELECT 1, 'OPEN', '2026-11-03 08:00:00', '2026-11-03 22:00:00', '演示开放时段'
WHERE NOT EXISTS (
    SELECT 1 FROM schedule_period
    WHERE resource_unit_id = 1 AND period_type = 'OPEN'
      AND start_at = '2026-11-03 08:00:00' AND end_at = '2026-11-03 22:00:00'
);

INSERT INTO schedule_period (resource_unit_id, period_type, start_at, end_at, note)
SELECT 1, 'TEACHING', '2026-11-03 10:00:00', '2026-11-03 12:00:00', '体育课占用'
WHERE NOT EXISTS (
    SELECT 1 FROM schedule_period
    WHERE resource_unit_id = 1 AND period_type = 'TEACHING'
      AND start_at = '2026-11-03 10:00:00' AND end_at = '2026-11-03 12:00:00'
);
