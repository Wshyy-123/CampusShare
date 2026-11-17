-- MySQL 5.7.26 / InnoDB / utf8mb4. Import into a NEW EMPTY database.
-- Final schema equivalent to historical migrations 001-008. No database is selected here.
SET NAMES utf8mb4;
SET time_zone='+08:00';
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `app_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account` varchar(50) NOT NULL COMMENT '登录账号',
  `display_name` varchar(50) DEFAULT NULL COMMENT '显示名称',
  `password_hash` varchar(100) DEFAULT NULL COMMENT '密码摘要',
  `role` varchar(32) NOT NULL DEFAULT 'STUDENT',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reservation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '预约用户ID',
  `resource_id` bigint(20) NOT NULL COMMENT '关联资源ID',
  `client_request_id` varchar(100) NOT NULL,
  `accepted_at` datetime(3) NOT NULL COMMENT '预约成功时间(XIA-10用于计算30分钟超时)',
  `arrival_deadline` datetime(3) DEFAULT NULL,
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'RESERVED' COMMENT '状态: RESERVED, CHECKED_IN, CANCELLED, COMPLETED',
  `check_in_at` datetime(3) DEFAULT NULL COMMENT '实际到馆签到时间',
  `check_out_at` datetime(3) DEFAULT NULL,
  `cancel_reason` varchar(50) DEFAULT NULL COMMENT '取消原因: USER_CANCEL, NO_SHOW 等',
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reservation_request` (`user_id`,`client_request_id`),
  KEY `ix_reservation_resource_time` (`resource_id`,`start_at`,`end_at`),
  KEY `ix_reservation_user_time` (`user_id`,`start_at`),
  KEY `ix_reservation_expiry` (`status`,`accepted_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reservation_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint(20) NOT NULL,
  `event_type` varchar(32) NOT NULL COMMENT 'CREATED / CANCELLED / COMPLETED / NO_SHOW / RESOURCE_DISABLED',
  `operator_id` bigint(20) DEFAULT NULL COMMENT 'NULL 表示系统任务',
  `occurred_at` datetime(3) NOT NULL,
  `request_id` varchar(64) NOT NULL,
  `reason` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_event_reservation` (`reservation_id`,`occurred_at`),
  KEY `fk_event_operator` (`operator_id`),
  CONSTRAINT `fk_event_operator` FOREIGN KEY (`operator_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_event_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约历史';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '资源名称',
  `location` varchar(100) DEFAULT NULL COMMENT '位置',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型：LIBRARY_SEAT, BADMINTON 等',
  `unit_label` varchar(50) NOT NULL DEFAULT '',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_block` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `block_type` varchar(32) NOT NULL COMMENT 'TEACHING / EXTERNAL / DISABLED',
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `reason` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_block_resource_time` (`resource_id`,`start_at`,`end_at`),
  CONSTRAINT `fk_block_resource` FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学/外部占用和停用安排';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_catalog_guard` (
  `id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_opening` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `start_at` datetime(3) NOT NULL,
  `end_at` datetime(3) NOT NULL,
  `note` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_opening_resource_time` (`resource_id`,`start_at`,`end_at`),
  CONSTRAINT `fk_opening_resource` FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='允许预约的开放安排';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_schedule_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `schedule_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `schedule_id` bigint(20) NOT NULL,
  `operation` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator_id` bigint(20) NOT NULL,
  `occurred_at` datetime(3) NOT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `before_json` longtext COLLATE utf8mb4_unicode_ci,
  `after_json` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `fk_schedule_event_resource` (`resource_id`),
  KEY `fk_schedule_event_operator` (`operator_id`),
  CONSTRAINT `fk_schedule_event_operator` FOREIGN KEY (`operator_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_schedule_event_resource` FOREIGN KEY (`resource_id`) REFERENCES `resource` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `violation_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '违规用户ID',
  `reservation_id` bigint(20) NOT NULL COMMENT '关联的预约ID',
  `violation_type` varchar(50) NOT NULL COMMENT '违规类型，如 SPORTS_NO_SHOW',
  `occurred_at` datetime(3) NOT NULL,
  `created_at` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_violation_reservation_type` (`reservation_id`,`violation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
