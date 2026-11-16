-- ============================================================
-- 校享 CampusShare 迁移 006：XIA-3 开放安排备注字段
-- 适用：已应用 001 至 005 的 campusshare 数据库。不重写已有脚本。
-- 开放安排（resource_opening）补充备注，与 resource_block.reason 对应，
-- 供管理员在时段维护页面记录开放说明（如节假日调整、临时开放）。
-- ============================================================

USE campusshare;

ALTER TABLE resource_opening
    ADD COLUMN note VARCHAR(200) NULL AFTER end_at;
