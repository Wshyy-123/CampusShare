-- ==========================================
-- 校享 CampusShare 初始演示数据
-- 文件名：002_demo_data.sql
-- ==========================================

USE campusshare;

-- 1. 插入测试用户
INSERT INTO app_user (id, account, display_name) VALUES
                                                     (1, 'student01', '张三'),
                                                     (2, 'student02', '李四');

-- 2. 插入测试资源
INSERT INTO resource (id, name, location, resource_type) VALUES
                                                             (1, '图书馆A区01座', '图书馆一楼', 'LIBRARY_SEAT'),
                                                             (2, '体育馆1号羽毛球场', '体育馆二楼', 'BADMINTON');

-- 3. 插入测试预约记录（用于测试 XIA-10 刷卡和取消）
-- 预约ID=1：用于测试刷卡成功（状态 RESERVED，时间是刚刚）
INSERT INTO reservation (id, user_id, resource_id, accepted_at, start_at, status)
VALUES (1, 1, 1, NOW(), NOW(), 'RESERVED');

-- 预约ID=2：用于测试取消规则（状态 RESERVED，预约时间是明天）
INSERT INTO reservation (id, user_id, resource_id, accepted_at, start_at, status)
VALUES (2, 1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), 'RESERVED');

-- 预约ID=3：用于测试 XIA-9 定时任务超时自动取消（状态 RESERVED，开始时间是 20 分钟前）
INSERT INTO reservation (id, user_id, resource_id, accepted_at, start_at, status)
VALUES (3, 2, 2, NOW(), DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'RESERVED');