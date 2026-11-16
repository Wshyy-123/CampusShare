# 前端基准验证记录

日期：2026-11-10。范围：共享frontend工程、导航与路由、前端演示服务、真实接口边界及协作文档。

实际执行：npm ci（本地使用工作区缓存）、npm run check:baseline、npm run build，均退出0。build包含vue-tsc类型检查与Vite生产构建。

check:baseline的实际检查：

- 教学、外部、维护、既有预约、停用资源的演示时段排除；重复预约拒绝，相邻时段可用，新增占用影响可选时段。
- 缺CSRF时不发写请求；网络/HTTP/非JSON错误抛出，不伪造成功；Session Cookie与CSRF参数通过模拟fetch校验。
- 三个统一导航路径的组件可渲染，包含样例资源、预约和占用；新增演示预约能被预约页读取。
- api模式资源与预约为空、写操作失败，明确报告尚未接入，不回退demo。

检查脚本位于frontend/scripts/check-baseline.mjs，CI配置位于.github/workflows/frontend.yml。文档发布时新增工作流，远端是否通过以对应Actions运行结果为准。

未执行：真实浏览器截图、点击和各屏幕尺寸验收；真实登录、权限、数据库、扫码、刷卡与后台自动释放联调。组件渲染和模拟fetch检查不等于这些功能已验收。

旧Library终端组件保留源码，未纳入当前导航，也未将其旧取消按钮及后台逻辑问题标记为修复。
