# 前端基准与页面扩展指南

版本：V1.0｜日期：2026-11-10｜适用：全体前端成员、接口对接成员及其Agent。

后续在本次统一界面上持续开发，保留主布局、导航、主题和组件，在主工程内新增页面与功能。主工程仍为frontend/；本次发布是前端结构与演示交互基准，不表示后端、认证、数据库或完整预约流程已接通。

业务要求按[项目背景与业务规则](校享CampusShare_项目背景与业务规则.md)，工程与Git协作按[主工程集成与协作要求](校享CampusShare_主工程集成与协作要求_2026-11-10.md)，通用前端要求按[前端开发约定](校享CampusShare_前端开发约定.md)。本文件负责当前代码怎样扩展，不能用页面样例改写业务规则。

## 1. 统一入口和当前范围

| 路径/功能 | 当前内容 | 扩展方式 |
|---|---|---|
| /resources | 分类、搜索、日期、资源卡片、详情弹窗、占用时间轴、模拟预约 | 在ResourceExplorer及共享组件内扩展；真实可用性改用服务端结果 |
| /bookings | 演示预约列表与状态示例 | 在MyReservations接本人预约、真实身份与状态 |
| /admin/schedules | 模拟占用列表、新增弹窗 | 在ScheduleManage接共享后端的开放/教学/外部/维护安排及管理员权限 |
| 资源详情 | 当前为弹窗 | 需要收藏、分享或直接访问时增加/resources/:id路由，复用详情组件 |
| 手机扫码 | 本基准未实现 | 使用可直接访问的地点URL；按业务规则接真实登录和签到写接口 |

主功能使用站内路由；新增、编辑、确认等局部操作使用弹窗。不要为每个菜单打开新浏览器标签，也不要新建独立Vue应用。暂未实现的菜单不以可点击空页面伪装成功能完成。

当前六个资源、三条预约及占用记录均为本地演示样例。默认日期按Asia/Shanghai生成；预置状态用于展示，不是实时考勤。按一小时排列时段是本次演示排版选择，不建立新的业务时长规则。SPORTS只是UI分组，真实乒乓球桌、羽毛球、网球资源类型依正式接口映射。

## 2. 同学拉取与启动

已克隆仓库的同学先保存自己的未提交工作，再执行：

```powershell
git fetch origin
git switch -c feature/XIA-2-resource-ui origin/main
cd frontend
npm.cmd ci
npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort
```

上例使用已有XIA-2资源管理工作项演示分支命名，成员按自己实际任务替换。分支已存在时切换并合入origin/main。浏览器打开http://127.0.0.1:5173/resources。5173被占用时先检查已有前端服务，不同时启动多套主应用。

## 3. 文件入口

| 文件/目录（相对frontend/） | 职责 |
|---|---|
| src/App.vue | 共用外壳、导航渲染、模式提示、全局反馈；不堆入各页业务 |
| src/config/pages.ts | 主页面路径、名称、标题、图标与懒加载组件的唯一配置 |
| src/router/index.ts | 从配置生成路由、首页跳转及路由行为；独立落地页在这里注册 |
| src/views/ | 每项主功能的页面 |
| src/components/common/ | 共用按钮/图标/表单等组件 |
| src/components/visual/ | 场地插图和视觉组件 |
| src/styles/theme.css | 当前主题与响应式样式；组件复用颜色变量、间距和按钮类 |
| src/composables/useCampus.ts | 页面统一取数、错误反馈和提交入口 |
| src/services/types.ts | 展示层服务接口与状态形状 |
| src/services/demoCampusService.ts、src/data/preview.ts | 演示模式行为与样例资源；页面不直接导入样例数据 |
| src/api/campusApi.ts | 真实服务适配入口，目前明确返回未接入 |
| src/api/request.ts | /api/v1请求、Session Cookie、CSRF、超时及错误处理 |
| src/types/campus.ts、src/utils/presentation.ts | 展示模型、日期/时刻格式与展示文案 |
| scripts/check-baseline.mjs | 演示边界、请求错误、模式隔离、路由及组件渲染检查 |

原接口终端保留在src/components/LegacyLibraryTerminal.vue，未挂入新版导航，包含尚未修复的旧业务问题，不作为新增功能的实现模板。frontend/XIA-3-XIA-4保留作迁移来源，后续不在其中扩展第二套产品前端。

## 4. 增加一个菜单和页面

以新增资源管理页面为例，这一页尚未在本基准实现：

1. 新建src/views/ResourceManage.vue，复用现有主内容布局和主题，不复制App的侧栏、顶栏或另写全局body样式。
2. 在src/config/pages.ts的navigationPages数组追加：

```ts
{
  to: '/admin/resources',
  name: 'resource-management',
  label: '资源管理',
  icon: 'settings',
  component: () => import('../views/ResourceManage.vue'),
},
```

3. 导航与路由会共用此条配置生成，不需要再复制一份菜单。资源详情、登录回跳、扫码等不出现在主导航的页面直接注册在router/index.ts。
4. 页面调用useCampus提供的服务；新增服务方法时先更新services/types.ts，再分别处理demo与api实现，禁止在页面硬编码数据库账号、Basic凭证或userId。
5. 菜单可见性、路由权限与服务端权限分别实现。当前管理菜单是演示入口，没有认证守卫；真实模式必须以服务端身份做页面控制，服务端继续独立校验权限，隐藏按钮不能代替授权。

弹窗复用当前dialog样式，保留标题、关闭、Esc退出、表单错误及提交中状态。提交成功后读取服务端结果并刷新相关列表；不能只关闭窗口就声称保存成功。

## 5. 演示与真实接口模式

默认demo。需要显式配置时，在frontend目录复制.env.example为.env.local，设置后重启Vite：

```dotenv
VITE_DATA_MODE=demo
```

| 配置 | 行为 |
|---|---|
| demo | 加载样例资源与状态，顶部常驻演示标识，数据只在当前页面内存，刷新重置 |
| api | 不加载样例；目前显示真实接口未接入，操作不可用，不把演示结果当作后台成功 |
| 其他值 | 显示配置错误，不自动启用演示样例 |

env变量构建时生效；已发布静态站切换模式需要重新构建。.env.local按Git忽略规则不提交，示例不含真实密码。切换api不是已经接通后端，也不提供登录或权限。

真实接口接入顺序：

1. 与后端共同按docs/api的契约核对共享身份、资源ID、枚举、时区、状态和错误响应，不能分别复制独立模块的模型。services/types.ts是前端适配接口，不是已确认的REST字段清单。
2. 在api/campusApi.ts实现load：从真实接口加载资源和本人记录，经DTO校验/映射更新state；成功后才置ready。资源ID来自真实数据，不复用演示编号。
3. 实现loadAvailability，并按资源ID、日期缓存服务端返回的区间；slots和blocks读取缓存。列表日期变化与详情打开会调用此加载入口。真实模式不在前端独立算出一个结果代替后端判断。
4. 实现预约、占用维护：经统一request发写请求，带真实Session和服务器返回的CSRF headerName/token；验证返回结果后刷新预约与可用缓存。拒绝、网络异常、超时和字段错误均显示失败，禁止catch后回退demo或返回假成功。
5. 当前小时数字、固定一小时时段和展示资源分组属于演示模型。接真实接口时按契约转换日期/OffsetDateTime及资源类型，并扩展完整预约参数，不把演示shape直接用作SQL或正式API规范。
6. 接入真实身份、退出清理和权限守卫。任何写请求的本人预约、资源、状态和时间仍由后端核验。自动释放继续在后端执行，不能靠页面计时器代替。

当前request工具具备传输和错误处理结构，但尚未用于真实业务联调；CSRF取得、登录后令牌刷新和DTO校验仍由实际对接代码实现。没有调用成功、数据库记录与相应测试，不能把某个模块标成完成。

## 6. 检查与合并

在frontend目录执行：

```powershell
npm.cmd ci
npm.cmd run check:baseline
npm.cmd run build
```

Windows重装依赖前停止自己正在运行的Vite，避免原生绑定文件被占用。依赖变化提交package.json和package-lock.json，不提交node_modules、dist、真实凭证或本机配置。

前端CI执行以上检查。check:baseline覆盖样例占用/预约边界、请求失败不伪成功、CSRF传输、demo/api隔离以及导航页面组件渲染；它不操作真实浏览器，不验证手机、认证、数据库、扫码或后台释放。页面新增时同步检查脚本中的预期内容及相应有意义的测试，PR记录实际视觉与接口检查结果。

共享App、主题、pages配置、服务接口或API字段变动在PR中明确说明，功能成员持续同步main。另一名开发人员检查后合并；仅构建通过不等于业务验收。本次基准不指定人员、日期或工时，不改变已有PingCode状态。
