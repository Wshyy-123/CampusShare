# XIA-4 / PB-04：查询资源和可用时段

> PingCode 独立工作项：**XIA-4**  
> 建议 Git 分支：`feature/XIA-4-resource-availability`

## 1. 本任务范围

学生/教师查询资源、预约单位及指定日期的可预约时段。

可用时段由服务端计算：

```text
可用 = OPEN开放时段 - TEACHING教学占用 - 有效预约 - 停用/维护区间
```

资源或预约单位整体状态为 `DISABLED` 时，直接返回不可预约。

验收对应：
1. 选择资源和日期后展示开放及可预约情况；
2. 没有可预约时段时明确返回“暂无可预约时段”；
3. 资源停用时显示停用状态，停用期间不可预约。

## 2. XIA-4 自己新增/负责的代码

```text
resource/controller/ResourceController.java
resource/service/ResourceService.java
resource/dto/*
availability/service/AvailabilityService.java
availability/service/AvailabilityCalculator.java
availability/service/TimeInterval.java
availability/dto/*
```

下面这些是读取其他工作项数据所需的**最小公共依赖/只读模型**，不是 XIA-4 在业务上负责维护它们：

```text
schedule/domain/* + schedule/repository/SchedulePeriodRepository.java
reservation/domain/* + reservation/repository/ReservationRepository.java
block/domain/* + block/repository/ResourceBlockRepository.java
resource/domain/* + resource/repository/*
```

特别注意：本包中**没有** XIA-3 的 `AdminScheduleController`、`ScheduleService`、创建/修改时段 DTO，因此 XIA-4 已与任务3的写入逻辑解耦。

## 3. API

- `GET /api/v1/resources?keyword=&type=&page=1&size=20`
- `GET /api/v1/resources/{resourceId}`
- `GET /api/v1/resources/{resourceId}/availability?date=2026-11-03`
- `GET /api/v1/resources/units/{unitId}/availability?date=2026-11-03`

演示数据中 `unitId=1` 在 `2026-11-03`：
- OPEN：08:00–22:00
- TEACHING：10:00–12:00
- 已预约：14:00–15:00
- 停用维护：18:00–19:00

因此可用结果为：
- 08:00–10:00
- 12:00–14:00
- 15:00–18:00
- 19:00–22:00

## 4. 运行

1. MySQL 创建数据库：`campusshare`
2. 配置数据库连接
3. 执行：`mvn clean test`
4. 执行：`mvn spring-boot:run`

业务时区统一按 `Asia/Shanghai`。
