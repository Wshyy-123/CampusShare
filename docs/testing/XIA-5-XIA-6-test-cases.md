# XIA-5 / XIA-6 测试用例

- 版本：V1.0
- 日期：2026-11-03
- 关联计划：`docs/testing/XIA-5-XIA-6-test-plan.md`
- 关联验证记录：`docs/verification/XIA-5-XIA-6.md`

## 1. 使用说明

- 优先级：P0 = 主流程/验收必测；P1 = 重要边界/异常/权限；P2 = 体验/兼容/补充。
- 类型：功能、边界、异常、权限、并发、安全、UI、兼容、数据。
- 状态：`通过（自动化）`、`通过（端到端）`、`通过（自动化+端到端）`、`未执行（原因）`、`待评审`。
- 自动化方法名位于 `backend/src/test/java/com/campusshare/reservation/ReservationCreateIntegrationTest.java` 等同目录测试类。
- 端到端检查使用真实 Session + CSRF，请求经 Vite `/api` 代理到后端。
- 演示数据基线：`student01`、`student02`、`teacher01`，密码 `Campus@123`；资源 101/102 座位、201/202 羽毛球、301 乒乓球、401 网球；201 在 D+1 10:00–12:00 有教学占用。

## 2. 测试数据

| 数据项 | 值 |
|---|---|
| D+1 空闲座位时段 | 资源 101，15:00–16:00（测试夹具中无冲突） |
| D+1 空闲体育时段 | 资源 201，13:00–14:00（避开教学占用） |
| 重叠时段 | 资源 101，15:30–16:30 与 15:00–16:00 重叠 |
| 教学占用时段 | 资源 201，D+1 10:30–11:30 |
| 非开放时段 | 资源 101，D+1 06:00–07:00 |
| 停用资源 | 将资源 101 的 `enabled` 置为 FALSE |
| 演示预约 | student01：座位 RESERVED、羽毛球 CANCELLED(USER_CANCEL)、网球 CHECKED_IN、座位 CANCELLED(NO_SHOW)；student02：座位 RESERVED |

## 3. XIA-5 创建预约测试用例

| 用例ID | 优先级 | 类型 | 前置条件 | 操作步骤 | 预期结果 | 实际结果/状态 | 自动化/证据 |
|---|---|---|---|---|---|---|---|
| TC-X5-001 | P0 | 功能 | student01 已登录；资源 101 开放；D+1 15:00–16:00 空闲 | 提交 resourceId=101、start/end、唯一 clientRequestId | 201；code=OK；status=RESERVED；返回资源、时段、acceptedAt/arrivalDeadline；数据库新增一条预约和一条 CREATED 事件 | 通过 | `createsReservationAndWritesEvent`；端到端 create-201 |
| TC-X5-002 | P0 | 功能 | student01 已登录；资源 201 在 D+1 13:00–14:00 开放且无占用 | 提交体育预约 | 201；status=RESERVED；资源类型 BADMINTON | 通过 | `sportsDeadlineIsFifteenMinutesFromStart`；端到端 sports-deadline |
| TC-X5-003 | P0 | 规则 | 座位预约创建成功 | 比较 acceptedAt 与 arrivalDeadline | 差值恰好 30 分钟 | 通过 | `seatDeadlineIsThirtyMinutesFromAcceptedTime`；端到端 seat-deadline |
| TC-X5-004 | P0 | 规则 | 体育预约创建成功 | 比较 startTime 与 arrivalDeadline | 差值恰好 15 分钟 | 通过 | `sportsDeadlineIsFifteenMinutesFromStart`；端到端 sports-deadline |
| TC-X5-005 | P0 | 功能/冲突 | student01 已创建资源 101 的 15:00–16:00 预约 | student02 提交同资源同重叠时段 | 409；code=RESERVATION_CONFLICT；数据库仍只有一条有效占用 | 通过 | `rejectsOverlappingReservationFromAnotherUser`；端到端 conflict-409 |
| TC-X5-006 | P0 | 并发 | MySQL 8.0.44；资源 101 同一时段空闲 | 两个用户同时提交重叠预约，线程同时放行 | 一个成功，另一个 409 RESERVATION_CONFLICT；数据库只有一条 RESERVED/CHECKED_IN | 通过（MySQL） | `concurrentOverlappingRequestsLeaveAtMostOneActiveReservation` |
| TC-X5-007 | P0 | 幂等 | 已成功创建一条预约 | 用相同 clientRequestId 和完全相同参数再次提交 | 200；返回同一 reservationId；数据库仍只有一条 | 通过 | `idempotentReplayReturnsSameReservation`；端到端 idempotent-replay |
| TC-X5-008 | P0 | 幂等/异常 | 已成功创建一条预约 | 用相同 clientRequestId、不同结束时间再次提交 | 409；code=IDEMPOTENCY_CONFLICT；不新增预约 | 通过 | `sameClientRequestIdWithDifferentParametersIsConflict`；端到端 idempotency-conflict-409 |
| TC-X5-009 | P0 | 并发/幂等 | MySQL；同一用户、同一 clientRequestId、同一参数 | 两个线程同时提交 | 一个 NEW、一个 REPLAY；返回同一 reservationId；数据库只有一条 | 通过（MySQL；本用例发现并修复 DEF-001） | `concurrentSameClientRequestIdReturnsOneReservation` |
| TC-X5-010 | P0 | 规则/阻塞 | 资源 201 在 D+1 10:00–12:00 有教学占用 | 提交 10:30–11:30 的预约 | 409；code=RESOURCE_UNAVAILABLE；不新增预约 | 通过 | `rejectsTeachingBlock`；端到端 teaching-block-409 |
| TC-X5-011 | P0 | 规则/停用 | 资源 101 的 enabled=FALSE | 提交该资源预约 | 409；code=RESOURCE_UNAVAILABLE；不新增预约 | 通过（自动化） | `rejectsDisabledResource`；端到端未单独重复此分支 |
| TC-X5-012 | P0 | 规则/开放 | 资源 101 仅 08:00–22:00 开放 | 提交 D+1 06:00–07:00 | 409；code=RESOURCE_UNAVAILABLE；不新增预约 | 通过 | `rejectsTimeOutsideOpening`；端到端 outside-opening-409 |
| TC-X5-013 | P1 | 边界 | 已登录 | 开始时间晚于结束时间 | 400；code=INVALID_ARGUMENT；不新增预约 | 通过 | `rejectsInvalidTimeRange` |
| TC-X5-014 | P1 | 边界 | 已登录 | 开始时间等于结束时间 | 400；code=INVALID_ARGUMENT；不新增预约 | 通过 | `rejectsEqualStartAndEnd` |
| TC-X5-015 | P1 | 异常 | 已登录 | 请求体缺少 clientRequestId | 400；code=INVALID_ARGUMENT；不新增预约 | 通过 | `rejectsMissingClientRequestId` |
| TC-X5-016 | P1 | 异常 | 已登录 | resourceId 为非数字、0 或负数 | 400；code=INVALID_ARGUMENT；不新增预约 | 待补单独自动化（服务端 `parseResourceId` 已覆盖） | 代码审查；建议补参数化测试 |
| TC-X5-017 | P0 | 权限 | 无会话 | 未登录提交创建请求 | 401；code=AUTH_REQUIRED；不新增预约 | 通过 | `requiresAuthentication`；端到端 unauth-401（查询侧） |
| TC-X5-018 | P0 | 安全 | 已登录 | 创建请求不携带 CSRF 请求头 | 403；code=CSRF_INVALID；不新增预约 | 通过 | `rejectsWriteWithoutCsrfToken`；端到端 csrf-missing-403 |
| TC-X5-019 | P1 | 时间 | 已登录；D+1 09:00–10:00 空闲 | 用 UTC 偏移 `01:00Z–02:00Z` 提交 | 201；响应 startTime/endTime 为 `09:00–10:00+08:00` | 通过 | `convertsUtcOffsetToShanghaiBusinessTime` |
| TC-X5-020 | P1 | 数据 | 创建成功 | 查询 reservation_event | 存在 CREATED 事件，operator_id=当前用户，request_id 非空 | 通过 | `createsReservationAndWritesEvent` |
| TC-X5-021 | P1 | 安全/契约 | 已登录 | 检查请求体和响应 | 请求不传 userId；业务 ID 为字符串；响应含 code/message/data/requestId | 通过 | 端到端响应检查；OpenAPI 审查 |
| TC-X5-022 | P2 | UI | 前端可运行；student01 登录 | 资源列表→选择座位→选择 D+1 16:30–17:30→提交 | 页面显示“预约成功”、预约号、状态和详情入口 | 未执行（浏览器输入/截图表面超时） | 前端构建通过；DOM 可渲染；点击走查待补 |
| TC-X5-023 | P2 | UI/异常 | 前端可运行；存在冲突/教学占用 | 选择冲突或教学占用时段提交 | 页面显示冲突/不可用提示；刷新当天安排 | 未执行（同上） | API 侧已验证 409 分支 |
| TC-X5-024 | P2 | UI/幂等 | 前端可运行；模拟网络失败 | 同一参数重试提交 | 复用 clientRequestId；成功后不重复建预约 | 未执行（同上） | 代码审查：`ensureRequestId` |
| TC-X5-025 | P2 | 兼容 | 手机或 375×812 视口 | 走完选择日期/时段/提交 | 表单可点、无横向溢出、按钮可触达 | 未执行 | 待浏览器/真机恢复后补做 |

## 4. XIA-6 我的预约测试用例

| 用例ID | 优先级 | 类型 | 前置条件 | 操作步骤 | 预期结果 | 实际结果/状态 | 自动化/证据 |
|---|---|---|---|---|---|---|---|
| TC-X6-001 | P0 | 功能 | student01 有座位、羽毛球、网球预约 | 查询 `/reservations/mine` | 200；只含 student01 的记录；每条含资源、时段、状态；按开始时间倒序 | 通过 | `listsOnlyOwnReservations...`；端到端 mine-only-own |
| TC-X6-002 | P0 | 权限 | student01/student02 各有预约 | student02 查询列表 | 只返回 student02 自己的记录，不含 student01 的 reservationId | 通过 | `anotherUserSeesOnlyTheirOwnReservation`；端到端 other-user-mine |
| TC-X6-003 | P1 | 功能 | 存在 RESERVED/CANCELLED 等记录 | 按 status=CANCELLED 查询 | 只返回取消记录；带 cancelReason | 通过 | `filtersByStatus`；端到端 mine-status-history |
| TC-X6-004 | P1 | 功能 | 至少 2 条本人记录 | page=2、size=1 查询 | total 正确；items 长度为 1；page/size 回显 | 通过 | `paginates` |
| TC-X6-005 | P1 | 功能 | 有一条 D+1 14:00 的预约 | from=13:00、to=16:00 查询 | 只返回该时段开始的记录 | 通过 | `filtersByStartTimeRange` |
| TC-X6-006 | P0 | 功能/空状态 | teacher01 没有预约 | 查询列表 | 200；items=[]；total=0 | 通过 | `emptyStateReturnsEmptyItems`；端到端 empty-state |
| TC-X6-007 | P0 | 功能/权限 | student01 有预约 id=own | 查询 `/reservations/{own}` | 200；返回本人资源、时段、状态、acceptedAt/arrivalDeadline | 通过 | `returnsOwnReservationDetail`；端到端 detail-own |
| TC-X6-008 | P0 | 权限 | student02 的预约 id=other | student01 查询 other | 404；code=RESERVATION_NOT_FOUND；不返回他人信息 | 通过 | `cannotReadAnotherUsersReservation`；端到端 other-user-detail-404 |
| TC-X6-009 | P1 | 异常 | 已登录 | 查询不存在的 id=999999 | 404；code=RESERVATION_NOT_FOUND | 通过 | `unknownReservationIsNotFound` |
| TC-X6-010 | P0 | 状态 | 演示数据含 CANCELLED(USER_CANCEL)、CANCELLED(NO_SHOW)、CHECKED_IN | 查询列表/详情 | 状态与取消原因如实展示；不再显示为 RESERVED | 通过（API） | `filtersByStatus`；端到端 mine-status-history；UI 文字待走查 |
| TC-X6-011 | P1 | 异常 | 已登录 | status=WAITING、page=0、size=101 | 400；code=INVALID_ARGUMENT | 通过 | `rejectsInvalidStatusAndPagination` |
| TC-X6-012 | P0 | 权限 | 无会话 | 查询我的预约 | 401；code=AUTH_REQUIRED | 通过 | `rejectsAnonymousReservationQuery`；端到端 unauth-401 |
| TC-X6-013 | P1 | UI | 前端可运行；student01 登录 | 打开“我的预约” | 卡片显示资源、时间、状态文字（非仅颜色）、取消原因；分页可用 | 未执行（浏览器输入/截图表面超时） | 前端构建通过；状态组件代码审查 |
| TC-X6-014 | P1 | UI | 前端可运行；有本人预约 | 点击卡片进入详情 | 显示预约号、资源、时段、状态、acceptedAt、arrivalDeadline、checkIn/out、cancelReason | 未执行（同上） | 前端构建通过；API 详情已验证 |
| TC-X6-015 | P2 | UI | 前端可运行 | 制造加载、失败、空列表 | 分别显示加载/错误/暂无预约记录，不误显示成功 | 未执行（同上） | 前端代码有对应分支；空状态 API 已验证 |
| TC-X6-016 | P2 | 兼容 | 375×812 视口 | 查看列表和详情 | 卡片不溢出、字段可读、按钮可点 | 未执行 | 待浏览器/真机恢复后补做 |
| TC-X6-017 | P2 | 数据/UI | 后端状态字典 | 对照 RESERVED/CHECKED_IN/COMPLETED/CANCELLED 与 USER_CANCEL/NO_SHOW/RESOURCE_DISABLED | 前后端文字一致；未知状态不崩溃 | 通过（代码审查） | `StatusBadge.vue`；`ReservationService` |
| TC-X6-018 | P2 | 安全 | 他人预约与不存在 id | 分别请求 | 两者都返回 404 RESERVATION_NOT_FOUND，不通过 403/404 差异泄露 | 通过 | `cannotReadAnotherUsersReservation`、`unknownReservationIsNotFound`；端到端 |

## 5. 跨故事/工程测试用例

| 用例ID | 优先级 | 类型 | 操作 | 预期结果 | 实际结果/状态 | 证据 |
|---|---|---|---|---|---|---|
| TC-CROSS-001 | P0 | 构建 | `mvn test`（H2） | 编译通过；36 个测试，0 失败，2 个 MySQL 专用并发测试跳过 | 通过 | H2 测试输出 |
| TC-CROSS-002 | P0 | 数据库 | 在隔离 MySQL 8.0.44 执行 `mvn test` | 36 个测试，0 失败，0 跳过；并发用例执行 | 通过 | MySQL 测试输出 |
| TC-CROSS-003 | P1 | CI | 推送后运行 GitHub Actions | MySQL 8.4 service 上 `mvn verify`、前端 `npm ci && npm run build` 全绿 | 未执行（工作流已配置，未推送运行） | `.github/workflows/ci.yml` |
| TC-CROSS-004 | P0 | 前端构建 | `npm run build` | `vue-tsc -b` 通过；Vite 构建成功 | 通过 | 45 个模块，构建成功 |
| TC-CROSS-005 | P1 | 契约 | 检查成功/失败响应 | 统一 code/message/data/requestId；错误码稳定；HTTP 状态正确 | 通过 | 自动化 JSON 断言；端到端错误体 |
| TC-CROSS-006 | P1 | 脚本 | 在 MySQL 8.0.44 执行 001/002 | 建表、6 个资源、42 条开放、1 条教学占用、5 条演示预约 | 通过 | 实际执行记录 |
| TC-CROSS-007 | P1 | 安全 | 扫描仓库真实密码 | 不含真实数据库密码；配置使用环境变量；演示密码仅为虚构账号 | 通过 | 本地扫描；`application.yml` 使用 `${CAMPUSSHARE_DB_PASSWORD:}` |
| TC-CROSS-008 | P1 | 评审 | 另一名成员评审代码/契约/脚本 | 评审意见关闭；有评审记录 | 未执行（待 PR） | 待补 |
| TC-CROSS-009 | P1 | GUI | 真实浏览器走登录→预约→列表→详情 | 主流程可用；无可见错误 | 阻塞（IAB 输入/截图表面超时；DOM 检查发现并修复 main.ts 未安装路由） | 不标记通过 |
| TC-CROSS-010 | P2 | 兼容 | 手机真机/窄屏 | 关键操作可用；无溢出 | 未执行 | 待补 |
| TC-CROSS-011 | P2 | 文档 | 校验 OpenAPI/YAML | YAML 可解析；路径/字段与实现一致 | 通过 | Python YAML 解析；端到端对照 |

## 6. 执行摘要

| 项目 | 结果 |
|---|---|
| 后端自动化（H2） | 36 个测试，0 失败、0 错误、2 跳过（MySQL 专用并发） |
| 后端自动化（MySQL 8.0.44） | 36 个测试，0 失败、0 错误、0 跳过 |
| 接口端到端 | 25 项检查全部通过（2026-11-02 执行；早于最后的锁定读修复，修复后未重跑，修复点由 MySQL 并发自动化用例覆盖） |
| 前端构建 | 通过（45 个模块） |
| 真实浏览器点击走查 | 未完成（工具表面超时；已修复入口路由缺陷） |
| GitHub Actions MySQL 8.4 | 未运行（工作流已提交） |
| 另一名成员评审 | 未进行 |
| 结论 | PR/评审/CI 可开始；团队 DoD 尚未全部完成 |

## 6.1 PingCode 导入记录

- 时间：2026-11-03；测试库：`XXC`（校享测试库）。
- 导入文件：`docs/testing/XIA-5-XIA-6-test-cases-import.xlsx`。
- 预览验证：**43/43 通过，0 条未通过**；导入后测试库总数 26 → 69。
- 新增编号：
  - XIA-5：XXC-32 至 XXC-56（25 条）。
  - XIA-6：XXC-57 至 XXC-74（18 条）。
- 模块列为空，未勾选“自动创建新模块”，新增用例位于“无模块用例”；后续可按需移动到 XIA-5/XIA-6 模块。
- 跨故事/工程用例（TC-CROSS-001 至 TC-CROSS-011）未导入 PingCode，保留在本文档。

## 7. 缺陷记录

### DEF-001：并发同一 clientRequestId 误报预约冲突

- 严重级别：高（影响幂等与并发重复提交）
- 关联用例：TC-X5-009
- 现象：两个线程用同一用户、同一 `clientRequestId`、同一参数并发创建；一个成功，另一个返回 409 `RESERVATION_CONFLICT`，没有返回原预约。
- 复现：MySQL 8.0.44，两个线程同时调用 `ReservationService.create`。
- 根因：MySQL 默认 REPEATABLE READ；等待资源锁之前执行过普通 `SELECT`，事务已建立旧快照；拿到资源锁后用普通 `SELECT` 重查幂等键，看不到另一个事务刚提交的预约。
- 修复：新增 `ReservationMapper.lockByUserAndClientRequestId`，使用 `SELECT ... FOR UPDATE`；拿到资源锁后用锁定读重查 `user_id + client_request_id`。
- 复测：`concurrentSameClientRequestIdReturnsOneReservation` 在 MySQL 8.0.44 通过；全量 36 个测试 0 失败、0 跳过。
- 状态：已修复并复测通过；合并前仍需评审。

## 8. 待补/未关闭项

1. 真实浏览器主流程点击、成功/失败反馈和窄屏检查（工具恢复后）。
2. GitHub Actions 在 MySQL 8.4 上实际运行并回填结果。
3. 另一名成员代码/契约/脚本评审。
4. `resourceId` 非法值（非数字/0/负数）的参数化自动化用例。
5. XIA-1/XIA-4 正式交付后替换临时 auth/resource 依赖并重跑相关用例。
6. XIA-7/8/11 接入后的取消、签到、超时状态联动回归。
