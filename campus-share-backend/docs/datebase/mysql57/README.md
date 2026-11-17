# MySQL 5.7.26 数据库入口

当前统一版本：**MySQL 5.7.26、InnoDB、utf8mb4**。不再要求安装8.4。主工程和CI均按5.7.26验证。

## 选择导入模式

| 目的 | 按顺序导入 | 结果 |
|---|---|---|
| 日常开发、老师现场演示（默认） | `001_schema.sql` → `002_demo_data.sql` | 4个虚构账号、14个不重复资源（含7间教室）、一周开放及101段占用，预约从空开始 |
| 完整复现2026-11-17本机数据 | `001_schema.sql` → `snapshot_2026-11-17.sql` | 15资源、19预约及审计/违规历史，原日期、旧重复记录均原样保留 |

两种模式分别导入**新建空库**，不能把两份数据文件叠加。脚本没有DROP/TRUNCATE/DELETE，不负责覆盖旧库。已有数据的开发库只追加缺少的历史迁移，已应用001—007的只执行上级目录`008_resource_identity_guard.sql`；不要重跑初始化脚本。

完整快照只含原项目4个虚构演示账号，未包含本机MySQL账户、连接密码、Session或其他数据库。快照保留绝对日期和全部当前预约历史，包括用于验证的历史记录；它不是每天自动更新的演示数据。旧座位样例的日期和NO_SHOW状态也保留作历史，不代表当前仍能提前预约。旧重复资源#1/#503只保留在快照里，干净初始化版仅保留#1，没有改动本机原库。

## 从空库导入

先在phpStudy的MySQL命令行进入仓库根目录，使用你自己的数据库账号密码连接；数据库名应为空的新库。下面演示默认应用库名：

```sql
CREATE DATABASE campusshare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campusshare;
SOURCE campus-share-backend/docs/datebase/mysql57/001_schema.sql;
SOURCE campus-share-backend/docs/datebase/mysql57/002_demo_data.sql;
```

若`campusshare`已经存在，先停在这里，按已有库升级路径处理；不要把这些命令用于覆盖自己的数据。需要快照模式时，最后一行替换为：

```sql
SOURCE campus-share-backend/docs/datebase/mysql57/snapshot_2026-11-17.sql;
```

导入后核对：

```sql
SELECT VERSION();
SHOW TABLES;
SELECT resource_type, COUNT(*) FROM resource GROUP BY resource_type;
SELECT COUNT(*) FROM resource_catalog_guard WHERE id=1;
```

共9张表，全部InnoDB；guard行数应为1。完整数量及文件校验值在`manifest.json`。

## 启动与协作

- 后端仍使用`campus-share-backend/`，在IDEA环境变量配置`CAMPUSSHARE_DB_URL`、`CAMPUSSHARE_DB_USERNAME`、`CAMPUSSHARE_DB_PASSWORD`，不提交本机密码。默认数据库名campusshare、端口3306、业务时区Asia/Shanghai。
- 前端在`frontend/`执行`npm ci`，复制`.env.example`为`.env.local`并设`VITE_DATA_MODE=api`，再执行`npm run dev`。本地配置由Git忽略。
- 虚构演示账号：student01、student02、teacher01、admin01；演示密码均为`Campus@123`，不能把它当真实学校账号。
- 所有资源仅能新建当天尚未开始的预约；管理员仍可维护未来课表。干净初始化版课表按导入当天生成七天，示例课程不代表学校真实课表。讨论室可在开始前取消，座位到馆期限仍按成功后30分钟计算。
- 同学开发先读[本次统一开发基准](../../../../docs/校享CampusShare_统一开发基准与MySQL57同步_2026-11-17.md)，继续在主工程分支中扩展。

## 兼容历史迁移与自动验证

历史001—007不重写。新装采用本目录的最终结构，不再直接执行旧001的8.4排序规则。需要验证历史升级路径时，`scripts/mysql57/prepare_legacy.py`只生成临时兼容副本，替换5.7不支持的排序规则并设置InnoDB和时区，不修改原文件。

`scripts/mysql57/verify_exports.py`在三个新建隔离库分别验证干净导入、完整快照、历史001—008，检查表结构一致、全部InnoDB、外键完整、数据数量与资源身份唯一性。它拒绝复用已有库，只清理自己创建的隔离库。MySQL客户端密码通过`MYSQL_PWD`或本机配置提供，脚本中没有连接密码。
