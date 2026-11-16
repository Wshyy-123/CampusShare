# XIA-7 / XIA-8 主工程集成验证（2026-11-16）

## 集成范围

分支 `feature/XIA-7-XIA-8` 已合入 `origin/main`（`2cd69de`），按《主工程集成与协作要求》把体育取消、签到、签退迁入主工程：

| 项 | 变更 |
|---|---|
| 公共层 | 删除本分支自建的 `ApiResponse/ApiException/ErrorCode/GlobalExceptionHandler/RequestIdFilter`，改用 main 的 `common` 与 `BusinessException` |
| 身份 | 删除 dev 调试头 `CurrentUserProvider`，改用 main 的 `auth/CurrentUserProvider`（服务端会话） |
| 接口 | 新增 `reservation/controller/SportsReservationController`，复用 main 的 `ReservationService.detail` 返回 `ReservationVO` |
| 数据库 | 删除与 003 重复的 `003_sports_attendance.sql`、`004_sports_demo_data.sql`；`check_out_at`、`reservation_event`、唯一约束均复用 main 的 003 |
| 超时任务 | `SportsTimeoutTask` 增加 `campusshare.sports-no-show.enabled` 开关（默认开启），只处理体育资源 |
| 前端 | 删除独立 App 外壳、`SportsReservationView.vue`、`LibraryTerminal.vue`；在 main 的 `ReservationDetailView.vue` 上增加取消/签到/签退按钮，走 `reservationsApi` |

## 实际执行

| 检查 | 命令 | 结果 |
|---|---|---|
| 后端编译 | `mvnw.cmd -B -q -DskipTests compile` | 通过 |
| 单元测试 | `mvnw.cmd -B test -Dtest=SportsBookingRulesTest,SportsReservationServiceTest` | 22 个通过（7 规则 + 15 服务） |
| 真实 MySQL 联调 | `MYSQL_TEST=true` + `SportsReservationIntegrationTest` | 9 个通过 |
| 前端构建 | `pnpm run build`（本机无 npm，用仓库自带 Node 24 的 pnpm） | 通过，68 个模块 |
| 全量 `mvnw.cmd -B verify` | 无环境变量覆盖，测试 profile 使用 H2 | 118 个测试，0 失败、0 错误、12 跳过（含本分支 9 条 MySQL 联调用例，未设置 `MYSQL_TEST` 时跳过）；BUILD SUCCESS |

### 联调环境

- 数据库：MySQL 8.4.13，`127.0.0.1:3307`，按 main 的 `001`~`007` 顺序应用（无本分支新增迁移）。
- 应用：Spring Boot 3.5.16，Java 21；测试用账号 `cs_test`。
- 联调用例：取消释放、重复取消幂等、开始后拒绝取消、座位不可按体育取消、他人预约 404、窗口内签到、提前签到拒绝、NO_SHOW 后签到拒绝且不恢复、签退完成、超时任务不影响座位。

## 未执行 / 已知限制

1. **未做浏览器手工点选**：详情页新增按钮只做了类型检查与构建，没有在浏览器里实际点击验证。
2. **未做手机真实扫码**：扫码入口仍按 reservationId 调用，地点码解析未实现。
3. **npm ci 未执行**：本机没有 npm，使用 pnpm 安装与构建；CI 上的 npm 结果以工作流为准。
4. **漏签退未自动完成**：本故事未实现，预约会停留在 CHECKED_IN。
5. **提前签到默认不允许**、迟到临界为开始后 15 分钟整，均待 PO 确认。

## 与旧验证记录的关系

`docs/verification/XIA-7-XIA-8.md` 记录的是迁移前（dev 调试身份、独立前端）的验证结果，保留作为历史；本文件记录合入主工程后的实际结果。
