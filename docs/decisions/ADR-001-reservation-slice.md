# ADR-001：XIA-5/XIA-6 预约切片与主后端集成

- 状态：已采用（V1.2 同步后修订）
- 日期：2026-11-02，修订 2026-11-10
- 关联工作项：XIA-5、XIA-6
- 相关分支/提交：`feature/XIA-11-XIA-14-lifecycle`、`feature/XIA-9-XIA-10`（已由 PR #2 合入 main）、`origin/main` f59c17a

## 背景

最初实现时，`main` 只有文档，团队还没有共用后端。XIA-9/10 的分支提供了 `campus-share-backend/` 与 `frontend/`，XIA-11/14 提供了独立 lifecycle 库模块和真实 MySQL 测试。我们当时在 `backend/` 建了一个可独立运行的预约切片。

2026-11-04 的 V1.2 文档与 AGENTS.md 明确：当前主后端是 `campus-share-backend/`；独立功能模块必须接入主工程的依赖、包扫描、共享数据库、认证和统一锁顺序，不能另建独立生产后端。团队随后把 XIA-9/10 的 PR #2 合入 `main`。

## 决定

1. 删除独立 `backend/` 生产工程，把 `com.campusshare.common/auth/resource/reservation/lifecycle/config` 源码并入 `campus-share-backend/src/main/java/`。
2. 主启动类 `org.campus.campussharebackend.CampusShareBackendApplication` 使用：
   - `scanBasePackages = {"org.campus.campussharebackend", "com.campusshare"}`
   - `@MapperScan` 覆盖原主工程 mapper、auth/resource/reservation mapper；lifecycle 由 `LifecycleConfiguration` 扫描。
3. 我们的预约 Mapper 改名为 `ReservationRepository`，避免与主工程旧的 `ReservationMapper` 默认 Bean 名冲突；XML namespace 同步更新。
4. 主工程 `pom.xml` 合并 validation、Spring Security、H2/Spring Security Test；`application.yml` 改为环境变量注入数据库连接，不提交真实密码；端口默认 8080，可用 `CAMPUSSHARE_PORT` 覆盖。
5. 数据库不在 001/002 上静默改结构，新增：
   - `campus-share-backend/docs/database/003_xia5_xia6_core.sql`：补 `app_user.role/enabled`、`resource.unit_label`、`reservation.client_request_id/arrival_deadline/check_out_at`、索引与唯一约束、`resource_opening`、`resource_block`、`reservation_event`、`violation_record.occurred_at`。
   - `004_xia5_xia6_demo.sql`：演示账号、7 个资源、开放安排、教室教学占用、体育外部占用、RESERVED/CANCELLED/COMPLETED/NO_SHOW 演示预约。
6. 主工程旧的 `SportsTimeoutTask` 按全部 RESERVED 扫描，不符合 V1.2 按资源类型处理的要求；加 `campusshare.sports-no-show.enabled=false` 默认关闭，待 XIA-9 负责人改为分类处理后启用。`CheckInController` 仍使用旧 `userId` 查询参数，已通过 `campusshare.legacy-checkin.enabled=false` 默认关闭；待 XIA-8 负责人替换为会话身份+地点码后再启用。
7. 前端保留 `frontend/`，默认代理到 8080；本机端口冲突可用 `VITE_API_PROXY_TARGET` 覆盖。班级/体育预约的 15 分钟文案、地点/外部占用显示按 V1.2 调整。

## 验证

- H2：37 个测试，0 失败，2 个 MySQL 专用并发测试跳过。
- 本地 MySQL 8.0.12（3306，独立测试库 `campusshare_xia56_test`）：37 个测试，0 失败，0 跳过。
- 主后端 + 前端 + `campusshare` 演示库真实验证：26 项端到端检查全部通过，覆盖座位/教室/体育创建、30/15 分钟期限、教学/外部占用、幂等、冲突、我的预约状态和越权 404。
- 迁移 001→002→003→004 已在 MySQL 8.0.12 实际执行成功。

## 后果与限制

- XIA-5/6 已在主工程运行；XIA-1/XIA-4 仍是临时依赖，XIA-7/8/9/10/11 的新流程尚未接入。
- 本地 MySQL 8.0.12 低于基线 8.4；CI 配置使用 MySQL 8.4 service，但尚未在 GitHub Actions 实际运行。
- 旧的 `CheckInController` 仍信任 `userId` 查询参数，不能作为 V1.2 的最终签到实现。
- 工作区改动尚未推送；PR 需关联真实 XIA-5/XIA-6 编号并由另一名成员评审。
