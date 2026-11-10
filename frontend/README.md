# CampusShare 主前端

Vue 3 + TypeScript + Vite，统一应用入口。后续成员基于最新main的本工程扩展页面，导航/路由共用src/config/pages.ts，页面统一通过src/composables/useCampus.ts取得服务。

先读[前端基准与页面扩展指南](../docs/校享CampusShare_前端基准与页面扩展指南.md)，业务按[项目背景与业务规则](../docs/校享CampusShare_项目背景与业务规则.md)。

```powershell
npm.cmd ci
npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort
```

打开http://127.0.0.1:5173/resources。主页面另有/bookings和/admin/schedules。

默认demo，常驻模拟数据提示，刷新重置。复制.env.example到.env.local可设置VITE_DATA_MODE；api模式当前明确返回未接入，不使用样例伪装真实结果。

```powershell
npm.cmd run check:baseline
npm.cmd run build
```

检查包含演示边界、错误处理、模式隔离、路由和组件渲染，不代替实际浏览器、真实后端、认证和扫码验收。Windows重装依赖前停止自己运行的Vite，避免原生绑定占用。

新增入口先在views实现业务页，再注册pages配置，共用App和theme。局部新增/修改使用弹窗；扫码和可分享详情使用独立路由。XIA-3-XIA-4和LegacyLibraryTerminal为来源材料，不继续发展独立应用。
