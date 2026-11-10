-- 校享 CampusShare 演示账号 V2
-- 统一演示密码：Campus@123
-- password_hash 为 BCrypt 值，仅用于本地演示；正式环境应改用真实账号体系并更换密码。
-- 执行方式：mysql -u root -p campus_share < V2__seed_demo_accounts.sql

USE campus_share;

INSERT IGNORE INTO app_user (username, password_hash, display_name, role, enabled)
VALUES ('stu2024001', '$2b$10$CcJc0hlFM.s7YAssmNuy7epmluDlJzF/wS/HCSCPDvXFwaxPTTv6W', '张同学', 'STUDENT', 1),
       ('tea2024001', '$2b$10$CcJc0hlFM.s7YAssmNuy7epmluDlJzF/wS/HCSCPDvXFwaxPTTv6W', '李老师', 'TEACHER', 1),
       ('admin01', '$2b$10$CcJc0hlFM.s7YAssmNuy7epmluDlJzF/wS/HCSCPDvXFwaxPTTv6W', '王管理员', 'RESOURCE_ADMIN', 1);
