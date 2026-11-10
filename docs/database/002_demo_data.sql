-- ============================================================
-- 校享 CampusShare 演示数据
-- 适用：空库初始化后执行一次；所有账号均为虚构演示账号。
-- 演示密码统一为 Campus@123（bcrypt 摘要见 app_user）。
-- ============================================================

USE campusshare;

INSERT INTO app_user (id, account, password_hash, display_name, role, enabled) VALUES
    (1, 'student01', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', '张三', 'STUDENT', TRUE),
    (2, 'student02', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', '李四', 'STUDENT', TRUE),
    (3, 'teacher01', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', '王老师', 'TEACHER', TRUE),
    (4, 'admin01', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', '资源管理员', 'RESOURCE_ADMIN', TRUE);

INSERT INTO resource (id, name, location, resource_type, unit_label, enabled) VALUES
    (101, '图书馆A区01座', '图书馆一楼', 'LIBRARY_SEAT', 'A-01', TRUE),
    (102, '图书馆A区02座', '图书馆一楼', 'LIBRARY_SEAT', 'A-02', TRUE),
    (201, '体育馆1号羽毛球场', '体育馆二楼', 'BADMINTON', '1号场', TRUE),
    (202, '体育馆2号羽毛球场', '体育馆二楼', 'BADMINTON', '2号场', TRUE),
    (301, '乒乓球台1号', '体育馆一楼', 'TABLE_TENNIS', '1号台', TRUE),
    (401, '网球场1号', '室外运动场', 'TENNIS', '1号场', TRUE);

-- 未来 7 天，每天 08:00-22:00 开放
INSERT INTO resource_opening (resource_id, start_at, end_at)
SELECT r.id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d.n DAY), '08:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d.n DAY), '22:00:00')
FROM resource r
JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) d
WHERE r.enabled = TRUE;

-- 明天 10:00-12:00 羽毛球场有教学占用，用于验证 XIA-5 的阻塞检查
INSERT INTO resource_block (resource_id, block_type, start_at, end_at, reason)
VALUES (201, 'TEACHING',
        TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'),
        TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '12:00:00'),
        '体育课教学');

-- 演示预约：覆盖 RESERVED、CANCELLED、CHECKED_IN、NO_SHOW 四种展示结果
INSERT INTO reservation
    (id, user_id, resource_id, client_request_id, start_at, end_at, accepted_at,
     arrival_deadline, status, cancel_reason, check_in_at, check_out_at, created_at, updated_at)
VALUES
    (1, 1, 101, 'demo-student01-seat-001',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'),
     NOW(3), DATE_ADD(NOW(3), INTERVAL 30 MINUTE),
     'RESERVED', NULL, NULL, NULL, NOW(3), NOW(3)),
    (2, 1, 201, 'demo-student01-badminton-cancelled',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '15:00:00'),
     DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(3), INTERVAL 2 DAY), INTERVAL 15 MINUTE),
     'CANCELLED', 'USER_CANCEL', NULL, NULL, NOW(3), NOW(3)),
    (3, 2, 102, 'demo-student02-seat-001',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00'),
     NOW(3), DATE_ADD(NOW(3), INTERVAL 30 MINUTE),
     'RESERVED', NULL, NULL, NULL, NOW(3), NOW(3)),
    (4, 1, 401, 'demo-student01-tennis-checked-in',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '15:00:00'),
     DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(3), INTERVAL 2 DAY), INTERVAL 15 MINUTE),
     'CHECKED_IN', NULL,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:05:00'), NULL, NOW(3), NOW(3)),
    (5, 1, 101, 'demo-student01-seat-no-show',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:00:00'),
     DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY) + INTERVAL 30 MINUTE,
     'CANCELLED', 'NO_SHOW', NULL, NULL, NOW(3), NOW(3));

INSERT INTO reservation_event (reservation_id, event_type, operator_id, occurred_at, request_id, reason) VALUES
    (1, 'CREATED', 1, NOW(3), 'demo-request-001', '创建预约'),
    (2, 'CANCELLED', 1, NOW(3), 'demo-request-002', 'USER_CANCEL'),
    (4, 'CREATED', 1, NOW(3), 'demo-request-004', '创建预约'),
    (5, 'CANCELLED', NULL, NOW(3), 'demo-request-005', 'NO_SHOW');

INSERT INTO violation_record (reservation_id, user_id, violation_type, occurred_at)
VALUES (5, 1, 'NO_SHOW', NOW(3));
