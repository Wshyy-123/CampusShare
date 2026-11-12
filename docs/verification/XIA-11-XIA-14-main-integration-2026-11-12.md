# XIA-11 / XIA-14 主工程接入与验证

日期：2026-11-12。基于 main `5955133`，功能分支 `feature/XIA-11-XIA-14-integrate`。仅完成 XIA-11 座位超时处理及 XIA-14 场地停用；依据主工程集成要求、前端基准与页面扩展指南和已确认的停用规则。

## 接入结果

- 主后端 `campus-share-backend/`：`resource` 包增加停用控制器/请求/结果/事务服务，`reservation.service` 增加座位未到事务服务，`job/SeatNoShowJob` 接入主应用调度。迁移逻辑来源为 `backend/reservation-lifecycle`，来源应用保持原状，无新增生产应用。
- 复用共享 Session、CSRF、CurrentUserProvider、统一响应/异常、RequestIdContext、LifecycleMapper、lifecycleClock 及共享表。真实权限按数据库当前角色和启用状态校验，不相信前端传来的用户身份。
- 主前端 `frontend/src`：复用现有“时段管理”路由、共享主题及 useCampus；新增停用弹窗和真实接口适配。“我的预约”复用 StatusBadge 显示 RESOURCE_DISABLED / NO_SHOW，取消后隐藏到馆期限与使用提示。
- API 模式遇到错误不回退演示数据；提交中禁用重复提交，冲突保留表单和错误；提交成功后刷新预约/停用列表和可用时段，刷新失败明确提示“已成功，无需重复提交”。写入前的旧可用性响应不能覆盖停用结果。
- 演示模式不写真实停用；教学/外部占用写操作维持原接入边界，没有扩展其他人的任务。

## 规则和事务

XIA-14：停用区间为 `[startTime, endTime)`，取消重叠且仍为 RESERVED/CHECKED_IN 的未使用预约。已签到且当前时间到达预约开始时刻，即判为正在使用；存在一条此类重叠预约就返回409，整次停用不生效。提前签到但尚未到预约开始时间可以取消。原预约、取消原因 RESOURCE_DISABLED、操作者及事件均保留。resource_block 独立保存停用区间和管理员填写的原因；取消预约不会删除停用、教学或外部占用。没有受影响预约也会保存停用区间。

XIA-11：只处理 LIBRARY_SEAT，判定为 RESERVED、check_in_at 为空且服务器时间 `>= accepted_at + 30分钟`。释放以 CANCELLED/NO_SHOW 表示，在同一事务插入一条违规和取消事件，不删除预约。已确认到馆、已结束、已取消及其他类型均跳过。后台任务默认启用，启动立即补查，默认每轮结束30秒后再查；不依赖页面或管理员操作。扫描用100条一批的主键分页，单条失败回滚，继续后续记录，下轮重试失败项。

创建预约、停用和超时都先锁 resource，再锁相关 reservation；等待锁后复核状态和时间。未来接入刷卡的负责人应继续遵守该锁顺序及期限校验，并原子写入 CHECKED_IN/check_in_at。

## 数据库与启动

不新增迁移，不修改已应用的001至005：共享003已包含 resource_block、reservation_event、violation_record 及预约字段/唯一约束，005已处理讨论室期限。已有主工程数据库按原预约接入说明应用到005即可。

后端保持原数据库环境变量 `CAMPUSSHARE_DB_URL / CAMPUSSHARE_DB_USERNAME / CAMPUSSHARE_DB_PASSWORD`，启动原主工程。新增可选配置：

| 配置 | 默认 | 用途 |
|---|---|---|
| CAMPUSSHARE_SEAT_NO_SHOW_ENABLED | true | 主应用启用座位未到任务 |
| CAMPUSSHARE_SEAT_NO_SHOW_DELAY_MS | 30000 | 两轮扫描间隔；截止时刻即满足条件，实际处理取决于下一次扫描及锁等待 |

前端设 `VITE_DATA_MODE=api`；后端非8080时沿用 `VITE_API_PROXY_TARGET`。管理员登录后进入“时段管理 → 停用场地”，选择资源、北京时间起止及最多500字原因。接口契约见 `docs/api/lifecycle.yaml`。

## 实际验证

环境：Windows、Java21、Node.js24.22.3、MySQL8.4.13；独立测试实例127.0.0.1:3307，未操作共享或他人的数据库。自动测试使用 campusshare_test，实际应用使用由原001至005初始化的独立 campusshare。

1. 主后端完整 `mvnw.cmd -B verify`（配置测试数据源为MySQL8.4）：**60 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS**。包括原44项回归和新增16项：期限前1毫秒/正好30分钟、已到馆保护、整次停用冲突、边界不重叠、保留历史、401/403/CSRF、事务回滚、两任务并发、创建与停用并发、等待锁后复核、超过100条分页、失败重试以及真实 Spring 调度无需HTTP自动执行。
主工程默认 H2 测试方式另执行 `mvnw.cmd -B test`：60项，0失败，0错误，按原项目条件跳过3项仅MySQL适用的并发测试；这3项已在上述MySQL运行中全部通过。

2. 前端 `npm ci`、`npm run check:baseline`、`npm run check:api`、`npm run build`：通过。check:api 已纳入 check-lifecycle.mjs，覆盖带CSRF的停用请求、BIGINT字符串ID、403/409/500不伪造成功、旧查询刷新竞争、取消原因展示和401清理。该层使用模拟HTTP，不冒充真实服务端验证。
3. 主应用实际启动：原 Spring Boot jar 8081 + Vite API模式5173 + MySQL8.4，沿用真实 Session/CSRF 登录。浏览器学生一创建102座位次日10:00—11:00预约#9，管理员经停用表单提交，页面返回自动取消1条；数据库和用户页面均保留该预约并显示 RESOURCE_DISABLED。另一账号提交该时段仍得到409 RESOURCE_UNAVAILABLE。
4. 实际后台任务：经真实接口创建座位预约#10，核对 acceptedAt 到 arrivalDeadline 为30分钟，再仅调整独立测试记录的 accepted_at 到31分钟前以加速验证。没有调用手动触发接口或浏览器计时器；后台自动转 CANCELLED/NO_SHOW，违规及取消事件各1条，多轮扫描无重复；学生二随后同段预约#11成功。已确认到馆的记录跨期限保持 CHECKED_IN 且无违规。
5. 实际HTTP权限：未登录401、学生停用403、管理员无CSRF403；以数据库夹具构造正在使用记录后，停用409，区间数量及使用状态不变。
6. Chrome实际页面：学生/管理员切换、管理员菜单及停用提交、本人列表显示“资源停用”“未到超时”、学生直接访问管理路由被拒绝，浏览器未捕获console error。桌面及390×844视口检查；修复了弹窗日期字段的横向溢出，修复后弹窗 clientWidth=scrollWidth=340px。已恢复浏览器原视口。

截图保存在本次开发会话本地输出：XIA-14-mobile-form.jpg、XIA-11-XIA-14-results.jpg。不将机器绝对路径、构建产物或数据库凭据提交到仓库。

## 验证边界与协作

- 本次到馆保护使用数据库夹具/并发事务验证共享状态兼容性；未实现或冒称完成其他工作项的真实刷卡/扫码流程、离场和体育/教室结束释放。
- 超时实际联调缩短扫描间隔至1秒并调整测试记录时间；没有等待现实30分钟。精确30分钟判定由固定时钟测试覆盖，生产默认仍30分钟期限/30秒扫描。
- 手机验证是Chrome 390×844响应式视口，不是真机扫码或实物刷卡验证。
- 代码、测试和记录由AI辅助实现与自查；未将自查写成人工团队验收。PR仍需按团队约定由另一位开发者审核后合并。
