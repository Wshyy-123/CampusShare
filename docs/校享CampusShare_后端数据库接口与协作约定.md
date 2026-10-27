# 校享 CampusShare 后端、数据库、接口与开工协作约定

版本：V1.1｜日期：2026-10-27｜适用对象：全体成员，重点供后端开发及前后端联调使用。

本文件与《校享CampusShare_前端开发约定.md》配套，提供后端工程、数据库、登录、接口、联调与开发协作的统一方案。业务规则以产品范围和对应PingCode工作项的验收标准为准。

## 一、方案总览

| 事项 | 方案 |
|---|---|
| 后端语言与框架 | Java 21 + Spring Boot 3.5.16 |
| 数据库 | MySQL 8.4 LTS；每人本地开发，后续共同联调 |
| 前端 | Vue 3 + TypeScript + Vite，npm，参考Vue Bits视觉方向 |
| 后端结构 | 一个Spring Boot单体应用，按业务分包 |
| 构建 | Maven 3.9.x + Maven Wrapper，工程提交Wrapper配置 |
| 数据访问 | MyBatis-Spring-Boot-Starter 3.0.5 |
| 认证 | Spring Security + 服务端Session + HttpOnly Cookie |
| 密码存储 | Spring Security PasswordEncoder，使用bcrypt |
| 数据库变更 | 顺序编号SQL脚本，合并前检查，记录每次应用版本 |
| 接口文档 | 仓库docs/api/维护OpenAPI文件，模拟数据按同一契约生成 |
| 联调 | 浏览器同源访问前端，/api通过代理访问后端 |

Spring Security、Spring Framework、MySQL驱动等由Spring Boot依赖管理统一版本，不由每位成员分别填写版本。Spring Boot与MyBatis Starter版本写入pom.xml，Maven版本写入Wrapper配置。

所有成员使用同一份工程、依赖配置和数据库脚本；初始化工程执行依赖解析、构建、登录与数据库连接检查。

## 二、后端工程与编码方式

### 1. 工程组织

根包使用com.campusshare，前后端放在同一个GitHub仓库的frontend/和backend/目录，说明和接口放docs/。

```text
backend/
  pom.xml
  mvnw / mvnw.cmd / .mvn/
  src/main/java/com/campusshare/
    common/           统一响应、异常和通用工具
    config/           安全、时间和其他应用配置
    auth/             登录、当前身份与退出
    user/             用户与角色
    resource/         资源、开放和占用安排
    reservation/      预约及状态变更
    attendance/       扫码、刷卡确认与签退
    violation/        违规记录
    job/              后台超时检查
  src/main/resources/
    mapper/           MyBatis XML
    application.yml   共享非敏感配置
  src/test/
docs/
  api/                接口契约
  database/           建表、初始化和变更脚本
  decisions/          业务与技术决定
```

每个业务包按需要设置controller、service、mapper、dto、vo、entity等子包，保持同一组织方式。Controller接收请求和校验输入，Service处理业务与事务，Mapper访问数据库。状态改变、超时释放和权限检查不散落在多个Controller或定时任务中。

### 2. 共用规则

- Java类使用PascalCase，方法和变量使用camelCase；数据库表与列使用snake_case。
- 输入DTO、输出VO与数据库实体分别表达各自用途，不直接返回包含密码摘要等字段的实体。
- 统一处理参数错误、未登录、无权限、业务冲突和系统异常；日志保留定位信息，对外返回清楚且稳定的错误码。
- 预约、到馆、取消、停用、超时处理调用共用Service规则，避免不同入口使用不同判断。
- 可通过配置改变的端口、数据库连接和任务间隔集中管理；个人连接密码留在本地配置或环境变量。
- 只选一套数据库访问方式，避免在同一项目中由不同成员各自引入MyBatis、JPA等不同方案。

## 三、数据库统一方案

### 1. 环境与脚本

- 数据库使用MySQL 8.4 LTS，个人通过phpStudy管理本地数据库时使用同一版本基线。
- 开发数据库统一命名campusshare，使用InnoDB和utf8mb4；排序规则在建库脚本中统一。
- 每人从仓库运行同一份建表和初始化脚本，个人连接地址、端口、用户名和密码可以不同。
- 脚本依次命名001_schema.sql、002_demo_data.sql、003_具体变更.sql；已被他人应用的脚本不静默重写，后续变化新建脚本。
- 成员同步代码时同步数据库变更；记录本地已应用到哪个脚本版本，不能只在自己的数据库图形工具里改表。
- 演示数据采用虚构账号、资源和记录。真实凭据与真实人员数据不作为共享初始化数据提交。

### 2. 数据对象

数据库脚本、Java实体、接口类型使用相同的数据字典。

| 表 | 职责 | 关键字段 |
|---|---|---|
| app_user | 登录账号和角色 | id、account、password_hash、display_name、role、enabled |
| resource | 一个最小可预约单位 | id、name、location、resource_type、unit_label、enabled |
| resource_opening | 允许预约的开放安排 | id、resource_id、start_at、end_at |
| resource_block | 教学占用和停用等不可约安排 | id、resource_id、block_type、start_at、end_at、reason |
| reservation | 本人预约及当前状态 | id、user_id、resource_id、client_request_id、start_at、end_at、accepted_at、arrival_deadline、status、cancel_reason、check_in_at、check_out_at、created_at、updated_at |
| reservation_event | 预约操作与处理历史 | id、reservation_id、event_type、operator_id、occurred_at、request_id、reason |
| violation_record | 违规记录 | id、reservation_id、user_id、violation_type、occurred_at |

resource一行对应一张桌、一块场、一张座位或一整间房。资源类型与具体预约单位分别存储，预约记录关联具体resource_id。

resource_type使用TABLE_TENNIS、BADMINTON、TENNIS、LIBRARY_SEAT、DISCUSSION_ROOM、CLASSROOM；role使用STUDENT、TEACHER、RESOURCE_ADMIN。前后端共用同一份编码字典。

开放安排按具体日期时段维护，教学占用使用resource_block记录。

### 3. 字段、索引与时间

- 数据库ID使用BIGINT；对外JSON中的ID统一用字符串，避免前端整数精度和类型不一致。
- account建立唯一约束；预约按resource_id和时间范围建立合适索引，按user_id支持“我的预约”查询。
- 创建预约的client_request_id非空，对user_id、client_request_id建立联合唯一约束；相同标识的请求仍需核对参数一致性。
- 违规记录对reservation_id、violation_type设唯一约束，防止后台重跑重复记同类违规。
- 示例数据、脚本、Java时间处理和对外接口统一使用Asia/Shanghai业务口径。采用DATETIME(3)保存业务本地时间，所有外部带偏移时间先由服务端转换至同一口径；时间字段含义写入数据字典。
- accepted_at表示服务端确认预约成功的计时基准，不取客户端时间；回滚的预约不成为有效记录。
- 座位arrival_deadline从accepted_at起算30分钟；体育从预约start_at起算15分钟。
- 教学、停用、预约的历史保留，不通过删除历史记录实现释放。

### 4. 预约状态

| 状态 | 含义 | 占用含义 |
|---|---|---|
| RESERVED | 已预约，尚未确认到馆 | 对预约时段构成有效占用 |
| CHECKED_IN | 已确认到馆 | 对预约时段仍构成有效占用；不直接等于已开始实际使用 |
| COMPLETED | 已按明确的结束规则完成 | 不继续形成有效占用，保留历史 |
| CANCELLED | 主动取消、未到处理或停用导致失效 | 解除该预约占用，记录具体原因 |

失效原因使用USER_CANCEL、NO_SHOW、RESOURCE_DISABLED编码。状态变化保存操作时间、操作者和原因，历史记录可追溯。

## 四、并发、事务与后台处理

### 1. 同一资源的写入采用共同顺序

在数据库事务内锁定资源行，处理顺序如下：

1. 验证登录和请求格式，确定需要操作的资源。
2. 开始事务，先锁定对应resource行，再按固定顺序读取或锁定相关预约。
3. 重新检查开放、教学占用、停用和预约冲突；预约冲突检查使用当前锁定读，不依赖先前普通查询的旧快照。
4. 在同一事务内写预约或状态变更，并保存所需事件、违规记录。
5. 成功后提交，失败时回滚；提交成功后再对外返回成功。

创建、取消、签到、签退、超时处理及新增占用/停用等会改变可用性的入口遵循同一锁顺序。涉及多个资源时按ID固定顺序加锁，减少死锁风险。数据库异常和重试要有边界，不把锁超时伪装成预约成功。

并发控制在后端事务与数据库中实现，前端禁用提交按钮仅用于改善操作体验。锁定读的事务要求参见[MySQL官方说明](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)。

### 2. 重叠时段和重复请求

同一资源行锁下，对全部有效占用和阻塞安排使用共用冲突检查。请求开始必须早于结束；输入校验依据对应资源的预约规则执行。

预约请求携带clientRequestId，由前端为同一次提交生成并在重试时复用。服务端按当前用户和clientRequestId识别同一创建请求；重复且参数相同返回同一预约，参数不一致返回冲突。数据库约束和事务共同支持，不能只依赖内存缓存。

重复取消、重复签到或后台重跑，不重复释放、不重复记违规，不影响后来新建的预约。具体成功返回和状态冲突结果写入接口契约。

### 3. 后台超时与签到并发

- 使用Spring定时任务轮询到期候选，扫描间隔通过配置管理。
- 扫描只是找候选，真正处理时仍在事务中按共同锁顺序读取最新状态并复核期限。
- 已有效确认到馆的预约不因原到馆期限被取消；已取消的预约不被迟到签到恢复。
- 签到与超时任务共用服务端时间口径和期限判断逻辑。
- 启动或任务恢复后检查遗漏的到期预约；任务异常有日志，不能因为用户关闭页面就停止释放。
- 自动取消、释放和违规记录作为一项一致操作处理，任一步失败按事务回滚或可追踪的补偿策略处理，不留下半完成状态。

## 五、登录和权限

### 1. 认证方式

首版预置学生、教师、资源管理员演示账号，由Spring Security校验账号和密码，建立服务端Session。浏览器通过HttpOnly会话Cookie携带身份凭证；前端不把用户ID或角色选择当作已认证身份。

密码采用PasswordEncoder生成bcrypt摘要。当前用户及角色由后端返回，服务端对每次受保护请求验证角色和记录归属；前端隐藏按钮只改善体验，不代替权限检查。

会话无操作超时设置为30分钟，使用配置管理。登录失效不改变预约业务状态。

### 2. 前后端共用登录流程

1. 前端先请求GET /api/v1/auth/csrf获取CSRF令牌及请求头名称。
2. POST /api/v1/auth/login提交账号密码和相应CSRF请求头，后端认证成功后设置会话Cookie，并返回当前身份。
3. 后续写请求携带当前CSRF令牌，浏览器携带会话Cookie；GET /auth/me用于恢复当前登录展示。
4. 登录成功或退出后重新获取令牌；统一处理401登录失效和403权限/CSRF失败，避免无限重试。
5. POST /auth/logout由后端使会话失效，清除前端身份展示。

登录请求使用JSON，成功和失败均返回共同JSON结构。Spring Security配置对应的JSON认证处理和结果返回，认证成功后持久化安全上下文。

开发和部署优先使用同源代理。生产使用HTTPS，Cookie的HttpOnly、Secure、SameSite和有效范围按环境正确配置；会话方案保留CSRF防护。实现依据见[会话管理](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html)、[密码存储](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html)和[CSRF](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html)。

### 3. 角色权限边界

| 操作 | 学生/教师 | 资源管理员 |
|---|---|---|
| 查询资源和时段 | 查询可预约资源 | 按管理权限访问 |
| 创建与查询个人预约 | 操作本人数据 | 按管理权限访问预约记录 |
| 体育取消、签到、签退 | 操作本人有权处理的预约 | 不默认可替任意用户签到 |
| 资源、开放和教学占用维护 | 拒绝管理操作 | 按实际管理权限处理 |
| 资源停用 | 拒绝 | 按实际管理权限处理 |

## 六、接口契约

接口说明统一维护在docs/api/，页面、模拟数据、后端和测试使用同一份契约。

### 1. 共同格式

- 前缀/api/v1，JSON字段用camelCase，业务ID用字符串，JSON请求和返回使用UTF-8。
- 时间使用带偏移的ISO 8601字符串，例如2026-10-28T14:00:00+08:00，服务端统一解释时区。
- 查询和一般操作成功返回HTTP 200，创建预约成功返回201，code为OK；错误用正确HTTP状态和稳定业务code，不把全部失败包装为HTTP 200。
- 分页page从1开始，默认size为20，上限100；返回items、total、page、size。
- 每个响应返回服务端追踪号requestId，用于定位一次接口调用；创建预约请求中的clientRequestId用于识别同一次业务提交，重试时保持不变。两者分别命名，不能混用。
- 服务器不返回密码摘要、会话值、数据库连接信息或内部堆栈。

### 2. 主要接口清单

| 方法 | 路径（省略/api/v1） | 用途/关联故事 |
|---|---|---|
| GET | /auth/csrf | 获取CSRF信息，可未登录访问 |
| POST | /auth/login | 登录，XIA-1 |
| GET | /auth/me | 当前用户和角色，XIA-1 |
| POST | /auth/logout | 退出 |
| GET | /resources | 按类型、位置等筛选资源，XIA-4 |
| GET | /resources/{resourceId} | 资源详情，XIA-4 |
| GET | /resources/{resourceId}/availability | 指定日期可约信息，XIA-3/4 |
| POST | /reservations | 创建预约，XIA-5 |
| GET | /reservations/mine | 我的预约，XIA-6 |
| GET | /reservations/{reservationId} | 有权限的预约详情，XIA-6 |
| POST | /reservations/{reservationId}/cancel | 体育预约取消，XIA-7 |
| POST | /reservations/{reservationId}/check-in | 体育扫码确认，XIA-8 |
| POST | /reservations/{reservationId}/check-out | 体育扫码签退，XIA-8 |
| POST | /admin/resources | 新增资源，XIA-2 |
| PUT | /admin/resources/{resourceId} | 修改允许维护的资源信息，XIA-2 |
| POST | /admin/resources/{resourceId}/openings | 维护开放安排，XIA-3 |
| POST | /admin/resources/{resourceId}/teaching-blocks | 维护教学占用，XIA-3 |
| POST | /admin/resources/{resourceId}/disable-periods | 按明确范围停用并处理受影响预约，XIA-14 |

上表列出主要业务入口。查询及维护接口的参数、返回字段、权限和错误例子一并记录在docs/api/。

XIA-9、XIA-11的自动处理在后台执行，不设置一个必须由前端点击才释放的业务接口。测试可调整受控测试数据与时间条件验证，不能把测试入口无条件开放给普通用户。

### 3. 创建预约示例

下列是文档示例，不是线上数据或已执行结果。身份从当前会话识别，请求不传可冒用的userId。

```json
{
  "resourceId": "101",
  "startTime": "2026-10-28T14:00:00+08:00",
  "endTime": "2026-10-28T15:00:00+08:00",
  "clientRequestId": "example-create-reservation-001"
}
```

成功响应示例：

```json
{
  "code": "OK",
  "message": "预约成功",
  "data": {
    "reservationId": "1001",
    "resourceId": "101",
    "status": "RESERVED",
    "startTime": "2026-10-28T14:00:00+08:00",
    "endTime": "2026-10-28T15:00:00+08:00"
  },
  "requestId": "example-trace-001"
}
```

### 4. 共同错误码

| HTTP | code | 前端处理 |
|---|---|---|
| 400 | INVALID_ARGUMENT | 显示具体参数问题 |
| 401 | AUTH_REQUIRED | 提示重新登录 |
| 403 | FORBIDDEN | 显示无权限，不伪装为空列表 |
| 403 | CSRF_INVALID | 刷新令牌后按流程处理，不无限重试 |
| 404 | RESOURCE_NOT_FOUND / RESERVATION_NOT_FOUND | 提示对象不存在或不可访问 |
| 409 | RESERVATION_CONFLICT | 显示时段已被占用并刷新可约信息 |
| 409 | RESOURCE_UNAVAILABLE / INVALID_STATE | 显示停用、状态不符等具体原因 |
| 409 | IDEMPOTENCY_CONFLICT | 同一创建请求标识对应了不同参数 |
| 500 | INTERNAL_ERROR | 显示稍后重试并保留追踪号 |

无权限与不存在的外部返回需要共同检查是否泄露他人的记录信息。业务冲突、网络失败和系统错误分别处理，前端不能在错误分支继续显示预约成功。

## 七、联调环境与手机扫码

### 地址安排

| 场景 | 安排 |
|---|---|
| 本人电脑开发 | Vite端口5173，Spring Boot端口8080，可通过配置调整 |
| 浏览器请求 | 使用相对路径/api/v1/...，由Vite开发代理转发到后端 |
| 共同联调 | 使用同一套前端、后端、数据库和脚本版本，联调地址集中写入环境配置 |
| 手机访问 | 使用手机实际能访问的联调域名或开发机器地址，二维码不能写手机自身的localhost |
| 演示与部署 | 优先同一HTTPS站点对外服务，/api反向代理至后端 |

个人本地开发与共同联调使用不同数据库配置和演示数据。配置示例可入库，真实密码留在各自环境中。联调地址改变时由指定维护者统一通知并重新生成受影响二维码。

### 扫码与模拟刷卡

体育签到、签退使用手机真实扫码，图书馆到馆通过模拟刷卡事件验证。模拟事件关联演示用户和对应预约，由后端执行身份、状态与时间校验。

二维码携带资源或入口标识，最终由后端结合当前用户、有效预约、资源与时间判断。识别二维码或打开页面不直接等于签到成功；签到和签退采用明确写请求，展示接口处理结果。

若采用网页内调用摄像头扫描，应按浏览器要求准备安全上下文和权限提示。手机通过普通局域网HTTP地址打开页面，不能当然认为满足摄像头调用要求；需要实际手机验证。[浏览器接口说明](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia)

## 八、业务规则与实现要求

- 学生、教师使用平台查询和预约，当前预约流程无审批。
- 同一排他资源的重叠时段只能存在一个有效预约，教学占用时段不可预约。
- 座位预约成功后30分钟内通过刷卡确认到馆；不允许用户主动取消；超时未到自动释放并记录违规。
- 体育场地使用前可取消；到馆扫码签到，预约开始后15分钟未签到自动取消、释放并记录违规；使用结束扫码签退。
- 取消、超时释放保留历史记录，后台处理不依赖用户打开或刷新页面。
- 违规保留记录，不增加扣分、罚款或禁约处理。
- 首轮优先处理XIA-1至XIA-11及XIA-14，讨论室和教室专项故事XIA-12、XIA-13放在后续范围。

## 九、Sprint计划、分工与完成标准

### 1. 本轮范围与计划方式

PO提出先做XIA-1至XIA-11及XIA-14共12条最高优先级故事。这个方向继续保留，开发人员估算后决定实际可完成工作，不用优先级代替容量判断。

Sprint计划记录目标、时间、故事、任务及负责人。体育和座位的估算分别包含到馆、取消、超时、违规记录与真实联调工作。

### 2. 协作职责

- 前端：统一页面与组件、共用布局和请求封装，按页面与功能拆分任务。
- 后端：按账号与权限、资源和安排、预约与并发、到馆和后台任务拆分工作。
- 数据库和状态：指定一名牵头维护者，所有后端共同评审；共享结构通过PR和脚本更新。
- 接口：每个故事明确前后端对接人及评审人；涉及共享状态和可用性逻辑的改动同时通知相关开发人员。
- SM促进计划和障碍处理，PO确认业务取舍；开发人员自行决定具体实现和任务分配。

### 3. 完成标准（DoD）

1. 本故事的验收标准明确，正常、边界、异常和权限场景按实际范围执行检查。
2. 前后端相关代码通过另一人评审；类型检查、构建、相关自动化测试通过。
3. 使用真实后端和数据库完成主流程联调；模拟数据验证不能独立代表故事完成。
4. 手机和电脑上的关键操作可用；扫码使用真实手机验证。
5. 对预约冲突、到馆超时与重复请求执行相关一致性验证，保留真实结果。
6. 无影响本轮主流程的严重未解决缺陷；已知限制有明确记录和处理决定。
7. 工作项、接口、脚本、必要文档和实际AI使用记录同步更新。

Sprint内按完成标准记录实际进度，完成代码编写后继续完成评审、联调和验证。

### 4. 本轮必须重点验证的场景

| 场景 | 要验证的结果 |
|---|---|
| 两人并发预约同一资源重叠时段 | 最多一个有效占用成功，其他得到冲突结果 |
| 资源停用与预约提交同时发生 | 按统一事务规则产生一致结果，不出现停用生效后无效的新预约 |
| 到馆确认与超时任务同时处理 | 按已确定边界产生一个一致结果，不既保留预约又记未到违规 |
| 后台重复扫描 | 不重复违规、释放或影响后来预约 |
| 用户关闭页面 | 后台仍按规则处理超时 |
| 重复提交、迟到扫码/刷卡 | 不重复建预约、不恢复已失效占用 |
| 普通用户请求他人预约或管理接口 | 服务端拒绝无权限操作，不泄露个人记录 |
| 15/30分钟到馆期限 | 页面、接口与后台使用相同的计时基准和判断逻辑 |

执行验证时保留输入条件、实际结果和发现的问题。

## 十、开工顺序与要留下的产物

| 顺序 | 工作 | 形成的产物 |
|---|---|---|
| 1 | 使用统一的工程结构、依赖配置和数据字典 | 共用工程配置与技术说明 |
| 2 | 根据估算确定Sprint目标、日期、可交付故事和必要初始化任务 | Sprint计划与任务分工 |
| 3 | 后端统一工程、数据库最小结构和演示账号 | 可共同拉取启动的工程、脚本和配置示例 |
| 4 | 前后端共同定稿登录、当前身份、资源查询和预约接口 | 同一份接口契约和成功/失败例子 |
| 5 | 尽早跑通一个页面调用后端并访问数据库，验证会话和权限 | 真实联调记录 |
| 6 | 按任务分支并行开发，持续集成并及时联调 | 工作项、PR和检查结果 |
| 7 | 按DoD测试、演示，收集反馈并回顾改进 | 实际测试、评审和回顾记录 |

初始化只做到本轮必要程度。人员与日期记录在Sprint计划和工作项中，本文集中维护开发规范。

## 参考依据

- 当前项目的产品范围、14条PB及44条验收标准、前端开发约定和开发协作约定。
- [Spring Boot 3.5系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)：用于版本与Java/Maven要求核对。
- [MyBatis Starter兼容表](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)和[3.0.5发布记录](https://github.com/mybatis/spring-boot-starter/releases/tag/mybatis-spring-boot-3.0.5)：用于依赖组合的文档核对。
- [Spring Security 6.5会话管理](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html)、[密码存储](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html)、[CSRF防护](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html)：用于认证方案与SPA联调要求。
- [MySQL 8.4锁定读](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)：用于事务与锁顺序说明。
- [浏览器摄像头接口说明](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia)：用于网页内扫码的环境要求。

