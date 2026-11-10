# ADR-001：XIA-5/XIA-6 预约垂直切片的工程边界

- 状态：已采用（本地实现）
- 日期：2026-11-02
- 关联工作项：XIA-5、XIA-6
- 相关分支：`feature/XIA-11-XIA-14-lifecycle`（共享可用性检查）、`feature/XIA-9-XIA-10`（前端脚手架）

## 背景

`main` 当时只有 docs，没有共用前后端工程。XIA-9/10 分支提供了 Spring Boot 单体 + Vite 前端，但其数据库结构缺少开放安排、阻塞安排、预约事件、幂等字段和到馆期限，接口也没有统一响应格式；XIA-11/14 分支提供了独立的 lifecycle 库模块、真实 MySQL 测试和 CI，并明确要求创建预约在同一个事务内调用 `ResourceAvailabilityGuard`。

## 决定

1. 在 `backend/` 建立一个可独立运行的 Spring Boot 3.5.16 + Java 21 工程，包根为 `com.campusshare`，预约业务放在 `reservation` 包。
2. 复制 XIA-11/14 的 `com.campusshare.lifecycle` 最小共享部分（`ResourceAvailabilityGuard`、`LifecycleMapper`、`LifecycleException`、`LifecycleConfiguration`、`SeatArrivalDeadline`）；不复制其停用接口和座位未到定时任务。
3. 创建预约的事务顺序：
   - 校验参数并识别当前用户；
   - 快速幂等检查；
   - 调用 `ResourceAvailabilityGuard.assertNotBlocked` 锁 resource 行并检查启用/阻塞；
   - 拿到资源锁后再次读取同一 `clientRequestId`，处理并发重复提交；
   - 锁定读开放安排；
   - 锁定读同资源有效预约冲突；
   - 插入 `reservation` 与 `reservation_event` 后提交。
4. `frontend/` 使用 XIA-9/10 的 Vite + Vue 3 + TypeScript 脚手架，重写为路由化页面；请求统一走 `src/api/http.ts`，自动处理 CSRF。
5. 前端默认代理到 `http://localhost:8081`，因为本机 8080 已被其他进程占用；后端端口可用 `CAMPUSSHARE_PORT` 覆盖。
6. `auth` 和只读 `resource` 接口是 XIA-1/XIA-4 落地前的临时依赖，已在 README、OpenAPI 和验证记录中标注，不声称完成对应故事。

## 后果

- XIA-5/XIA-6 可以在没有共用主工程的情况下独立运行、测试和演示。
- 合并到团队主工程时，需要把 `com.campusshare.reservation`、`common`、`config` 及对应 mapper/schema 迁入；`auth`/`resource` 应替换为 XIA-1/XIA-4 的正式实现。
- 数据库脚本以 `docs/database/001_schema.sql` 为基线；若团队已有其他已应用脚本，不应静默重写，应新建 `003_*.sql` 变更。
