INSERT IGNORE INTO campus_resource (id, name, location, type, status) VALUES
(1, '北体育馆乒乓球区', '北体育馆一层', 'PING_PONG', 'ACTIVE'),
(2, '南体育馆羽毛球场', '南体育馆', 'BADMINTON', 'ACTIVE'),
(3, '网球场', '东区网球场', 'TENNIS', 'ACTIVE'),
(4, '图书馆座位区', '图书馆二层', 'LIBRARY_SEAT', 'ACTIVE'),
(5, '图书馆讨论室', '图书馆三层', 'DISCUSSION_ROOM', 'ACTIVE'),
(6, '教学楼教室', '教学楼A区', 'CLASSROOM', 'ACTIVE');

INSERT IGNORE INTO resource_unit (id, resource_id, name, unit_code, status) VALUES
(1, 1, '乒乓球桌1号', 'PP-001', 'ACTIVE'),
(2, 1, '乒乓球桌2号', 'PP-002', 'ACTIVE'),
(3, 2, '羽毛球场1号', 'BD-001', 'ACTIVE'),
(4, 2, '羽毛球场2号', 'BD-002', 'ACTIVE'),
(5, 3, '网球场1号', 'TN-001', 'ACTIVE'),
(6, 4, '座位A001', 'LIB-A001', 'ACTIVE'),
(7, 4, '座位A002', 'LIB-A002', 'ACTIVE'),
(8, 5, '讨论室301', 'DR-301', 'ACTIVE'),
(9, 6, 'A101教室', 'CR-A101', 'ACTIVE');

INSERT INTO schedule_period (resource_unit_id, period_type, start_at, end_at, note)
SELECT 1, 'OPEN', '2026-11-03 08:00:00', '2026-11-03 22:00:00', '演示开放时段'
WHERE NOT EXISTS (
    SELECT 1 FROM schedule_period
    WHERE resource_unit_id = 1
      AND period_type = 'OPEN'
      AND start_at = '2026-11-03 08:00:00'
      AND end_at = '2026-11-03 22:00:00'
);

INSERT INTO schedule_period (resource_unit_id, period_type, start_at, end_at, note)
SELECT 1, 'TEACHING', '2026-11-03 10:00:00', '2026-11-03 12:00:00', '体育课占用'
WHERE NOT EXISTS (
    SELECT 1 FROM schedule_period
    WHERE resource_unit_id = 1
      AND period_type = 'TEACHING'
      AND start_at = '2026-11-03 10:00:00'
      AND end_at = '2026-11-03 12:00:00'
);

INSERT INTO reservation (resource_unit_id, user_id, start_at, end_at, status)
SELECT 1, 10001, '2026-11-03 14:00:00', '2026-11-03 15:00:00', 'RESERVED'
WHERE NOT EXISTS (
    SELECT 1 FROM reservation
    WHERE resource_unit_id = 1
      AND start_at = '2026-11-03 14:00:00'
      AND end_at = '2026-11-03 15:00:00'
      AND status = 'RESERVED'
);

INSERT INTO resource_block (resource_unit_id, start_at, end_at, active, reason)
SELECT 1, '2026-11-03 18:00:00', '2026-11-03 19:00:00', TRUE, '设备维护'
WHERE NOT EXISTS (
    SELECT 1 FROM resource_block
    WHERE resource_unit_id = 1
      AND start_at = '2026-11-03 18:00:00'
      AND end_at = '2026-11-03 19:00:00'
      AND active = TRUE
);
