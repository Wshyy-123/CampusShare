# XIA-7 / XIA-8 验证记录

执行日期：2026-11-03（Asia/Shanghai）。执行位置：本机 Windows，分支 `feature/XIA-7-XIA-8`（基于 `main` d4906b3）。

## 一、环境

| 项 | 实际值 |
|---|---|
| JDK | Amazon Corretto 21.0.13 |
| Spring Boot | 3.5.16（pom 既有版本） |
| 数据库 | MySQL 8.4.13 LTS，便携版运行在 `127.0.0.1:3307`，不是系统服务 |
| 前端 | Node 24.21.0 + pnpm 11.28.5（本机没有 npm，安装时未生成 pnpm-lock） |
| 浏览器 | Edge headless 截图 |

数据库脚本按 `001_schema.sql` → `002_demo_data.sql` → `003_sports_attendance.sql` → `004_sports_demo_data.sql` 顺序应用，四步均无报错。

## 二、验收标准对应

### XIA-7 体育预约取消

| 验收标准 | 验证方式 | 结果 |
|---|---|---|
| 1. 取消成功后自动解除占用并保留取消记录 | `POST /4/cancel` → `status=CANCELLED`、`cancelReason=USER_CANCEL`；`reservation_event` 中 `CANCEL` 1 条 | 通过 |
| 2. 已开始实际使用时不按使用前规则放行 | 已签到再取消 → 409；到达开始时间取消 → 409（单元 + 数据库用例各一条） | 通过 |
| 3. 重复提交不恢复占用、不产生重复预约 | 连续两次 `POST /4/cancel`：第二次返回 200、状态不变，`CANCEL` 事件仍为 1 条 | 通过 |

### XIA-8 体育扫码签到 / 签退

| 验收标准 | 验证方式 | 结果 |
|---|---|---|
| 1. 在可签到范围内签到并记录时间，可查询状态 | 8 号预约（开始于 1 分钟前）`POST /8/check-in` → `CHECKED_IN` 且写入 `checkInAt`；`GET /8` 可查到 | 通过 |
| 2. 结束后签退并记录时间，可查询使用记录 | 5 号预约（已签到）`POST /5/check-out` → `COMPLETED` 且写入 `checkOutAt`；`GET /5` 可查到 | 通过 |
| 3. 超时取消后再扫码不恢复原预约 | 后台把 3 号取消（`NO_SHOW`）后 `POST /3/check-in` → 409，状态仍为 `CANCELLED`，无 `CHECK_IN` 事件 | 通过 |

## 三、测试结果

### 1. 单元测试（不连数据库）

```
mvnw -B test -Dtest=SportsBookingRulesTest,SportsReservationServiceTest
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

覆盖：开始前 1 纳秒/开始时刻/开始后 15 分钟整的临界、提前签到窗口可配置、开始时间为空、非体育资源、他人预约 404、未登录 401 之外的状态冲突、重复取消/签到/签退的幂等、事件写入。

### 2. 真实 MySQL 联调测试

```
set MYSQL_TEST=true
set SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3307/campusshare?...
set SPRING_DATASOURCE_USERNAME=cs_test
set SPRING_DATASOURCE_PASSWORD=cs_test_pw
mvnw -B test -Dtest=SportsReservationIntegrationTest
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

覆盖：取消释放、开始后拒绝取消、座位预约不能走体育取消、他人预约返回 404、窗口内签到、提前签到拒绝、`NO_SHOW` 后签到拒绝且不恢复、超时任务不处理座位预约、签退完成。测试在事务内执行并回滚。

### 3. HTTP 接口联调（真实后端 + 真实数据库）

后端以 `dev` profile 运行在 8081（本机 8080 被 `ApplicationWebServer` 占用），前端经 Vite 代理访问。

| # | 请求 | 结果 |
|---|---|---|
| 1 | `GET /api/v1/reservations/4`（用户 1） | 200，`status=RESERVED`，`resourceType=BADMINTON` |
| 2 | `POST /4/cancel`（用户 1） | 200，`CANCELLED` / `USER_CANCEL` |
| 3 | 再次 `POST /4/cancel` | 200，状态不变；数据库中 `CANCEL` 事件仍为 1 条 |
| 4 | `POST /5/check-out`（用户 1） | 200，`COMPLETED`，`checkOutAt=2026-11-03T12:33:46.457+08:00` |
| 5 | `POST /7/check-in`（未到开始时间） | 409 `INVALID_STATE`，"未到可签到时间" |
| 6 | `GET /6`（用户 1 查用户 2 的预约） | 404 `RESERVATION_NOT_FOUND` |
| 7 | `GET /4`（用户 2 查用户 1 的预约） | 404 `RESERVATION_NOT_FOUND` |
| 8 | `POST /4/cancel`（不带身份） | 401 `AUTH_REQUIRED` |
| 9 | `GET /abc`（非法 ID） | 400 `INVALID_ARGUMENT` |
| 10 | `POST /8/check-in`（开始于 1 分钟前） | 200，`CHECKED_IN`，`checkInAt=2026-11-03T12:33:52.103+08:00` |
| 11 | `POST /3/check-in`（已被超时取消） | 409 `INVALID_STATE`，"预约已取消，不能签到" |

数据库终态：4 号 `CANCELLED/USER_CANCEL`、5 号 `COMPLETED`、8 号 `CHECKED_IN`、3 号 `CANCELLED/NO_SHOW`；事件表 `3/NO_SHOW=1`、`4/CANCEL=1`、`5/CHECK_OUT=1`、`8/CHECK_IN=1`；违规表 `2 号用户/3 号预约/SPORTS_NO_SHOW=1`。

### 4. 后台任务

服务启动后 10 秒内日志出现：

```
SportsNoShowService : 体育预约 3 超时未签到，已自动取消并记录违规
SportsTimeoutTask   : 本轮自动取消 1 条超时未签到的体育预约
```

图书馆座位预约（1 号资源）没有被该任务处理。

### 5. 前端

```
pnpm run build   # vue-tsc -b && vite build
EXITCODE=0
```

截图（Edge headless，真实后端数据）：

- 电脑 800×1000：[XIA-7-XIA-8-desktop.png](images/XIA-7-XIA-8-desktop.png)（4 号预约，已预约状态，显示"使用前取消 / 扫码签到"）
- 手机 430 CSS px @2x：[XIA-7-XIA-8-mobile.png](images/XIA-7-XIA-8-mobile.png)（5 号预约，已签到状态，显示"扫码签退"）

## 四、未执行与已知限制

1. **手机真实扫码未验证**：需要手机与开发机同网段，并解决本机 8080 被占用的问题；本次只用 URL 参数模拟扫码结果页。
2. **登录与 CSRF 未实现**（属 XIA-1）：当前用 `dev` profile + `X-Debug-User-Id` 调试身份，属于临时手段，XIA-1 落地后必须替换；不能用于演示以外的环境。
3. **漏签退未自动处理**：签到后一直不签退，预约会停留在 `CHECKED_IN` 并继续占用场地。是否自动完成或记违规需要 PO 决定，本故事未实现。
4. **并发只验证了逻辑不变量**：锁内重读 + 带状态条件的 UPDATE 能保证"签到与超时只成功一方"，未做多线程压测。
5. **未经另一名开发人员评审**，也未做整组 DoD 的评审与演示。
6. 既有的 `CampusShareBackendApplicationTests.contextLoads` 需要可用的数据库，本次按测试名过滤掉，未纳入结果。

## 五、待 PO 确认的口径

| 编号 | 问题 | 当前默认 |
|---|---|---|
| 1 | 是否允许提前签到、提前多久 | 不允许，从开始时间起（`app.sports.check-in-early-minutes=0`） |
| 2 | 迟到签到的临界 | 开始后 15 分钟整即拒绝（`check-in-grace-minutes=15`） |
| 3 | 已签到但未到开始时间能否取消 | 不能，`CHECKED_IN` 一律视为已开始使用 |
| 4 | 是否允许提前签退 | 允许，按实际时间写入并转 `COMPLETED` |
| 5 | 漏签退如何处理 | 暂不处理，见已知限制 3 |

## 六、复现步骤

```powershell
# 1. 数据库（本机已装便携版，端口 3307）
#    停止：Stop-Process -Name mysqld
#    数据目录：%LOCALAPPDATA%\campusshare-mysql\data

# 2. 建库与演示数据：把 001~004 脚本复制到不含中文的路径后依次执行
mysql --host=127.0.0.1 --port=3307 --user=cs_test --password=cs_test_pw --default-character-set=utf8mb4 --execute="source <path>/001_schema.sql"

# 3. 后端（8080 被占用时换 8081）
java -jar campus-share-backend/target/campus-share-backend-0.0.1-SNAPSHOT.jar `
  --spring.profiles.active=dev --server.port=8081 `
  --spring.datasource.url="jdbc:mysql://127.0.0.1:3307/campusshare?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" `
  --spring.datasource.username=cs_test --spring.datasource.password=cs_test_pw

# 4. 前端（代理指向实际后端端口）
$env:VITE_PROXY_TARGET="http://127.0.0.1:8081"; pnpm run dev
# 打开 http://localhost:5173/?reservationId=4

# 5. 测试
mvnw -B test -Dtest=SportsBookingRulesTest,SportsReservationServiceTest
$env:MYSQL_TEST="true"; mvnw -B test -Dtest=SportsReservationIntegrationTest
```

## 七、AI 使用记录

本次使用 DSH（DeepSeek）完成：阅读仓库与三份协作约定、起草接口契约、编写后端与前端代码、编写单元/联调测试、安装 JDK 21 与便携 MySQL、执行测试与 HTTP 联调、截图验证。共享表结构、身份方案与本文"待 PO 确认"的五条口径需要人工复核。
