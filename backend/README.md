# CampusShare backend

XIA-5（创建预约）和 XIA-6（我的预约、预约详情）的后端实现，按《后端数据库接口与协作约定》的工程结构组织，并复用 XIA-11/14 分支的共享可用性检查。

## 技术基线

- Java 21、Spring Boot 3.5.16、MyBatis Starter 3.0.5、Maven 3.9.9（Wrapper）
- MySQL 8.0/8.4；本地开发使用 MySQL，自动化测试默认用 H2 的 MySQL 模式，CI 用 MySQL 8.4
- Spring Security 会话 + CSRF；登录入口是 XIA-1 落地前的临时实现

## 启动

1. 切到 JDK 21：

   ```powershell
   $env:JAVA_HOME = 'D:\Program Files\Java\jdk-21.0.9'
   $env:Path = "$env:JAVA_HOME\bin;$env:Path"
   ```

2. 准备数据库（空库执行一次；已有数据的库不要重复执行演示数据）：

   ```bash
   mysql --default-character-set=utf8mb4 -u root -p < ../docs/database/001_schema.sql
   mysql --default-character-set=utf8mb4 -u root -p < ../docs/database/002_demo_data.sql
   ```

3. 设置连接信息并启动。8080 在本机已被其他进程占用，默认使用 8081：

   ```powershell
   $env:CAMPUSSHARE_PORT = '8081'
   $env:CAMPUSSHARE_DB_URL = 'jdbc:mysql://127.0.0.1:3306/campusshare?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
   $env:CAMPUSSHARE_DB_USERNAME = 'root'
   $env:CAMPUSSHARE_DB_PASSWORD = '你的本地密码'
   .\mvnw.cmd spring-boot:run
   ```

   仓库不提交真实密码。也可以使用 `application.yml` 中的环境变量名自行配置。

## 主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/reservations` | XIA-5 创建预约；新建返回 201，幂等重放返回 200 |
| GET | `/api/v1/reservations/mine` | XIA-6 本人预约分页列表 |
| GET | `/api/v1/reservations/{reservationId}` | XIA-6 本人预约详情；他人记录统一 404 |
| GET | `/api/v1/resources` | XIA-4 依赖：资源列表 |
| GET | `/api/v1/resources/{id}/availability` | XIA-4 依赖：指定日期开放/占用信息 |

完整契约见 `docs/api/reservations.yaml` 与 `docs/api/resources.yaml`。

## 测试

```powershell
# 默认 H2；并发行锁测试会跳过
.\mvnw.cmd test

# 真实 MySQL（示例：独立测试库，端口按实际修改）
.\mvnw.cmd test `
  -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/campusshare_test?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" `
  -Dspring.datasource.username=root `
  -Dspring.datasource.password=你的测试库密码 `
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

测试夹具会反复重建 `app_user/resource/resource_opening/resource_block/reservation/reservation_event/violation_record`，不要把真实数据放进测试库。

## 与 XIA-11/14 的衔接

`com.campusshare.lifecycle` 下复制了 XIA-11/14 分支的 `ResourceAvailabilityGuard`、`LifecycleMapper`、`LifecycleException`、`LifecycleConfiguration` 和 `SeatArrivalDeadline`。创建预约在同一个事务内先调用 `assertNotBlocked`（锁 resource 行、检查启用与重叠 block），再做开放安排和预约冲突的锁定读。XIA-11/14 的停用接口、座位未到定时任务没有在本分支启用；合并时按该分支 README 接入即可。

## 当前边界

- `auth` 是 XIA-5/XIA-6 联调所需的最小会话登录，不声称完成 XIA-1。正式登录、角色入口和会话过期策略应由 XIA-1 负责人替换。
- `resource` 只提供预约页面所需的只读模型，不声称完成 XIA-4。
- 取消、签到、签退属于 XIA-7/XIA-8，本分支未实现。
