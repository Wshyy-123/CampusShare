# CampusShare 前端 — XIA-3 时段管理 / XIA-4 资源与可用时段

本目录是任务 3（管理员维护开放/教学占用时段）与任务 4（学生/教师查询资源与可用时段）的前端代码。
按 `docs/校享CampusShare_前端开发约定.md` 的目录规范组织，业务文件可直接合并到主工程 `frontend/src/` 下。

## 本地联调

```bash
npm install
npm run dev      # 默认 http://localhost:5174，/api 代理到 localhost:8080
```

启动对应后端独立模块：
- XIA-3：`backend/XIA-3-schedule-management/`（admin / admin123）
- XIA-4：`backend/XIA-4-resource-availability/`

## 页面与接口

| 路由 | 页面 | 调用接口 |
|---|---|---|
| `/resources` | 资源列表（关键字/类型筛选、分页） | `GET /api/v1/resources` |
| `/resources/:id` | 资源详情 + 当日可用时段时间轴 | `GET /api/v1/resources/{id}/availability?date=YYYY-MM-DD` |
| `/admin/schedules` | 管理员时段 CRUD | `GET/POST/PUT/DELETE /api/v1/admin/schedules` |

时间轴颜色约定：绿底=开放可约，红=教学占用，蓝=已预约，紫=停用/维护。

## 集成到主 frontend/ 工程

主工程 `frontend/` 已有 `package.json`、`vite.config.ts` 等骨架。集成时只搬业务文件，不要覆盖主工程配置：

| 本目录文件 | 复制到主工程 |
|---|---|
| `src/api/request.ts` | `frontend/src/api/request.ts` |
| `src/api/schedule.ts` | `frontend/src/api/schedule.ts` |
| `src/api/resource.ts` | `frontend/src/api/resource.ts` |
| `src/types/index.ts` | `frontend/src/types/index.ts`（如已有则合并类型） |
| `src/views/admin/ScheduleManage.vue` | `frontend/src/views/admin/ScheduleManage.vue` |
| `src/views/resource/ResourceList.vue` | `frontend/src/views/resource/ResourceList.vue` |
| `src/views/resource/ResourceDetail.vue` | `frontend/src/views/resource/ResourceDetail.vue` |
| `src/styles/theme.css` | `frontend/src/styles/theme.css`（或合并到现有样式变量） |

然后在主工程：
1. `package.json` 加依赖 `vue-router`（如未安装）
2. `src/router/index.ts`（新建）注册上述三条路由
3. `src/main.ts` 里 `app.use(router)`
4. `src/App.vue` 加导航链接指向 `/resources` 与 `/admin/schedules`

## 待集成时调整的点

- `api/schedule.ts` 里目前用 Basic Auth `admin/admin123` 做独立联调；接入主后端 Spring Security 后改为 Session Cookie，删除 `auth` 参数。
- `views/admin/ScheduleManage.vue` 里 `KNOWN_UNITS` 是写死的演示数据；接入资源下拉接口后改为动态加载。
- 时间轴范围固定 06:00–23:00；后续按学校实际开放时间改为从后端读取。
