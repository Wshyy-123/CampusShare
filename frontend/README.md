# CampusShare frontend

XIA-5（资源预约提交）和 XIA-6（我的预约、详情）的 Vue 3 页面。

## 启动

```bash
npm install
npm run dev
```

- 开发地址：`http://localhost:5173`
- Vite 将 `/api` 代理到 `http://localhost:8080`（主后端默认端口；本机可用 `VITE_API_PROXY_TARGET` 覆盖）
- 演示账号：`student01`、`student02`、`teacher01`，密码均为 `Campus@123`

## 检查

```bash
npm run typecheck
npm run build
```

## 页面

| 路由 | 页面 |
|---|---|
| `/login` | 临时登录入口（XIA-1 落地后替换） |
| `/resources` | 资源列表与筛选 |
| `/reserve/:id` | 选择日期/时段、查看当天开放与占用、提交预约 |
| `/reservations` | 我的预约列表、状态筛选、分页、空状态 |
| `/reservations/:id` | 本人预约详情；他人记录显示“不存在或无权查看” |

## 说明

- 请求统一走 `src/api/http.ts`，自动获取并携带 CSRF 令牌；401 统一跳回登录。
- `clientRequestId` 由前端为同一次提交生成，网络失败重试时复用；参数变化才生成新值。
- 页面不自行判断预约成功，成功状态以接口返回为准。
- 资源读取接口是 XIA-4 的临时依赖，后续应替换为 XIA-4 的正式契约。
