# XIA-3 / PB-03：维护开放与教学占用时段

> PingCode 独立工作项：**XIA-3**  
> 建议 Git 分支：`feature/XIA-3-schedule-management`

## 1. 本任务范围

资源管理员维护预约单位的：
- `OPEN`：开放时段
- `TEACHING`：教学占用时段

验收对应：
1. 有权限且时间合法时可保存开放/教学占用安排；
2. 开始时间必须早于结束时间，否则拒绝保存；
3. 支持查询、修改、删除已维护时段；
4. 管理接口仅资源管理员可操作。

## 2. XIA-3 自己新增/负责的代码

```text
schedule/controller/AdminScheduleController.java
schedule/service/ScheduleService.java
schedule/domain/SchedulePeriod.java
schedule/domain/SchedulePeriodType.java
schedule/dto/ScheduleCreateRequest.java
schedule/dto/ScheduleUpdateRequest.java
schedule/dto/ScheduleResponse.java
schedule/repository/SchedulePeriodRepository.java
```

`resource/`、`common/`、`config/` 是为了让这个任务包可独立运行而保留的最小公共依赖，不应在 PingCode 中记成 XIA-3 的额外业务功能。

## 3. API

管理员账号（仅独立联调用）：`admin / admin123`

- `POST /api/v1/admin/schedules`：新增开放/教学占用时段
- `GET /api/v1/admin/schedules?unitId=1&date=2026-11-03`：查询某预约单位某天安排
- `PUT /api/v1/admin/schedules/{id}`：修改时段
- `DELETE /api/v1/admin/schedules/{id}`：删除时段

新增示例：

```json
{
  "unitIds": [1, 9],
  "type": "TEACHING",
  "startAt": "2026-11-04T10:00:00+08:00",
  "endAt": "2026-11-04T12:00:00+08:00",
  "note": "课程占用"
}
```

如果 `startAt >= endAt`，返回 HTTP 400，错误码 `INVALID_TIME_RANGE`。

## 4. 运行

1. MySQL 创建数据库：`campusshare`
2. 修改 `src/main/resources/application.yml` 或通过环境变量提供数据库连接
3. 执行：`mvn clean test`
4. 执行：`mvn spring-boot:run`

业务时区统一按 `Asia/Shanghai`。
