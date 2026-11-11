-- 测试账号密码均为 Campus@123。
INSERT INTO app_user (id, account, password_hash, display_name, role, enabled) VALUES
    (1, 'student01', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', '张三', 'STUDENT', TRUE),
    (2, 'student02', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', '李四', 'STUDENT', TRUE),
    (3, 'teacher01', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', '王老师', 'TEACHER', TRUE),
    (4, 'admin01', '$2b$10$pMs6bd56oNjyOqFDeDugCuZ80ZrHQyNn16y96fTV87jKadixaPo4y', '资源管理员', 'RESOURCE_ADMIN', TRUE);

INSERT INTO resource (id, name, location, resource_type, unit_label, enabled) VALUES
    (101, '图书馆A区01座', '图书馆一楼', 'LIBRARY_SEAT', 'A-01', TRUE),
    (102, '图书馆A区02座', '图书馆一楼', 'LIBRARY_SEAT', 'A-02', TRUE),
    (201, '体育馆1号羽毛球场', '体育馆二楼', 'BADMINTON', '1号场', TRUE),
    (401, '网球场1号', '室外运动场', 'TENNIS', '1号场', TRUE);
