# XIA-5 / XIA-6 验证记录

- 执行日期：2026-11-02 至 2026-11-03（Asia/Shanghai）
- 执行位置：`backend/`、`frontend/`（本地）
- 对应故事：XIA-5 创建预约；XIA-6 我的预约与详情
- 关联测试计划/用例：`docs/testing/XIA-5-XIA-6-test-plan.md`、`docs/testing/XIA-5-XIA-6-test-cases.md`
- 结论：后端自动化测试在 H2 与隔离 MySQL 8.0.44 上均通过；真实会话的 API 端到端检查全部通过；前端类型检查与生产构建通过。浏览器工具能渲染页面，但输入/截图表面超时，未能完成真实点击的完整 GUI 走查，详见“浏览器检查”。

## 一、实际环境和命令

| 项目 | 实际值 |
|---|---|
| JDK | Microsoft OpenJDK 21.0.9 |
| Spring Boot | 3.5.16 |
| MyBatis Starter | 3.0.5 |
| Maven | 3.9.11（Wrapper 固定 3.9.9） |
| Node / npm | Node 22.23.1 / npm 11.12.1 |
| 默认测试库 | H2 2.x（MySQL 模式，内存） |
| MySQL 联调库 | 隔离实例 MySQL 8.0.44，127.0.0.1:3307，测试库 `campusshare_test`、演示库 `campusshare` |
| 前端 | Vue 3.5.44 + Vite 8.3.5 + TypeScript 6.0.3 |

```powershell
# H2 测试
cd backend
$env:JAVA_HOME = 'D:\Program Files\Java\jdk-21.0.9'
.\mvnw.cmd test

# 隔离 MySQL 8.0.44 测试（本机 3306 服务未启动，使用独立 3307 实例）
.\mvnw.cmd test `
  -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3307/campusshare_test?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" `
  -Dspring.datasource.username=root `
  -Dspring.datasource.password= `
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 前端
cd ../frontend
npm run build
```

## 二、自动化测试结果

| 运行 | 结果 |
|---|---|
| H2：`mvn test` | 36 个测试，0 失败、0 错误、2 跳过；跳过的是仅在 MySQL 执行的行锁并发测试 |
| MySQL 8.0.44：`mvn test`（参数覆盖数据源） | 36 个测试，0 失败、0 错误、0 跳过；两个并发测试实际执行 |
| 前端：`npm run build` | `vue-tsc -b` 通过；Vite 构建成功，45 个模块 |

测试类：

- `AuthIntegrationTest`：会话登录、`/auth/me`、退出、错误密码、CSRF 缺失、匿名访问。
- `ReservationCreateIntegrationTest`：创建成功并写事件、座位 30 分钟期限、体育 15 分钟期限、重叠冲突、幂等重放、幂等参数冲突、教学占用、开放时段外、资源停用、非法时间、开始=结束、缺少 clientRequestId、UTC 偏移转上海时间、未登录、并发重叠、并发同一 clientRequestId。
- `ReservationQueryIntegrationTest`：只返回本人、状态筛选、分页、时间筛选、空状态、本人详情、他人详情 404、不存在 404、非法状态/分页。
- `ResourceIntegrationTest`：资源列表/筛选、指定日期开放/阻塞/已有预约、资源不存在、匿名访问。

关键断言：

- 新建预约返回 201，状态 `RESERVED`，同时写入 `reservation_event(event_type='CREATED')`。
- 同一 `clientRequestId` 同参重放返回 200 且为同一预约号；参数不同返回 409 `IDEMPOTENCY_CONFLICT`。
- 座位 `arrival_deadline = accepted_at + 30 分钟`；体育 `arrival_deadline = start_at + 15 分钟`。
- 两个用户并发提交同资源重叠时段：一个成功，另一个得到 409 `RESERVATION_CONFLICT`，数据库只保留一个有效占用。
- 两个线程并发提交同一用户、同一 `clientRequestId`、同一参数：一个新建、一个重放，返回同一预约号，数据库只有一条记录。

### 2.1 本轮测试发现并修复的问题

并发同一 `clientRequestId` 用例最初失败：一个请求成功，另一个得到 `RESERVATION_CONFLICT`。原因是 MySQL 默认 REPEATABLE READ 下，等待资源锁之前在事务中执行过的普通 `SELECT` 已经建立旧快照；拿到资源锁后用普通查询重查幂等键，看不到另一个事务刚提交的预约。

修复：新增 `ReservationMapper.lockByUserAndClientRequestId`，SQL 使用 `FOR UPDATE`；创建预约在拿到资源锁后用该锁定读重查 `user_id + client_request_id`。修复后该用例在 MySQL 8.0.44 上通过。这个修复也说明了“冲突/幂等复核必须使用当前锁定读，不能依赖先前普通查询快照”的必要性。
- 教学占用、停用资源、开放时段外均返回 409 `RESOURCE_UNAVAILABLE`。
- 本人列表不含他人记录；他人预约详情统一返回 404 `RESERVATION_NOT_FOUND`，不泄露记录是否存在。

## 三、真实会话端到端联调

环境：隔离 MySQL 8.0.44 上的 `campusshare` 演示库；后端 8081；前端 Vite 5173；请求经 Vite `/api` 代理；使用演示账号和真实 Session + CSRF。

| 检查 | 实际结果 |
|---|---|
| CSRF 获取 | 200，返回 `X-XSRF-TOKEN` |
| student01 登录 / `auth/me` | 200，身份为张三 / STUDENT |
| 资源列表 | 6 条启用资源 |
| 图书馆座位当天可约信息 | 1 条开放安排、1 条已有预约 |
| 创建座位预约（空时段） | 201，状态 RESERVED |
| 座位期限 | `acceptedAt` 到 `arrivalDeadline` 恰好 30 分钟 |
| 同 clientRequestId 重放 | 200，返回同一预约号 |
| 同资源重叠时段 | 409 `RESERVATION_CONFLICT` |
| 同 clientRequestId 不同参数 | 409 `IDEMPOTENCY_CONFLICT` |
| 教学占用时段 | 409 `RESOURCE_UNAVAILABLE` |
| 开放时段外 | 409 `RESOURCE_UNAVAILABLE` |
| 体育预约 | 201，`arrivalDeadline = start + 15 分钟` |
| 我的预约 | 只含本人记录，同时出现 RESERVED / CHECKED_IN / CANCELLED 状态 |
| 本人详情 | 200 |
| 缺少 CSRF 写请求 | 403 `CSRF_INVALID` |
| student02 查看 student01 的预约 | 404 `RESERVATION_NOT_FOUND` |
| student02 我的预约 | 只含 student02 自己的记录 |
| teacher01 我的预约 | 200，`items=[]`、`total=0`（空状态） |
| 匿名访问我的预约 | 401 `AUTH_REQUIRED` |

以上检查全部通过。

> 说明：这 25 项真实会话端到端检查执行于 2026-11-02，早于 2026-11-03 的“锁定读重查幂等键”修复。修复后重新执行的是 MySQL 8.0.44 全量 36 个自动化测试（含新增的并发同一 `clientRequestId` 用例），没有重跑这 25 项 E2E。关键修复点已有自动化覆盖；若需要“最终版本 E2E”证据，应在推送前或 CI 后补跑一次。

### 3.1 PingCode 用例库导入记录

- 时间：2026-11-03；测试库：`XXC`（校享测试库）。
- 方式：测试库“更多 → 导入 → 表格文件”，使用模板要求的表头；导入文件保存在 `docs/testing/XIA-5-XIA-6-test-cases-import.xlsx`。
- 预览结果：**验证成功，本次共可导入 43 个用例**，未通过 0 条。
- 导入后：测试库用例总数从 26 增加到 **69**；新增编号 **XXC-32 至 XXC-74**。
  - XIA-5：XXC-32 至 XXC-56，共 25 条。
  - XIA-6：XXC-57 至 XXC-74，共 18 条。
- 导入时“模块”列留空，且未勾选“自动创建新模块”，因此新增用例落在“无模块用例”（62 条 = 原有 19 条 + 新增 43 条）。如需按 XIA-5/XIA-6 分模块，可在用例库中新建模块后批量移动，或带模块名和编号重新导入覆盖。
- 11 条跨故事/工程用例（TC-CROSS-*）没有导入 PingCode，保留在 `docs/testing/XIA-5-XIA-6-test-cases.md` 作为工程检查记录。

## 四、验收标准对照

### XIA-5

| AC | 覆盖方式 | 结果 |
|---|---|---|
| 1. 选择开放、未占用、符合规则的时段，提交后生成归属该用户的有效预约，可查询结果，无需审批 | 创建成功测试 + 端到端创建/查询；状态为 RESERVED，无审批字段或步骤 | 通过 |
| 2. 同一资源已有有效预约时，另一用户提交重叠预约应拒绝，不产生第二个有效占用 | `rejectsOverlappingReservationFromAnotherUser` + 端到端冲突检查 | 通过 |
| 3. 同一资源重叠时段并发提交，最多一个成功，其他得到冲突 | MySQL 8.0.44 上的 `concurrentOverlappingRequestsLeaveAtMostOneActiveReservation` | 通过 |
| 4. 资源停用、教学占用或不开放时拒绝，不能因客户端显示滞后而接受 | `rejectsDisabledResource`、`rejectsTeachingBlock`、`rejectsTimeOutsideOpening` + 端到端检查 | 通过 |

### XIA-6

| AC | 覆盖方式 | 结果 |
|---|---|---|
| 1. 登录用户查看我的预约，只展示本人有权查看的预约及资源、时段、状态 | `listsOnlyOwnReservations...`、`anotherUserSeesOnlyTheirOwnReservation`、端到端 my-only-own | 通过 |
| 2. 预约取消、确认到馆或超时处理后，再次查询展示处理后的结果 | 演示数据包含 CANCELLED(NO_SHOW/USER_CANCEL)、CHECKED_IN；`filtersByStatus` 与端到端状态集合检查 | 通过 |
| 3. 用户没有预约时显示暂无预约记录 | `emptyStateReturnsEmptyItems` + teacher01 端到端空列表；前端 `MyReservationsView` 有“暂无预约记录”空状态 | 通过（接口与前端分支） |
| 4. 普通用户访问他人预约时拒绝，不泄露记录 | `cannotReadAnotherUsersReservation` + 端到端 404；实现统一返回 RESERVATION_NOT_FOUND | 通过 |

## 五、浏览器检查

- 首次用真实浏览器打开 `http://127.0.0.1:5173` 时，页面只渲染页头/页脚，主区域为空，URL 停在 `/`。
- DOM 检查发现入口 `src/main.ts` 仍是脚手架的旧版本（`createApp(App).mount('#app')`），没有安装 Vue Router，页面上残留未解析的 `<routerlink>`/`<routerview>` 元素。
- 修复为 `createApp(App).use(router).mount('#app')` 并加入 401 统一处理之后，页面正确跳转到 `/login?redirect=/resources`，登录表单、演示账号和页头/页脚均正常渲染。
- 之后尝试用浏览器工具点击登录，Playwright 点击持续超时；截图返回 `browser screenshot surface preparation timed out after 3000ms`。这是当前 IAB 工具表面不可用的限制，不是页面报错；因此没有完成登录→资源→预约→详情的真实点击走查，也没有可用的可视化截图证据。**不把 GUI 走查标记为通过。**
- 建议在浏览器工具恢复后补做：登录、资源列表、预约提交成功、冲突提示、我的预约列表/空状态、详情页、手机窄屏布局。

## 六、已知限制与未执行项

1. `auth` 是 XIA-1 落地前的最小会话登录，不是 XIA-1 的完整交付；正式登录、角色入口、会话过期和会话固定防护仍需 XIA-1 负责人替换/加固。
2. `resources` 是 XIA-4 的临时只读依赖，正式资源查询和可约接口应由 XIA-4 负责人替换。
3. 本地真实 MySQL 为 8.0.44；团队基线是 8.4 LTS。已提交 GitHub Actions（MySQL 8.4 service）配置，但本次未在 GitHub 上实际运行。
4. 未实现 XIA-7 取消、XIA-8 签到/签退、XIA-11 座位超时任务；XIA-11/14 只接入了创建预约所需的 `ResourceAvailabilityGuard` 及期限规则。
5. 未执行移动端真机检查、未执行 XIA-5/6 的 PingCode 验收标准逐条人工演示（AC 文本已按用户提供内容实现并映射）。
6. 未合并到 `main`，未请求另一名成员评审；PR 尚待创建。

## 七、AI 使用记录

本次使用 ZCode 阅读三份协作约定、XIA-9/10 与 XIA-11/14 分支、生成后端/前端代码、数据库脚本、OpenAPI、测试和 CI 配置，并在本机执行 H2、MySQL 8.0.44、真实会话端到端检查和前端构建。浏览器 DOM 检查发现并修复了 `main.ts` 未安装路由的问题。所有实际结果以上文为准；未把模拟数据、接口检查或工具未完成的操作写成已通过。
