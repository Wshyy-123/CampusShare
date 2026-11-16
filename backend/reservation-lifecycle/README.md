# XIA-11 / XIA-14：预约释放模块

本模块只实现资源停用取消预约（XIA-14）和图书馆座位未到馆超时释放（XIA-11）。仓库尚无共用后端，故提供独立 Maven 库模块及真实 MySQL 集成测试，供统一工程合并；不实现其他成员的登录、资源维护、创建预约、刷卡或前端页面。

## 已确认的规则

- 停用指定资源的具体起止时间，区间为 `[startTime, endTime)`；重叠判断为 `reservation.start < disable.end && reservation.end > disable.start`。
- 仅处理 RESERVED、CHECKED_IN 的重叠预约。CHECKED_IN 且服务端时间已到预约开始时间，视为已经开始使用，整次停用返回 409，不写停用、不取消任何预约。提前签到但尚未开始使用的预约可以取消。
- 已完成、已取消、其他资源、不重叠的预约原样保留。取消原因 RESOURCE_DISABLED，操作人、操作时间和追踪号写入历史。停用原因单独保存在 resource_block。
- 释放采用状态变更，不删除预约。停用安排继续存在，取消预约不重新开放资源。
- 座位以 accepted_at（服务端确认创建成功的时间）起算30分钟。恰好到达截止时间即超时；签到方应使用同一个 SeatArrivalDeadline 规则。
- 未确认到馆且仍为 RESERVED 的 LIBRARY_SEAT 才自动取消，原因 NO_SHOW，并写一条 NO_SHOW 违规和一条取消事件。已经确认到馆不再处理；体育等其他类型不处理。
- 后台任务默认启动即扫描、每30秒扫描一次，重启后补查积压。释放的实际执行允许有一个扫描周期的延迟。单个失败回滚并记录日志，下次扫描重试，不阻止后续候选。

以上停用边界已由任务分配者在本次开发中确认。其他故事的业务规则由对应负责人实现。

## 合并到共享后端

依赖固定为 Java 21、Spring Boot 3.5.16、MyBatis Starter 3.0.5。Maven Wrapper 固定3.9.9。主工程可以依赖此 jar，也可以将源码合入主工程，并导入 `LifecycleConfiguration`（如果主工程已经扫描 com.campusshare，也可由组件扫描发现配置）。没有另起一个生产应用、登录实现或数据库自动初始化流程。

1. 共用应用提供 DataSource、事务管理器和 Spring Security 会话认证。`Authentication.getName()` 必须为经服务端验证的 `app_user.account`，管理员 authority 为 `ROLE_RESOURCE_ADMIN`。模块还会核对数据库中的 role 和 enabled。请求不能传管理员身份代替认证。
2. 在共享 SecurityFilterChain 中保护 `/api/v1/admin/resources/**`，保留原有 CSRF、Session 和统一401/403处理。模块本身不提供 SecurityFilterChain，也不关闭 CSRF；集成测试仅模拟已经认证的 SecurityContext，未验证共享登录链。
3. 数据库按团队数据字典提供 app_user、resource、resource_block、reservation、reservation_event、violation_record。Mapper 中 SQL 列名与约定一致；`operator_id` 允许 NULL 表示系统；新增 block/event/violation 的 id 由数据库生成。停用 block_type 为 DISABLED，取消 event_type 为 CANCELLED，违规 violation_type 为 NO_SHOW。数据库负责人应同步这些编码。
4. `violation_record(reservation_id, violation_type)` 必须有唯一约束；建议索引 `resource_block(resource_id,start_at,end_at)`、`reservation(resource_id,start_at,end_at)`、`reservation(status,accepted_at,id)`。本模块的完整最小建表 SQL 位于 `src/test/resources/lifecycle-schema.sql`，仅用于隔离测试，不充当共享生产数据库迁移，避免替其他成员决定全量表结构。
5. **创建预约的负责人**：在创建预约的同一事务中，先调用 `ResourceAvailabilityGuard.assertNotBlocked(resourceId,start,end)`，再执行其余开放时间、预约冲突检查及 INSERT；必须一直持有资源行锁到提交。该方法要求已有事务，不能在查询接口检查后另开事务创建。它只负责资源启用和阻塞检查，不代替其他预约校验。由于创建预约故事不在本次分工内，本模块没有编写该入口；验收标准“后续预约被拒绝”通过该共享检查及数据库事务测试验证，最终仍需接入创建入口联调。
6. **到馆确认的负责人**：同一事务先锁 resource，再锁 reservation，锁后重新读取状态、资源安排并取得服务端时间；对座位使用 `SeatArrivalDeadline.expired(acceptedAt, now)`，已超时或 CANCELLED 时拒绝确认。不能先读时间再长时间等待锁。写入 CHECKED_IN 和 check_in_at 后提交。不得恢复失效预约。本模块测试用模拟到馆事务验证竞态，没有实现刷卡入口。
7. 所有影响可用性的写入口统一先锁 resource，再按预约 id 顺序锁 reservation；多个资源按资源 id 排序。查询候选不算最终业务判断。隔离级别沿用 MySQL 默认 REPEATABLE READ；本模块的业务复核均为锁定读，避免使用旧快照。
8. 时间按 Asia/Shanghai 处理，数据库 DATETIME(3)。写入的对外带偏移时间转换为该时区并截取毫秒；共用应用创建预约时也应按毫秒精度保存 accepted_at/arrival_deadline，其中 arrival_deadline=accepted_at+30分钟。后台以 accepted_at 为权威计算基准。

共享非敏感配置示例（由主工程合并，无需密码入库）：

```yaml
campusshare:
  seat-no-show:
    enabled: true
    scan-delay-ms: 30000
```

主应用关闭此功能可配置 enabled=false。模块配置开启 Spring Scheduling；如果团队统一管理调度，可以迁移该注解，但应保留 SeatNoShowJob。

## 接口

`POST /api/v1/admin/resources/{resourceId}/disable-periods`，管理员会话，写请求按主工程携带 CSRF。具体契约见 `docs/api/lifecycle.yaml`（仓库根目录）。成功HTTP200，ID以字符串返回；接口请求不接收 userId/operatorId。

```json
{"startTime":"2026-11-02T10:00:00+08:00","endTime":"2026-11-02T12:00:00+08:00","reason":"场地维修"}
```

```json
{"code":"OK","message":"停用成功","data":{"resourceId":"101","cancelledReservationIds":["1001"]},"requestId":"服务端生成的追踪号"}
```

常见错误：400 INVALID_ARGUMENT、401 AUTH_REQUIRED、403 FORBIDDEN、404 RESOURCE_NOT_FOUND、409 INVALID_STATE、500 INTERNAL_ERROR。后台任务不提供普通用户可触发的HTTP接口。

## 构建和测试

在本模块目录执行 `./mvnw verify`（Windows：`./mvnw.cmd verify`），默认执行不需要数据库的单元测试，MySQL测试明确跳过。要执行完整数据库测试，先创建独立库：

```sql
CREATE DATABASE campusshare_lifecycle_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

测试要求库名精确为 campusshare_lifecycle_test，初始化和清理只在校验该库名后执行。测试会反复清理该库中的测试表，不要把真实数据放入测试库。

PowerShell 设置环境变量后执行：

```powershell
$env:LIFECYCLE_MYSQL_TEST='true'
$env:LIFECYCLE_TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/campusshare_lifecycle_test?connectionTimeZone=%2B08:00&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8'
$env:LIFECYCLE_TEST_DB_USER='root'
# 在本地设置 LIFECYCLE_TEST_DB_PASSWORD，不把密码写入仓库
./mvnw.cmd verify
```

测试夹具仅含虚构管理员、学生、两类资源和预约，不含密码或真实用户数据。验证结果及环境限制见仓库 `docs/verification/XIA-11-XIA-14.md`。
