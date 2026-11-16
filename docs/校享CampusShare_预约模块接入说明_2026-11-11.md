# 预约模块接入说明

日期：2026-11-11。范围：PR #6 的 XIA-5 创建预约、XIA-6 本人预约列表与详情，接入 PR #7 的统一前端框架。

**2026-11-13更新：管理模块、座位未到及停用已整合，数据库需追加006/007。当前完整步骤与范围见[管理与生命周期集成说明](校享CampusShare_管理与生命周期集成说明_2026-11-13.md)；下文保留PR #6原接入说明。**

## 当前可用范围

- 保留统一侧栏、主题、资源卡片、占用时间轴和主导航配置。
- demo模式继续使用独立内存样例；api模式通过Session与CSRF调用主后端的登录、资源/可用性、创建预约、本人分页查询和详情接口。接口错误不回退为演示成功。
- 学生/教师可使用本人预约功能。真实管理菜单按服务端身份显示，开放/教学维护和停用已在2026-11-13接入。
- 资源与预约编号使用字符串，避免JavaScript对BIGINT的精度损失。接口模式不虚构座位插座、场地容量等服务端未提供的信息。
- 扫码、刷卡、正常结束释放和教室/体育未到任务尚未接入；座位未到已在2026-11-13接入；旧Library终端和SportsTimeoutTask保持默认关闭。

业务规则按[项目背景与业务规则](校享CampusShare_项目背景与业务规则.md)，扩展页面按[前端基准与页面扩展指南](校享CampusShare_前端基准与页面扩展指南.md)。不把当前登录/资源适配当成XIA-1/2/3/4所有来源模块已经迁移完成。

## 首次运行真实接口模式

### 1. MySQL结构和演示数据

使用项目基线MySQL 8.4，先进入campus-share-backend目录，再打开本地MySQL客户端：

```powershell
mysql -u root -p
```

首次建库，在MySQL客户端执行：

```sql
SET time_zone = '+08:00';
source docs/datebase/001_schema.sql;
source docs/datebase/002_demo_data.sql;
source docs/datebase/003_xia5_xia6_core.sql;
source docs/datebase/004_xia5_xia6_demo.sql;
source docs/datebase/005_discussion_room_deadline.sql;
source docs/datebase/006_schedule_opening_note.sql;
source docs/datebase/007_schedule_history.sql;
exit;
```

已经应用001至004的开发库执行尚未应用的005、006、007，不重复执行ALTER迁移，不用独立模块schema覆盖现有库。005只让讨论室的到馆期限可空并清除该类错误期限，不删除预约历史。

004创建按导入日期生成的七天开放样例。样例过期后通过后续开发的时段管理或新增编号数据脚本维护，不反复重跑001至004。所有校园数据与账号均为课程模拟，不是学校真实教务或校园卡数据。

### 2. 启动主后端

Java 21环境，在campus-share-backend目录执行；账号密码通过当前进程环境变量提供：

```powershell
$credential = Get-Credential -UserName root -Message '本地MySQL账号'
$env:CAMPUSSHARE_DB_USERNAME = $credential.UserName
$env:CAMPUSSHARE_DB_PASSWORD = $credential.GetNetworkCredential().Password
.\mvnw.cmd spring-boot:run
```

默认连接localhost:3306/campusshare，主后端端口8080。需要更改连接时使用CAMPUSSHARE_DB_URL/CAMPUSSHARE_PORT；也可在IDEA运行主类时配置相同环境变量。密码不写入源码、提交或截图。

### 3. 启动主前端

另开终端进入frontend目录：

```powershell
npm.cmd ci
$env:VITE_DATA_MODE = 'api'
npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort
```

打开http://127.0.0.1:5173/resources，未登录会进入登录页并保留返回地址。导入004后可使用student01/student02、teacher01、admin01，演示密码为Campus@123；页面的账号按钮仅填表，实际登录仍由后端验证。

如果使用.env.local，将VITE_DATA_MODE设置为api并重启Vite。默认demo不会访问后端，刷新会重置演示记录；接口模式会写入数据库，不因刷新丢失预约。不同本机后端端口可在启动前设置VITE_API_PROXY_TARGET。

## 页面和服务接入位置

| 功能 | 代码入口 |
|---|---|
| 登录与会话恢复 | frontend/src/views/LoginView.vue、stores/auth.ts、api/auth.ts |
| 原统一资源页 | frontend/src/views/ResourceExplorer.vue |
| 完整预约表单 | /resources/:id/reserve，ReservationCreateView.vue |
| 我的预约 | /bookings，MyReservations.vue，后端分页/状态筛选 |
| 预约详情 | /bookings/:id，ReservationDetailView.vue，后端只返回本人记录 |
| 统一业务服务 | composables/useCampus.ts、api/campusApi.ts |
| 统一HTTP传输 | api/request.ts；api/http.ts仅编排CSRF生命周期、错误与会话失效 |
| 服务端可用区间 | ResourceService/AvailabilityCalculator，availablePeriods字段 |
| 预约写入 | reservation/service/ReservationService，先资源锁，再预约/幂等检查 |

同学原资源/预约列表的功能已迁入统一ResourceExplorer/MyReservations，不再保留两套路由和样式。原登录、完整预约表单、详情和API模块继续复用并适配当前主题。新增导航仍改config/pages.ts，不复制App或另起前端。

## 本次补充与限制

补充了过去时段拒绝、讨论室前一自然日限制及不套用15分钟期限、超大页码处理、相邻开放区间一致性、登录时会话ID/CSRF轮换、并发锁冲突可重试响应。网络丢失响应时，前端保留原clientRequestId重试，不按重试次数生成新预约。

可用区间由后端计算，前端快捷时段和完整表单读取它；创建时后端仍在事务中重新校验。前端时段展示、角色菜单和表单校验不能代替服务端授权、锁与冲突检查。

验证记录见[PR #6集成验证](verification/PR6-integration-2026-11-11.md)。本地H2和HTTP检查不等于MySQL并发、真实手机或整站业务验收，远端结果以对应Actions运行记录为准。
