# XIA-5 / XIA-6 验证记录

- 执行日期：2026-11-02 至 2026-11-10（Asia/Shanghai）
- 执行位置：`campus-share-backend/`、`frontend/`（本地）
- 对应故事：XIA-5 创建预约；XIA-6 我的预约与详情
- 关联测试计划/用例：`docs/testing/XIA-5-XIA-6-test-plan.md`、`docs/testing/XIA-5-XIA-6-test-cases.md`
- 结论：预约模块已按 V1.2 合入主后端 `campus-share-backend/`；H2 与本地 MySQL 8.0.12 自动化测试通过；主后端 + 前端 + 真实 MySQL 的 26 项端到端检查全部通过。浏览器 GUI 点击仍受工具表面超时限制，未标记通过。

## 一、实际环境和命令

| 项目 | 实际值 |
|---|---|
| JDK | Microsoft OpenJDK 21.0.9 |
| Spring Boot / MyBatis | 3.5.16 / 3.0.5 |
| 主后端 | `campus-share-backend/`，包 `org.campus.campussharebackend` + `com.campusshare` |
| 默认测试库 | H2 2.x，MySQL 模式 |
| 本地 MySQL | 127.0.0.1:3306，8.0.12；测试库 `campusshare_xia56_test`，演示库 `campusshare` |
| 前端 | Node 22.23.1、Vue 3.5.44、Vite 8.3.5、TypeScript 6.0.3 |
| 账号密码 | 通过本地环境变量/命令行传入，不写入仓库；文档不记录真实密码 |

```powershell
cd campus-share-backend
$env:JAVA_HOME = 'D:\Program Files\Java\jdk-21.0.9'
.\mvnw.cmd test

# 本地 MySQL 8.0.12（独立测试库，不覆盖 campusshare）
.\mvnw.cmd test `
  -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/campusshare_xia56_test?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" `
  -Dspring.datasource.username=root `
  -Dspring.datasource.password=你的本地密码 `
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

cd ../frontend
npm run build
```

数据库脚本按顺序执行（`campus-share-backend/docs/database/`）：

1. `001_schema.sql`
2. `002_demo_data.sql`
3. `003_xia5_xia6_core.sql`
4. `004_xia5_xia6_demo.sql`

`003` 扩展主工程原有表并新增开放、占用、预约历史结构；`004` 增加 V1.2 的教室、外部占用和状态演示数据。已在本地 MySQL 8.0.12 实际执行成功。

## 二、自动化测试结果

| 运行 | 结果 |
|---|---|
| H2：`mvn test` | **37 个测试，0 失败、0 错误、2 跳过**；跳过的是仅在 MySQL 执行的行锁并发测试 |
| MySQL 8.0.12：`mvn test`（参数覆盖数据源） | **37 个测试，0 失败、0 错误、0 跳过**；两个并发测试实际执行 |
| 前端：`npm run build` | `vue-tsc -b` 通过；Vite 构建成功 |

测试类：

- `AuthIntegrationTest`：会话登录、`/auth/me`、退出、错误密码、CSRF 缺失、匿名访问。
- `ReservationCreateIntegrationTest`：座位/体育创建、座位 30 分钟、体育 15 分钟、重叠冲突、幂等重放/异参冲突、教学占用、外部占用、开放时段外、资源停用、非法时间、开始=结束、缺少 `clientRequestId`、UTC→上海、未登录、并发重叠、并发同一 `clientRequestId`。
- `ReservationQueryIntegrationTest`：只返回本人、状态筛选、分页、时间筛选、空状态、本人详情、他人详情 404、不存在 404、非法状态/分页。
- `ResourceIntegrationTest`：资源列表/筛选、指定日期开放/阻塞/已有预约、资源不存在、匿名访问。
- `CampusShareBackendApplicationTests`：主工程上下文加载。

关键断言：

- 新建预约 201、状态 RESERVED，写 `reservation_event(CREATED)`。
- 同 `clientRequestId` 同参重放 200 同一预约；异参 409 `IDEMPOTENCY_CONFLICT`。
- 座位 `arrival_deadline = accepted_at + 30 分钟`；教室/体育 `start_at + 15 分钟`。
- 两用户并发重叠：一个成功、一个 409，数据库只有一条有效占用。
- 并发同一 `clientRequestId`：一个 NEW、一个 REPLAY，返回同一预约号。
- 教学占用、体育外部占用、停用、非开放时段均 409 `RESOURCE_UNAVAILABLE`。
- 本人列表不含他人记录；他人详情统一 404。

### 2.1 发现并修复的问题

1. **并发同一 `clientRequestId` 误报冲突**：MySQL REPEATABLE READ 下普通查询使用旧快照；改为拿到资源锁后用 `SELECT ... FOR UPDATE` 重查幂等键，MySQL 复测通过。对应 `ReservationRepository.lockByUserAndClientRequestId`。
2. **主工程旧 `ReservationMapper` 与我们的 Mapper Bean 同名**：我们的 Mapper 改名为 `ReservationRepository`，XML namespace 同步；主启动类统一 `@MapperScan` 两个包。
3. **主工程旧 `SportsTimeoutTask` 不符合 V1.2**：它按全部 RESERVED 扫描，可能误取消座位；加 `campusshare.sports-no-show.enabled`，默认关闭，待 XIA-9 负责人改为座位/教室/体育分类后再启用。

## 三、主工程真实端到端联调

环境：本地 MySQL 8.0.12 的 `campusshare` 演示库；主后端 8080；前端 Vite 5173；经 Vite `/api` 代理；真实 Session + CSRF。

| 检查 | 实际结果 |
|---|---|
| CSRF 获取 / student01 登录 / `auth/me` | 通过；身份张三 / STUDENT |
| 资源列表 | 7 个资源（座位、羽毛球、乒乓球、网球、教室） |
| 座位 102 当天可约信息 | 1 条开放安排、已有演示预约 |
| 座位 102 D+1 15:00–16:00 创建 | 201，RESERVED；期限 30 分钟 |
| 同 `clientRequestId` 重放 | 200，同一预约号 |
| student02 同资源重叠 | 409 `RESERVATION_CONFLICT` |
| 同 `clientRequestId` 异参 | 409 `IDEMPOTENCY_CONFLICT` |
| 教室 501 教学占用 10:30–11:30 | 409 `RESOURCE_UNAVAILABLE` |
| 羽毛球 2 外部占用 18:30–19:30 | 409 `RESOURCE_UNAVAILABLE` |
| 座位 102 06:00–07:00 | 409 `RESOURCE_UNAVAILABLE` |
| 体育 202 预约 | 201；`arrivalDeadline = start + 15 分钟` |
| 教室 501 D+2 预约 | 201；`arrivalDeadline = start + 15 分钟` |
| 我的预约 | 只含本人；状态包含 RESERVED、CANCELLED、COMPLETED |
| 本人详情 | 200 |
| student02 查看 student01 预约 | 404 `RESERVATION_NOT_FOUND` |
| 缺少 CSRF 写请求 | 403 `CSRF_INVALID` |
| teacher01 我的预约 | 200，`items=[]`、`total=0` |
| 匿名访问我的预约 | 401 `AUTH_REQUIRED` |

以上 **26 项检查全部通过**，执行于主工程集成完成后的当前代码版本。

## 四、PingCode 用例库

### 4.1 2026-11-03 已导入 43 条

- 测试库 `XXC`；“更多 → 导入 → 表格文件”；预览 43/43 通过，0 未通过。
- 导入后总数 26 → 69；新增编号 **XXC-32 – XXC-74**。
  - XIA-5：XXC-32 – XXC-56（25 条）。
  - XIA-6：XXC-57 – XXC-74（18 条）。
- 模块列留空，新增用例位于“无模块用例”；未勾选“自动创建新模块”。
- 导入文件：`docs/testing/XIA-5-XIA-6-test-cases-import.xlsx`。

### 4.2 V1.2 新增用例补导（2026-11-10）

- 导入文件：`docs/testing/XIA-5-XIA-6-V1.2-new-cases-import.xlsx`（5 条）。
- 用例：TC-X5-026 教室预约创建成功、TC-X5-027 教室教学占用被拒绝、TC-X5-028 体育外部占用被拒绝、TC-X6-019 COMPLETED 状态展示、TC-X6-020 V1.2 状态与教室体育文案。
- 预览验证：**5/5 通过，0 未通过**；导入后测试库总数 **138 → 143**（期间其他成员也在补充用例）。
- 本次未逐条记录新增 XXC 编号；页面可按标题搜索确认。
- 跨故事/工程用例（TC-CROSS-*）保留在文档中，未导入 PingCode。

## 五、验收标准对照

### XIA-5

| AC | 覆盖方式 | 结果 |
|---|---|---|
| 1. 开放、未占用、符合规则时段创建有效预约并可查询 | 创建成功测试 + 端到端座位/体育/教室创建 | 通过 |
| 2. 重叠预约拒绝，不产生第二个有效占用 | `rejectsOverlappingReservationFromAnotherUser` + 端到端 | 通过 |
| 3. 并发重叠最多一个成功 | MySQL 8.0.12 上的并发测试 | 通过 |
| 4. 停用、教学占用、外部占用或不开放时拒绝 | 对应测试 + 端到端 | 通过 |

### XIA-6

| AC | 覆盖方式 | 结果 |
|---|---|---|
| 1. 只展示本人预约及资源、时段、状态 | 列表隔离测试 + 端到端 | 通过 |
| 2. 取消/到馆/正常结束后展示处理结果 | 演示数据包含 CANCELLED、CHECKED_IN、COMPLETED；端到端状态集合 | 通过 |
| 3. 无预约显示空状态 | 空列表测试 + teacher01 端到端 | 通过 |
| 4. 访问他人预约被拒绝且不泄露 | 他人详情 404 + 不存在 404 | 通过 |

## 六、浏览器检查

- 早先在 IAB 中检查登录页时，发现 `frontend/src/main.ts` 仍是脚手架旧入口，没有安装 Vue Router；修复后 `/login?redirect=/resources` 正常渲染。
- IAB 的输入/截图表面持续超时（`browser screenshot surface preparation timed out`），因此没有完成登录→预约→我的预约的真实点击走查，也没有可视化截图证据；**GUI 不标记通过**。
- 建议工具恢复后补做：登录、资源列表、座位/教室/体育预约、冲突/占用提示、我的预约状态、详情、手机窄屏。

## 七、已知限制与未执行项

1. `auth` 是 XIA-1 落地前的最小会话登录；`resources` 是 XIA-4 的临时只读依赖。
2. 主工程旧的 `CheckInController` 仍使用 `userId` 查询参数，不符合 V1.2 的会话身份+地点码要求；已通过 `campusshare.legacy-checkin.enabled=false` 默认关闭，待 XIA-8 负责人改造。
3. 旧 `SportsTimeoutTask` 已默认关闭，等待 XIA-9 负责人按资源类型改造。
4. 本地 MySQL 为 8.0.12，低于团队基线 8.4；GitHub Actions（MySQL 8.4）配置已更新，但尚未实际运行。
5. 未执行真实手机/二维码/刷卡硬件验证；未完成另一名成员评审；未推送/合并到 main。
6. V1.2 新增测试用例尚未重新导入 PingCode；GUI 点击走查仍受工具限制。

## 八、AI 使用记录

本次使用 ZCode 阅读 V1.2 业务规则、AGENTS.md、开发同步说明和主工程代码，执行 main 合并、将预约模块接入 `campus-share-backend/`、修复 Mapper 冲突、补充 003/004 数据库迁移、更新前端文案和接口显示，并在本机 H2、MySQL 8.0.12 和真实前后端环境执行测试。所有实际结果以上文为准；未把未执行或工具阻塞的检查写成已通过。
