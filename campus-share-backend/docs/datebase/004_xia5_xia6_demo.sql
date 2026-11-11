-- ============================================================
-- 校享 CampusShare 迁移 004：XIA-5/XIA-6 演示数据与 V1.2 占用样例
-- 适用：在 001 + 002 + 003 之后执行；演示账号均为虚构数据。
-- 演示密码统一为 Campus@123。
-- ============================================================

USE campusshare;

-- 1. 演示账号（002 只有 student01/student02；补齐密码、角色和教师/管理员）
UPDATE app_user
SET password_hash = '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y',
    role = 'STUDENT',
    enabled = TRUE
WHERE account IN ('student01', 'student02');

INSERT INTO app_user (account, display_name, password_hash, role, enabled) VALUES
    ('teacher01', '王老师', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', 'TEACHER', TRUE),
    ('admin01', '资源管理员', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', 'RESOURCE_ADMIN', TRUE)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    enabled = VALUES(enabled);

-- 2. 资源：保留 002 的 1/2，补充座位、球桌、球场和教室
UPDATE resource SET name = '图书馆A区01座', location = '图书馆一楼', resource_type = 'LIBRARY_SEAT', unit_label = 'A-01', enabled = TRUE WHERE id = 1;
UPDATE resource SET name = '体育馆1号羽毛球场', location = '体育馆二楼', resource_type = 'BADMINTON', unit_label = '1号场', enabled = TRUE WHERE id = 2;

INSERT INTO resource (id, name, location, resource_type, unit_label, enabled) VALUES
    (102, '图书馆A区02座', '图书馆一楼', 'LIBRARY_SEAT', 'A-02', TRUE),
    (202, '体育馆2号羽毛球场', '体育馆二楼', 'BADMINTON', '2号场', TRUE),
    (301, '乒乓球台1号', '体育馆一楼', 'TABLE_TENNIS', '1号台', TRUE),
    (401, '网球场1号', '室外运动场', 'TENNIS', '1号场', TRUE),
    (501, '教学楼A101教室', '教学楼A区一楼', 'CLASSROOM', 'A101', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    location = VALUES(location),
    resource_type = VALUES(resource_type),
    unit_label = VALUES(unit_label),
    enabled = VALUES(enabled);

-- 3. 未来 7 天 08:00-22:00 开放
INSERT INTO resource_opening (resource_id, start_at, end_at)
SELECT r.id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d.n DAY), '08:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d.n DAY), '22:00:00')
FROM resource r
JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) d
WHERE r.id IN (1, 2, 102, 202, 301, 401, 501);

-- 4. V1.2 模拟占用：教室教务占用、体育外部占用
INSERT INTO resource_block (resource_id, block_type, start_at, end_at, reason) VALUES
    (501, 'TEACHING',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '12:00:00'),
     '模拟教务教学占用'),
    (2, 'EXTERNAL',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '18:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '20:00:00'),
     '模拟体育外部占用');

-- 5. XIA-6 展示用预约：RESERVED / CANCELLED / COMPLETED / NO_SHOW
INSERT IGNORE INTO reservation
    (user_id, resource_id, client_request_id, start_at, end_at, accepted_at,
     arrival_deadline, status, cancel_reason, check_in_at, check_out_at, created_at, updated_at)
VALUES
    (1, 102, 'XIA56-DEMO-RESERVED',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'),
     NOW(3), DATE_ADD(NOW(3), INTERVAL 30 MINUTE),
     'RESERVED', NULL, NULL, NULL, NOW(3), NOW(3)),
    (1, 2, 'XIA56-DEMO-CANCELLED',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '15:00:00'),
     DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(3), INTERVAL 2 DAY), INTERVAL 15 MINUTE),
     'CANCELLED', 'USER_CANCEL', NULL, NULL, NOW(3), NOW(3)),
    (1, 401, 'XIA56-DEMO-COMPLETED',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '15:00:00'),
     DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(3), INTERVAL 2 DAY), INTERVAL 15 MINUTE),
     'COMPLETED', NULL,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:05:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '15:00:00'),
     NOW(3), NOW(3)),
    (1, 102, 'XIA56-DEMO-NOSHOW',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:00:00'),
     DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(3), INTERVAL 2 DAY), INTERVAL 30 MINUTE),
     'CANCELLED', 'NO_SHOW', NULL, NULL, NOW(3), NOW(3)),
    (2, 102, 'XIA56-DEMO-STUDENT2',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:00:00'),
     NOW(3), DATE_ADD(NOW(3), INTERVAL 30 MINUTE),
     'RESERVED', NULL, NULL, NULL, NOW(3), NOW(3));

-- 6. 预约历史
INSERT INTO reservation_event (reservation_id, event_type, operator_id, occurred_at, request_id, reason)
SELECT id, 'CREATED', user_id, NOW(3), 'demo-xia56-evt-created', '创建预约'
FROM reservation WHERE client_request_id = 'XIA56-DEMO-RESERVED';

INSERT INTO reservation_event (reservation_id, event_type, operator_id, occurred_at, request_id, reason)
SELECT id, 'CANCELLED', user_id, NOW(3), 'demo-xia56-evt-cancelled', 'USER_CANCEL'
FROM reservation WHERE client_request_id = 'XIA56-DEMO-CANCELLED';

INSERT INTO reservation_event (reservation_id, event_type, operator_id, occurred_at, request_id, reason)
SELECT id, 'COMPLETED', user_id, NOW(3), 'demo-xia56-evt-completed', '正常结束'
FROM reservation WHERE client_request_id = 'XIA56-DEMO-COMPLETED';

INSERT INTO reservation_event (reservation_id, event_type, operator_id, occurred_at, request_id, reason)
SELECT id, 'CANCELLED', NULL, NOW(3), 'demo-xia56-evt-noshow', 'NO_SHOW'
FROM reservation WHERE client_request_id = 'XIA56-DEMO-NOSHOW';

-- 7. 座位未到违规
INSERT IGNORE INTO violation_record (reservation_id, user_id, violation_type, occurred_at)
SELECT id, user_id, 'NO_SHOW', NOW(3)
FROM reservation WHERE client_request_id = 'XIA56-DEMO-NOSHOW';
