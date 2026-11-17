-- 008：串行化低频资源管理写入，防止新增/编辑并发绕过物理资源身份校验。
-- 旧库可能已有重复资源：不删除、停用、合并或重编号，保留原预约与外键。
-- 使用数据库行锁而非进程锁，多个后端实例也共享同一写入闸门。
USE campusshare;

CREATE TABLE IF NOT EXISTS resource_catalog_guard (
    id INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO resource_catalog_guard(id) VALUES (1);
