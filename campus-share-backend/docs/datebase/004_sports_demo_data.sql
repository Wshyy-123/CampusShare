-- ==========================================
-- 校享 CampusShare 演示数据（XIA-7 / XIA-8）
-- 文件名：004_sports_demo_data.sql
-- 说明：在已应用 001、002、003 的数据库上执行一次
-- ==========================================

USE campusshare;

-- 4 号：明天 10:00 的羽毛球，状态 RESERVED —— 用于演示 XIA-7 使用前取消
INSERT INTO reservation (id, user_id, resource_id, accepted_at, start_at, end_at, status)
VALUES (4, 1, 2, NOW(3),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 10 HOUR),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 11 HOUR),
        'RESERVED');

-- 5 号：正在进行中的羽毛球，已签到 —— 用于演示 XIA-8 扫码签退
INSERT INTO reservation (id, user_id, resource_id, accepted_at, start_at, end_at, status, check_in_at)
VALUES (5, 1, 2, DATE_SUB(NOW(3), INTERVAL 20 MINUTE),
        DATE_SUB(NOW(3), INTERVAL 10 MINUTE),
        DATE_ADD(NOW(3), INTERVAL 50 MINUTE),
        'CHECKED_IN', DATE_SUB(NOW(3), INTERVAL 10 MINUTE));

-- 6 号：他人（2 号用户）的羽毛球 —— 用于验证"他人预约返回 404、不泄露记录"
INSERT INTO reservation (id, user_id, resource_id, accepted_at, start_at, end_at, status)
VALUES (6, 2, 2, NOW(3),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 14 HOUR),
        DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 15 HOUR),
        'RESERVED');

-- 7 号：半小时后才开始的羽毛球 —— 用于验证"未到可签到时间"返回 409
INSERT INTO reservation (id, user_id, resource_id, accepted_at, start_at, end_at, status)
VALUES (7, 1, 2, NOW(3),
        DATE_ADD(NOW(3), INTERVAL 30 MINUTE),
        DATE_ADD(NOW(3), INTERVAL 90 MINUTE),
        'RESERVED');
