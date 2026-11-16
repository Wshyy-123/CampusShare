# 管理与生命周期集成说明

日期：2026-11-13。范围：整合PR #8、#9、#10到唯一主工程。业务依据仍为《项目背景与业务规则》，页面和共享服务按《前端基准与页面扩展指南》。

## 当前接入

| 功能 | 页面 / 后端 |
|---|---|
| 资源新增、修改、分页、启用/停用 | /admin/resources；/api/v1/admin/resources |
| 开放与教学时段增改删 | /admin/schedules；共享resource_opening/resource_block |
| 按时间范围停用 | 时段管理“停用场地”；取消受影响的未使用预约，保留原因和历史 |
| 座位预约成功起30分钟未到 | 后台任务自动取消、释放，保存一条未到违规与取消事件 |
| 本人预约与可用性 | 保留已接入的登录、创建/列表/详情；管理写入后重新读取服务端状态 |

默认demo仍是内存预览。使用上述持久化管理功能须设置VITE_DATA_MODE=api并运行主后端及MySQL。

## 数据库升级与启动

在campus-share-backend目录打开本地MySQL 8.4客户端。首次按顺序应用001至007；已有001至005的开发库只追加下列两份，不能重跑建表脚本覆盖现有数据：

```sql
SET time_zone = '+08:00';
source docs/datebase/006_schedule_opening_note.sql;
source docs/datebase/007_schedule_history.sql;
```

已应用006的库仅追加007。006增加开放安排备注；007增加resource_schedule_event，保存开放/教学时段操作的前后快照、操作者、服务端时间和请求编号。删除当前安排仍保留其教学历史。已有001至005保持原内容，不修改已发布迁移。

后端在Java 21执行mvnw.cmd spring-boot:run，数据库环境变量、模拟登录账号和前端启动见[预约模块接入说明](校享CampusShare_预约模块接入说明_2026-11-11.md)。座位任务默认开启，默认每30秒扫描并在启动后补查；期限仍为accepted_at起30分钟，不是扫描间隔。

## 成员必须复用的约束

1. 管理端菜单、路由、服务端均检查RESOURCE_ADMIN；服务继续核对数据库中的有效账户及当前角色。Session/CSRF沿用现有实现，不信任前端userId。
2. 时段以type＋scheduleId共同定位。PUT的type在请求体中；DELETE必须带`?type=OPEN`或`?type=TEACHING`。两张表裸ID可能相同，列表key也必须包含type。
3. 写操作先锁资源，再重读相关记录与预约。维护教学时段若与有效预约重叠则409拒绝；缩短或删除开放安排若使已有预约失去开放覆盖则整次回滚。不得静默使已接受预约失效。
4. 需要取消受影响预约时使用停用服务，记录RESOURCE_DISABLED。存在已签到且到达开始时间的预约时整次停用409回滚。整资源enabled=false也走相同取消与历史逻辑，不能绕过按时段停用的保护。
5. 重新启用资源不恢复已取消预约，不删除教学/外部占用和仍生效的停用区间。有预约历史的资源不能更改资源类型，避免改写既有到馆期限、处罚与整间规则；需使用新资源。
6. 时段列表合并OPEN、TEACHING、EXTERNAL、DISABLED；后两类是只读记录，不能借时段删除接口解除停用。配置修改前后快照与实际修改在同一事务提交。
7. 页面继续使用useCampus和campusApi，不另写请求通道。修改成功后刷新资源/可用性；账户变更或筛选切换时忽略旧响应。刷新失败明确提示“保存成功但刷新失败”，不诱导重复提交写操作。

接口契约分别为docs/api/admin-resources.yaml、schedules.yaml和lifecycle.yaml。所有时间使用Asia/Shanghai业务时区，区间保持[开始,结束)。

## 检查与尚未接入范围

前端执行npm ci、npm run check:baseline、npm run check:api、npm run build；后端执行mvnw.cmd verify，MySQL 8.4并发与编号迁移以主工程CI验证。新增跨模块用例覆盖同号编辑/删除、已有预约保护、整资源停用与回滚、角色撤销、CSRF、分页溢出和教学维护/预约并发。

本次不等于整站验收完成。地点码扫码签到/签退、模拟刷卡入场/离场、教室/体育15分钟未到及正常结束自动释放仍须后续工作项接入；旧信任userId的刷卡入口及错误扫描全部预约的体育任务继续关闭。不得用数据库夹具或本次座位任务冒充真实手机扫码、刷卡或全流程验收。

本次实际验证及限制见[整合验证记录](verification/PR8-10-integration-2026-11-13.md)。其他同学从最新main开功能分支，在现有frontend与campus-share-backend继续开发，业务先读统一背景，接口按上述契约。
