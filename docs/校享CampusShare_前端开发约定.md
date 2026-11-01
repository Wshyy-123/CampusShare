# 校享 CampusShare 前端开发约定

版本：V1.0｜更新日期：2026-10-27｜适用对象：前端开发同学及参与接口联调的后端同学。

本文件统一前端技术、视觉、页面、接口协作和提交要求，并附Vue Bits参考源码。**技术路线和参考Vue Bits的视觉方向已确定；文中明确标注“建议”“草案”“待确认”的内容，由相关同学在开工时共同确定，不作为已经通过的团队决定。**

## 一、已经确定的方案

| 项目 | 统一方案 |
|---|---|
| 产品形态 | 浏览器访问的网页，兼顾电脑和手机 |
| 前端 | Vue 3 + TypeScript + Vite |
| 开发工具 | VS Code |
| 运行环境 | Node.js 24，具体补丁版本在工程初始化时固定 |
| 包管理器 | npm；统一提交package-lock.json，调整包管理器须先统一 |
| 后端 | Java 21 + Spring Boot；具体依赖版本由后端初始化时固定 |
| 数据访问 | 前端调用Spring Boot接口，由后端访问MySQL |
| 视觉方向 | 参考[Vue Bits](https://vue-bits.dev/)的视觉风格，围绕校园预约业务重新组织页面 |
| 代码仓库 | [Wshyy-123/CampusShare](https://github.com/Wshyy-123/CampusShare)，前端放在frontend/ |
| 需求管理 | PingCode，提交和PR关联实际工作项编号，例如XIA-5 |
| 演示方式 | 体育签到、签退用手机真实扫码；图书馆刷卡首版用模拟入口 |

Vue Bits提供视觉和动效组件。日期选择、表单、弹窗、表格等基础控件仍需统一一套方案，目前尚未确定采用shadcn-vue或其他组件库。

开发阶段各自使用本地数据库，团队准备通过phpStudy管理MySQL。数据库结构和初始化脚本由后端统一，前端通过接口联调；数据库实际版本还需与原MySQL 8.4 LTS基线核对。前端配置里不填写数据库密码。

## 二、第一轮功能范围

PO提出先完成12条“最高”优先级故事：**XIA-1至XIA-11、XIA-14**。开发人员仍需估算、核对依赖与验收条件，再确定本轮实际可承担范围。

| 工作项 | 前端涉及的主要内容 |
|---|---|
| XIA-1 | 登录、当前身份展示和未登录处理；管理端也需要有对应身份入口 |
| XIA-2、XIA-3 | 资源与预约单位维护、开放时段和教学占用管理 |
| XIA-4、XIA-5 | 资源查询、日期和可约时段、提交预约、冲突及失败反馈 |
| XIA-6 | 我的预约、本人记录与状态展示 |
| XIA-7、XIA-8、XIA-9 | 体育取消、手机扫码签到签退、后台未到处理后的结果展示 |
| XIA-10、XIA-11 | 座位到馆确认、模拟刷卡入口及超时释放后的结果展示 |
| XIA-14 | 场地停用操作、停用状态和受影响预约结果 |

XIA-12讨论室提前预约、XIA-13教室取消暂不列入本轮范围意向。通用资源页面是否覆盖相应类型，按实际选入的故事和验收标准确定。

后台自动取消、释放和记违规由服务端执行，用户关闭页面后仍需成立；前端负责请求、操作反馈和结果展示。涉及扫码、到馆、取消的页面不能自行计算并写入“业务成功”。

## 三、页面与视觉

### 1. 整体方向

参考Vue Bits的排版层次、留白、卡片、边框、背景和交互细节，形成一套统一风格。具体深浅主题、主色和动效强度尚未锁定，先完成代表性页面样例，再统一扩展。

| 页面 | 设计建议 |
|---|---|
| 首页 | 清晰的大标题、适度留白和局部渐变或动态背景，突出查询和预约入口 |
| 场地列表 | 统一场地卡片，名称、位置、资源单位和可约状态有明确层次 |
| 场地详情与预约 | 日期、时段、规则说明和确认按钮清晰，延续统一视觉 |
| 我的预约 | 卡片或列表显示资源、时间、状态及本人可执行的操作 |
| 登录 | 沿用同一字体、配色和表单控件，可加入局部背景效果 |
| 管理页面 | 保持同一设计语言，优先保证表格、筛选和表单易读易用 |

页面内容由本轮业务决定。参考网站的组件分类、展示面板和示例数据不直接成为项目需求。

### 2. 多人开发要共用的内容

- 颜色、文字层级、圆角、间距、边框和阴影集中管理，避免每页各定一套。
- 按钮、输入框、弹窗、日期选择、状态标签、加载和空状态使用共享组件。
- 状态除颜色外同时显示文字，未登录、无权限、空记录、加载失败等情况要有清楚反馈。
- 表单校验提示靠近输入位置，提交中显示处理中状态，操作结果以接口返回为准。
- 先统一首页、场地卡片和预约表单样例，再按同一规则分工开发。

### 3. 手机和动效

- 手机通过点击完成全部核心操作，卡片悬停光效只是增强。
- 日期和时段选择在手机上可读、可点，主要按钮便于触摸。
- 动态背景和内容进场按需使用；减少动态效果时仍能正常使用页面。
- 资源信息、预约时间和操作按钮保持稳定清晰，后台表格需要能在窄屏下查看。
- 选中的组件在实际手机上检查；作者示例不代替项目的适配验证。

## 四、工程与代码协作草案

以下为建议执行方式，由前端共同评审后固定到工程配置。

### 1. 建议目录

```text
frontend/
  src/
    api/                  统一请求封装与业务接口
    types/                接口类型与共享业务类型
    router/               路由与统一登录跳转
    views/                页面
    components/
      common/             共用业务和基础组件
      visual/             引入并适配的Vue Bits组件
    styles/               主题变量与公共样式
    assets/               项目图片和图标
  package.json
  package-lock.json
```

当前为目录建议，不表示工程已经建立。业务接口集中维护，页面不各自拼接后端地址，也不重复定义同一份返回数据类型。

### 2. 编码方式

- 使用Vue单文件组件和TypeScript，建议采用`<script setup lang="ts">`。
- 文件使用UTF-8、统一LF换行；前端默认2空格缩进，最终以共享格式化配置为准。
- Vue组件文件使用PascalCase；变量、函数和JSON字段使用camelCase。
- 共享类型、状态名称与后端接口文档一致，不能各自给同一状态改名。
- 避免为跳过检查普遍使用any；格式检查、类型检查和构建分别执行。
- AI生成代码与手写代码采用相同检查要求，提交者需要能解释实现及验证结果。

## 五、登录与接口联调

### 1. 登录

已确认业务角色为学生、教师、资源管理员。前端按身份展示对应入口，服务端负责认证、权限和记录归属校验。

建议首版使用数据库预置的演示账号。此前提出的Spring Security会话Cookie方案仍为技术建议，前后端需要共同定稿：登录参数、凭证传递、当前用户查询、登录失效、退出及必要的CSRF处理。不要把Vue Bits或其他模板的模拟登录当作真实登录功能。

### 2. 一条接口至少要写清什么

| 内容 | 要写清楚的事项 |
|---|---|
| 请求 | 地址、方法、参数名称、类型、必填条件 |
| 身份 | 是否需要登录、允许哪些角色、凭证怎样携带 |
| 成功结果 | 返回字段、字段含义、可展示的状态和时间 |
| 失败结果 | 未登录、无权限、参数错误、资源不存在、预约冲突等处理 |
| 例子 | 至少一个成功请求与返回，以及相关失败例子 |

此前草案建议使用/api/v1前缀，JSON字段使用camelCase，响应使用code/message/data，分页使用page/size和items/total。**这些字段和路径仍需前后端评审，未作为已经实现的接口。**

时间统一按Asia/Shanghai理解，对外时间格式在契约里写明。所有截止与超时判断以服务端口径为准。

前端可以按接口契约使用模拟数据并行开发；进入真实联调时使用后端接口。模拟数据下页面正常不能单独作为故事完成依据。接口变更先更新文档，再通知对接同学，并在对应工作项中记录影响。

### 3. 页面必须保留的业务边界

- 同一排他资源的重叠时段只能有一个有效预约；查询结果可能变旧，提交时仍需以后端检查为准。
- 座位到馆期限从预约提交成功起算30分钟；暂不允许主动取消。
- 体育预约开始后15分钟内未签到，由后台自动处理；已确认的取消与签退规则按PB执行。
- 已失效预约不能因迟到刷卡或扫码恢复原占用。
- 资源停用期间仍不可预约，解除某条预约占用不等于重新开放资源。
- 体育提前签到与取消的边界、第15/30分钟临界口径、漏签退、停用范围和正在使用预约的处理，涉及本轮实现时先由PO与开发人员澄清。

## 六、分工、提交和完成检查

### 已约定的协作方式

1. 每项功能建立独立分支，关联PingCode工作项编号。
2. 提交PR，由另一名开发人员检查后合并；具体评审搭档由开发团队安排。
3. 每项任务明确负责人与配合者，共用组件和接口改动提前沟通。
4. 持续集成与联调，不等全部页面写完才第一次对接后端。

### 建议纳入DoD的检查项

以下供团队制定完成定义时采用，不表示DoD已经通过：

- 相关功能符合对应验收标准，正常、异常和权限情况经过检查。
- 格式检查、TypeScript类型检查、构建及相关测试通过。
- 接入真实后端，并在约定电脑和手机环境检查主要流程。
- 加载、空状态、失败与成功反馈完整。
- 另一名开发人员完成代码检查，接口和必要说明同步更新。
- 原有流程没有因本次修改出现回归问题。

具体检查命令、GitHub Actions配置及执行结果在工程中落实。开发记录填写实际结果，尚未执行的检查保留为未执行。

## 七、开工前集中确定

| 事项 | 确定者 |
|---|---|
| 本轮一个Sprint目标、选入故事、工作量与任务分工 | 全体讨论，PO说明价值，开发人员估算 |
| 深浅主题、主色、字体、基础控件库、共用页面样例 | 前端共同提出，PO确认视觉方向 |
| Vue/Vite等具体版本、格式化和检查配置 | 前端开发人员 |
| 本轮实际使用哪些Vue Bits组件及依赖 | 前端开发人员 |
| 接口文档工具、字段、状态和登录凭证方案 | 前后端共同确定 |
| 手机扫码入口采用相机打开网页还是网页内调用摄像头 | PO与开发人员结合体验和实现确定 |
| 尚未明确的取消、签到、时间和停用边界 | PO澄清，开发人员核对可测试性 |

建议先跑通一个页面调用后端并读取数据库，再走通一次分支、PR和自动检查，随后按Sprint计划并行推进。

## 附录：Vue Bits参考源码

本附录内置所选原站CSS、三个Vue组件、场地卡片用法和完整作者许可，转发本文件即可查看。原始项目为[DavidHDev/vue-bits](https://github.com/DavidHDev/vue-bits)，来源固定在提交`3766ebeeed37ec35bce29f001b6d101c7b17da35`。

作者原文件与下面的CampusShare接入示例分别注明。原站CSS依赖其DOM、字体、全局变量和其他组件，单独复制CSS不会重现完整页面。中文字体和最终主题需要另行统一。

**依赖与适配：**本批组件使用useTemplateRef，需要Vue 3.5或兼容版本；SpotlightCard和示例的工具类需要Tailwind CSS；AnimatedContent需要GSAP，Aurora需要OGL。按选中的组件固定依赖，npm基线继续保留。原版AnimatedContent会在更新或卸载时清理全局ScrollTrigger，正式接入时应改成只清理本组件的触发器，并补充减少动态效果的支持。

目前原始文件已做来源一致性核对；没有安装依赖、构建、渲染或实测手机表现。参考源码不作为本项目业务已实现或验收通过的证据。

<details>
<summary>Hero.css：首页首屏布局、标题、按钮和响应式样式（作者原文件）</summary>

[原始文件](https://github.com/DavidHDev/vue-bits/blob/3766ebeeed37ec35bce29f001b6d101c7b17da35/src/components/landing/Hero/Hero.css)

```css
.ln-hero {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
}

.ln-hero-dots {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.ln-hero-band {
  position: absolute;
  mix-blend-mode: screen;
  bottom: 0;
  left: 0;
  right: 0;
  height: 150%;
  pointer-events: none;
  z-index: 1;
}

.ln-hero-bottom-fade {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 100%;
  display: block;
  pointer-events: none;
  z-index: 2;
}

.ln-hero-content {
  position: relative;
  z-index: 3;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: start;
  max-width: 1324px;
  margin: 0 auto;
  padding: clamp(120px, 16vw, 240px) 24px 80px;
  overflow: hidden;
}

.ln-hero-left {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ln-hero-tag {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: 'Geist Mono', monospace;
  font-size: 12px;
  font-weight: 500;
  text-transform: none;
  letter-spacing: 0;
  color: rgba(255, 255, 255, 0.7);
  width: fit-content;
  padding: 5px 16px 5px 5px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(18, 15, 23, 0.45);
  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);
  box-shadow:
    0 2px 12px rgba(0, 0, 0, 0.25),
    inset 0 0.5px 0 rgba(255, 255, 255, 0.06);
  transition:
    background 0.2s ease,
    color 0.2s ease,
    border-color 0.2s ease;
  text-decoration: none;
}

.ln-hero-tag:hover {
  background: rgba(18, 15, 23, 0.55);
  border-color: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.95);
}

.ln-hero-tag-new {
  font-size: 12px;
  color: #fff;
  background: #00ffa9;
  padding: 4px 10px;
  border-radius: 6px;
  text-transform: uppercase;
}

.ln-hero-headline {
  font-family: 'Geist Pixel Line', sans-serif;
  font-size: clamp(28px, 5.5vw, 68px);
  font-weight: 500;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: #fff;
  margin: 0;
}

.ln-hero-headline-line {
  white-space: nowrap;
}

.ln-hero-description {
  font-family: 'Geist', sans-serif;
  font-size: 16px;
  font-weight: 400;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.606);
  margin: 0;
  max-width: 42ch;
}

.ln-hero-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
}

.ln-hero-btn {
  font-family: 'Geist Mono', monospace;
  font-size: 14px;
  font-weight: 500;
  padding: 12px 24px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  transition: filter var(--transition-fast);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 9px;
}

.ln-hero-btn-primary {
  background: #00ffa9;
  color: #fff;
  border: 1px solid #00ffa9;
}

.ln-hero-btn-primary:hover {
  filter: brightness(0.85);
}

.ln-hero-btn-secondary {
  background: transparent;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.ln-hero-btn-secondary:hover {
  border-color: rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.04);
}

.ln-hero-btn-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

.ln-hero-proof {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  list-style: none;
  padding: 0;
  font-family: 'Geist Mono', monospace;
  font-size: 12px;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.4);
}

.ln-hero-proof-sep {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
}

.ln-hero-right {
  width: 100%;
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
}

.ln-hero-code-window {
  width: 100%;
  min-width: 0;
  max-width: 520px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(18, 15, 23, 0.45);
  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);
  box-shadow:
    0 4px 32px rgba(0, 0, 0, 0.25),
    inset 0 0.5px 0 rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.ln-hero-code-titlebar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 9px 9px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.ln-hero-code-titlebar-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ln-hero-code-reset {
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Geist Mono', monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.01);
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 0 10px;
  height: 30px;
  border-radius: 7px;
  cursor: pointer;
  box-sizing: border-box;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast);
}

.ln-hero-code-reset:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.1);
}

.ln-hero-code-dots {
  display: flex;
  gap: 6px;
}

.ln-hero-code-dots span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.ln-hero-code-dropdown {
  position: relative;
}

.ln-hero-code-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'Geist Mono', monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.01);
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 0 10px;
  height: 30px;
  border-radius: 7px 7px 2px 7px;
  cursor: pointer;
  box-sizing: border-box;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast);
}

.ln-hero-code-dropdown-trigger:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.1);
}

.ln-hero-code-caret {
  transition: transform 0.25s ease;
}

.ln-hero-code-caret.open {
  transform: rotate(180deg);
}

.ln-hero-code-dropdown-menu {
  position: absolute;
  top: calc(100% + 9px);
  right: -10px;
  min-width: 140px;
  padding: 4px;
  border-radius: 0 0 0 7px;
  background: rgba(18, 15, 23, 0.95);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.06);
  z-index: 10;
  opacity: 0;
  transform: translateY(-4px) scale(0.97);
  pointer-events: none;
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.ln-hero-code-dropdown-menu.open {
  opacity: 1;
  transform: translateY(0) scale(1);
  pointer-events: auto;
}

.ln-hero-code-dropdown-item {
  display: block;
  width: 100%;
  text-align: left;
  font-family: 'Geist Mono', monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  background: transparent;
  border: none;
  padding: 7px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.ln-hero-code-dropdown-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.8);
}

.ln-hero-code-dropdown-item.active {
  color: rgba(255, 255, 255, 0.8);
}

.ln-hero-code-body {
  padding: 4px 8px;
  overflow: hidden;
}

.ln-hero-code-pre {
  margin: 0;
  padding: 12px 8px;
  background: transparent;
  font-family: 'Geist Mono', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #b3ffe4;
  white-space: pre;
  overflow-x: auto;
  overflow-y: hidden;
}

/* Syntax token colors matching codeTheme */
.c-kw {
  color: #fff;
}
.c-punc {
  color: #ccc;
}
.c-comp {
  color: #b3ffe4;
}
.c-tag {
  color: #00ffa9;
}
.c-attr {
  color: #fff;
}
.c-str {
  color: #cdfff0;
}
.c-fn {
  color: #fdfdfd;
}

/* Interactive values */
.ln-hero-code-value {
  font-family: 'Geist Mono', monospace;
  font-size: 13px;
  border: none;
  padding: 0 4px;
  margin: 2px 0;
  border-radius: 4px;
  transition:
    background 0.15s ease,
    box-shadow 0.15s ease;
  display: inline-flex;
  align-items: center;
  line-height: inherit;
}

.ln-hero-code-value--number {
  color: #00ffa9;
  background: rgba(0, 255, 169, 0.08);
  cursor: ew-resize;
  touch-action: none;
}

.ln-hero-code-value--number:hover {
  background: rgba(0, 255, 169, 0.22);
}

.ln-hero-code-value--number:active {
  background: rgba(0, 255, 169, 0.22);
}

.ln-hero-code-value--bool {
  color: #fdfdfd;
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  border: none;
  font-family: 'Geist Mono', monospace;
}

.ln-hero-code-value--bool:hover {
  background: rgba(255, 255, 255, 0.12);
}

.ln-hero-code-value--color {
  color: #cdfff0;
  background: rgba(0, 255, 169, 0.08);
  cursor: pointer;
  position: relative;
}

.ln-hero-code-value--color:hover {
  background: rgba(209, 246, 215, 0.16);
}

.ln-hero-code-swatch {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  display: inline-block;
  border: 1px solid rgba(255, 255, 255, 0.15);
  flex-shrink: 0;
}

.ln-hero-code-color-input {
  display: none;
}

/* Custom color picker */
.ln-hero-color-picker {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 60;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: 10px;
  padding: 10px;
  width: 200px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.55);
}

.ln-hero-color-picker-area {
  position: relative;
  width: 100%;
  height: 120px;
  border-radius: 6px;
  cursor: crosshair;
  margin-bottom: 8px;
  touch-action: none;
}

.ln-hero-color-picker-hue {
  position: relative;
  width: 100%;
  height: 12px;
  border-radius: 6px;
  cursor: pointer;
  background: linear-gradient(to right, #f00, #ff0, #0f0, #0ff, #00f, #f0f, #f00);
  margin-bottom: 8px;
  touch-action: none;
}

.ln-hero-color-picker-thumb {
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.55);
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.ln-hero-color-picker-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.ln-hero-color-picker-preset {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  border: 1.5px solid rgba(255, 255, 255, 0.12);
  cursor: pointer;
  padding: 0;
  transition: border-color 0.15s;
}

.ln-hero-color-picker-preset:hover {
  border-color: rgba(255, 255, 255, 0.5);
}

.ln-hero-code-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 4px 16px 12px;
}

.ln-hero-code-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.ln-hero-code-preset {
  font-family: 'Geist Mono', monospace;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  background: transparent;
  border: none;
  padding: 5px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.ln-hero-code-preset:hover {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.85);
}

.ln-hero-code-preset.active {
  background: rgba(0, 255, 169, 0.1);
  color: rgba(0, 255, 169, 0.9);
}

.ln-hero-code-hint {
  font-family: 'Geist Mono', monospace;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.2);
  margin: 0;
  flex-shrink: 0;
}

/* ── Responsive ── */

@media (max-width: 1275px) {
  .ln-hero-content {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .ln-hero-right {
    justify-content: flex-start;
  }

  .ln-hero-code-window {
    max-width: 100%;
  }
}

@media (max-width: 1024px) {
  .ln-hero-content {
    padding: clamp(110px, 14vw, 160px) 24px 60px;
  }

  .ln-hero-right {
    justify-content: flex-start;
  }

  .ln-hero-headline {
    font-size: clamp(36px, 6vw, 48px);
  }

  .ln-hero-description {
    max-width: 45ch;
  }
}

@media (max-width: 640px) {
  .ln-hero-code-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .ln-hero-content {
    padding: clamp(90px, 16vw, 120px) 16px 48px;
  }

  .ln-hero-headline {
    font-size: clamp(30px, 8vw, 40px);
  }

  .ln-hero-description {
    font-size: 15px;
  }

  .ln-hero-buttons {
    width: 100%;
  }

  .ln-hero-btn {
    font-size: 13px;
    padding: 10px 20px;
    flex: 1 1 auto;
    justify-content: center;
    white-space: nowrap;
  }

  .ln-hero-tag {
    font-size: 11px;
  }

  .ln-hero-tag-new {
    font-size: 11px;
    padding: 3px 8px;
    text-transform: uppercase;
  }
}
```

</details>

<details>
<summary>Navbar.css：悬浮导航及滚动状态样式（作者原文件）</summary>

[原始文件](https://github.com/DavidHDev/vue-bits/blob/3766ebeeed37ec35bce29f001b6d101c7b17da35/src/components/landing/Navbar/Navbar.css)

```css
/* ─── Navbar ───────────────────────────────────────────────────────────────── */
.ln-navbar {
  position: fixed;
  top: 20px;
  left: 0;
  right: 0;
  z-index: 1500;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 24px;
  font-family: 'Geist Mono', monospace;
  pointer-events: none;
}

.ln-navbar-inner {
  width: 100%;
  max-width: 1680px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 0 20px;
  border-radius: 16px;
  border: 1px solid transparent;

  background: transparent;

  backdrop-filter: none;
  -webkit-backdrop-filter: none;

  transition:
    max-width 0.5s ease,
    background 0.5s ease,
    border-color 0.5s ease,
    padding 0.5s ease;

  pointer-events: auto;
  position: relative;
}

.ln-navbar.ln-navbar-scrolled .ln-navbar-inner {
  max-width: calc(1300px - 24px);

  background: #111;

  border-color: rgba(255, 255, 255, 0.04);
}

/* ──────────────────────────────────────────────────────────
   Left
   ────────────────────────────────────────────────────────── */

.ln-navbar-left {
  display: flex;
  align-items: center;
}

.ln-navbar-logo {
  width: 100px;
  display: flex;
  align-items: center;
}

.ln-navbar-logo svg,
.ln-navbar-logo .ln-navbar-logo-placeholder {
  height: 20px;
  width: auto;
}

.ln-navbar-divider {
  color: rgba(255, 255, 255, 0.82);

  font-size: 18px;
  font-weight: 300;

  user-select: none;

  margin: 0 8px 0 18px;
}

/* ──────────────────────────────────────────────────────────
   Links
   ────────────────────────────────────────────────────────── */

.ln-navbar-links {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ln-navbar-link-highlight {
  position: absolute;
  top: 0;
  left: 0;

  border-radius: 12px;

  background: rgba(20, 24, 18, 0.55);

  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);

  border: 1px solid rgba(255, 255, 255, 0.08);

  box-shadow:
    0 2px 16px rgba(0, 0, 0, 0.2),
    inset 0 0.5px 0 rgba(255, 255, 255, 0.06);

  opacity: 0;

  pointer-events: none;

  transition:
    transform 0.3s ease,
    width 0.3s ease,
    height 0.3s ease,
    opacity 0.2s ease;
}

.ln-navbar-link {
  position: relative;
  z-index: 1;

  color: rgba(255, 255, 255, 0.72);

  font-size: 13px;
  font-weight: 500;

  text-transform: uppercase;
  letter-spacing: 0.04em;

  padding: 6px 10px;

  border-radius: 12px;

  transition: color 0.2s ease;
}

.ln-navbar-link:hover {
  color: #fff;
}

.ln-navbar-link-active {
  color: #fff;
}

/* ──────────────────────────────────────────────────────────
   Right
   ────────────────────────────────────────────────────────── */

.ln-navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ln-navbar-soon {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.04em;

  color: rgba(255, 255, 255, 0.55);

  background: rgba(255, 255, 255, 0.08);

  padding: 2px 6px;

  border-radius: 4px;
}

/* ──────────────────────────────────────────────────────────
   GitHub Button
   ────────────────────────────────────────────────────────── */

.ln-navbar-github {
  display: flex;
  align-items: center;
  gap: 6px;

  font-size: 13px;
  font-weight: 500;

  color: rgba(255, 255, 255, 0.82);

  height: 36px;
  padding: 0 12px;

  border-radius: 10px;

  border: 1px solid rgba(255, 255, 255, 0.08);

  background: rgba(18, 15, 23, 0.45);

  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);

  box-shadow:
    0 2px 16px rgba(0, 0, 0, 0.2),
    inset 0 0.5px 0 rgba(255, 255, 255, 0.06);

  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.ln-navbar-github:hover {
  border-color: rgba(66, 184, 131, 0.24);

  background: rgba(66, 184, 131, 0.08);
}

/* ──────────────────────────────────────────────────────────
   Search
   ────────────────────────────────────────────────────────── */

.ln-navbar-search {
  display: flex;
  align-items: center;
  gap: 8px;

  height: 36px;
  padding: 0 10px;

  border-radius: 10px;

  border: 1px solid rgba(255, 255, 255, 0.08);

  background: rgba(18, 15, 23, 0.45);

  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);

  box-shadow:
    0 2px 16px rgba(0, 0, 0, 0.2),
    inset 0 0.5px 0 rgba(255, 255, 255, 0.06);

  color: rgba(255, 255, 255, 0.7);

  font-family: 'Geist Mono', monospace;
  font-size: 12px;
  font-weight: 500;

  cursor: pointer;

  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
}

.ln-navbar-search:hover {
  border-color: rgba(66, 184, 131, 0.32);

  background: rgba(66, 184, 131, 0.08);

  color: #fff;
}

.ln-navbar-search kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;

  min-width: 18px;
  height: 18px;

  padding: 0 5px;

  border-radius: 5px;

  border: 1px solid rgba(255, 255, 255, 0.1);

  background: rgba(255, 255, 255, 0.05);

  color: rgba(255, 255, 255, 0.45);

  font: inherit;
  font-size: 10px;
}

/* ──────────────────────────────────────────────────────────
   Icon Buttons
   ────────────────────────────────────────────────────────── */

.ln-navbar-icon-btn {
  display: flex;
  align-items: center;
  gap: 6px;

  font-size: 13px;
  font-weight: 500;

  color: rgba(255, 255, 255, 0.62);

  height: 36px;
  padding: 0 12px;

  border-radius: 10px;

  border: 1px solid rgba(255, 255, 255, 0.08);

  background: rgba(18, 15, 23, 0.45);

  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);

  cursor: pointer;

  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
}

.ln-navbar-icon-btn:hover {
  border-color: rgba(66, 184, 131, 0.24);

  background: rgba(66, 184, 131, 0.08);

  color: #fff;
}

/* ──────────────────────────────────────────────────────────
   Preferences
   ────────────────────────────────────────────────────────── */

.ln-navbar-prefs-wrapper {
  position: relative;
}

.ln-navbar-prefs-trigger {
  gap: 4px;
}

.ln-navbar-prefs-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;

  width: 220px;
  padding: 12px;

  border-radius: 14px;

  border: 1px solid rgba(66, 184, 131, 0.14);

  background: rgba(16, 20, 14, 0.96);

  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);

  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45);

  animation: ln-menu-slide-in 0.2s ease;

  z-index: 1000;
}

.ln-navbar-prefs-fav {
  display: flex;
  align-items: center;
  gap: 8px;

  font-size: 13px;
  font-weight: 500;

  color: #fff;

  padding: 8px;

  border-radius: 12px;

  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.ln-navbar-prefs-fav svg {
  box-sizing: content-box;

  width: 13px;
  height: 13px;

  padding: 6px;

  border-radius: 6px;

  background: #42b883;

  color: #08130f;
}

.ln-navbar-prefs-fav:hover {
  background: rgba(66, 184, 131, 0.12);

  color: #fff;
}

/* ──────────────────────────────────────────────────────────
   Mobile Hamburger
   ────────────────────────────────────────────────────────── */

.ln-navbar-hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;

  width: 36px;
  height: 36px;

  padding: 9px;

  border: 1px solid rgba(255, 255, 255, 0.08);

  border-radius: 10px;

  background: rgba(18, 15, 23, 0.45);

  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);

  cursor: pointer;

  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.ln-navbar-hamburger:hover {
  border-color: rgba(66, 184, 131, 0.24);

  background: rgba(66, 184, 131, 0.08);
}

.ln-navbar-hamburger span {
  display: block;

  width: 100%;
  height: 1.5px;

  background: #fff;

  border-radius: 1px;

  transition:
    transform 0.25s ease,
    opacity 0.25s ease;
}

.ln-navbar-hamburger.open span:nth-child(1) {
  transform: translateY(5.5px) rotate(45deg);
}

.ln-navbar-hamburger.open span:nth-child(2) {
  opacity: 0;
}

.ln-navbar-hamburger.open span:nth-child(3) {
  transform: translateY(-5.5px) rotate(-45deg);
}

/* ──────────────────────────────────────────────────────────
   Mobile Menu
   ────────────────────────────────────────────────────────── */

.ln-navbar-mobile-menu {
  display: none;
  flex-direction: column;
  gap: 2px;

  position: absolute;
  top: calc(100% + 8px);
  right: 5px;

  min-width: 200px;

  padding: 8px;

  border-radius: 14px;

  border: 1px solid rgba(66, 184, 131, 0.18);

  background: linear-gradient(145deg, rgba(66, 184, 131, 0.08), rgba(16, 20, 14, 0.96) 46%), rgba(16, 20, 14, 0.94);

  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);

  box-shadow:
    0 18px 48px rgba(0, 0, 0, 0.45),
    0 0 34px rgba(66, 184, 131, 0.08);

  pointer-events: auto;

  animation: ln-menu-slide-in 0.2s ease;

  z-index: 1001;
}

@keyframes ln-menu-slide-in {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ln-navbar-mobile-link {
  display: flex;
  align-items: center;
  justify-content: space-between;

  font-family: 'Geist Mono', monospace;
  font-size: 13px;
  font-weight: 500;

  text-transform: uppercase;
  letter-spacing: 0.04em;

  color: rgba(255, 255, 255, 0.72);

  padding: 12px 14px;

  border-radius: 10px;

  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.ln-navbar-mobile-link::after {
  content: '→';

  font-size: 16px;

  color: inherit;

  opacity: 0;

  transform: translateX(-6px);

  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.ln-navbar-mobile-link:hover {
  background: rgba(66, 184, 131, 0.12);

  color: #42b883;
}

.ln-navbar-mobile-link:hover::after {
  opacity: 1;
  transform: translateX(0);
}

/* ──────────────────────────────────────────────────────────
   Responsive
   ────────────────────────────────────────────────────────── */

@media (max-width: 768px) {
  .ln-navbar {
    padding: 0 16px;
  }

  .ln-navbar-inner {
    padding: 0 5px 0 16px;
  }

  .ln-navbar-links,
  .ln-navbar-divider,
  .ln-navbar-github,
  .ln-navbar-prefs-wrapper {
    display: none;
  }

  .ln-navbar-search span,
  .ln-navbar-search kbd {
    display: none;
  }

  .ln-navbar-search {
    width: 36px;
    justify-content: center;
    padding: 0;
  }

  .ln-navbar-hamburger {
    display: flex;
  }

  .ln-navbar-mobile-menu {
    display: flex;
  }

  .ln-navbar-inner {
    width: 100%;
    max-width: 100%;
    height: 56px;

    display: flex;
    align-items: center;
    justify-content: space-between;

    padding: 0 5px 0 5px;

    border-radius: 16px;

    border: 1px solid transparent;

    background: transparent;

    backdrop-filter: none;
    -webkit-backdrop-filter: none;

    transition:
      background 0.5s ease,
      border-color 0.5s ease,
      padding 0.5s ease;

    pointer-events: auto;

    position: relative;
  }

  .ln-navbar.ln-navbar-scrolled .ln-navbar-inner {
    max-width: 100%;

    padding: 0 5px 0 16px;

    background: #111;

    backdrop-filter: none;
    -webkit-backdrop-filter: none;

    border-color: rgba(255, 255, 255, 0.04);
  }
}

/* ─── Docs variant ─────────────────────────────────────────────────────────── */

.ln-navbar.ln-navbar-docs {
  top: 0;
  padding: 0;
}

.ln-navbar.ln-navbar-docs .ln-navbar-inner {
  max-width: 100%;

  border-radius: 0;

  height: 60px;

  padding: 0 2em;

  background: #111;

  border-color: transparent;

  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.ln-navbar.ln-navbar-docs.ln-navbar-scrolled .ln-navbar-inner {
  max-width: 100%;

  border-radius: 0;

  background: #111;

  backdrop-filter: none;
  -webkit-backdrop-filter: none;

  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  border-color: transparent;
}

@media (max-width: 768px) {
  .ln-navbar.ln-navbar-docs .ln-navbar-inner {
    padding: 0 16px;
  }
}

/* The docs sidebar hides at 967px (see docs.css), so the docs navbar's
   hamburger/search-icon-only mode must kick in at the same width — otherwise
   widths between 769px and 966px have no sidebar and no way to open it. */
@media (max-width: 967px) {
  .ln-navbar-docs .ln-navbar-hamburger {
    display: flex;
  }

  .ln-navbar-docs .ln-navbar-search span,
  .ln-navbar-docs .ln-navbar-search kbd {
    display: none;
  }

  .ln-navbar-docs .ln-navbar-search {
    width: 36px;
    justify-content: center;
    padding: 0;
  }
}
```

</details>

<details>
<summary>SpotlightCard.vue：光效卡片（作者原文件）</summary>

[原始文件](https://github.com/DavidHDev/vue-bits/blob/3766ebeeed37ec35bce29f001b6d101c7b17da35/src/content/Components/SpotlightCard/SpotlightCard.vue)

```vue
<template>
  <div
    ref="divRef"
    @mousemove="handleMouseMove"
    @focus="handleFocus"
    @blur="handleBlur"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    :class="['relative rounded-3xl border overflow-hidden p-8', className]"
  >
    <div
      class="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-500 ease-in-out"
      :style="{
        opacity,
        background: `radial-gradient(circle at ${position.x}px ${position.y}px, ${spotlightColor}, transparent 80%)`
      }"
    />

    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref, useTemplateRef } from 'vue';

interface Position {
  x: number;
  y: number;
}

interface SpotlightCardProps {
  className?: string;
  spotlightColor?: string;
}

const { className = '', spotlightColor = 'rgba(255, 255, 255, 0.25)' } = defineProps<SpotlightCardProps>();

const divRef = useTemplateRef<HTMLDivElement>('divRef');
const isFocused = ref<boolean>(false);
const position = ref<Position>({ x: 0, y: 0 });
const opacity = ref<number>(0);

const handleMouseMove = (e: MouseEvent) => {
  if (!divRef.value || isFocused.value) return;

  const rect = divRef.value.getBoundingClientRect();
  position.value = { x: e.clientX - rect.left, y: e.clientY - rect.top };
};

const handleFocus = () => {
  isFocused.value = true;
  opacity.value = 0.6;
};

const handleBlur = () => {
  isFocused.value = false;
  opacity.value = 0;
};

const handleMouseEnter = () => {
  opacity.value = 0.6;
};

const handleMouseLeave = () => {
  opacity.value = 0;
};
</script>
```

</details>

<details>
<summary>AnimatedContent.vue：内容进场动效（作者原文件）</summary>

[原始文件](https://github.com/DavidHDev/vue-bits/blob/3766ebeeed37ec35bce29f001b6d101c7b17da35/src/content/Animations/AnimatedContent/AnimatedContent.vue)

```vue
<script setup lang="ts">
import { onMounted, onUnmounted, watch, useTemplateRef } from 'vue';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

interface AnimatedContentProps {
  distance?: number;
  direction?: 'vertical' | 'horizontal';
  reverse?: boolean;
  duration?: number;
  ease?: string | ((progress: number) => number);
  initialOpacity?: number;
  animateOpacity?: boolean;
  scale?: number;
  threshold?: number;
  delay?: number;
  className?: string;
}

const props = withDefaults(defineProps<AnimatedContentProps>(), {
  distance: 100,
  direction: 'vertical',
  reverse: false,
  duration: 0.8,
  ease: 'power3.out',
  initialOpacity: 0,
  animateOpacity: true,
  scale: 1,
  threshold: 0.1,
  delay: 0,
  className: ''
});

const emit = defineEmits<{
  complete: [];
}>();

const containerRef = useTemplateRef<HTMLDivElement>('containerRef');

onMounted(() => {
  const el = containerRef.value;
  if (!el) return;

  const axis = props.direction === 'horizontal' ? 'x' : 'y';
  const offset = props.reverse ? -props.distance : props.distance;
  const startPct = (1 - props.threshold) * 100;

  gsap.set(el, {
    [axis]: offset,
    scale: props.scale,
    opacity: props.animateOpacity ? props.initialOpacity : 1
  });

  gsap.to(el, {
    [axis]: 0,
    scale: 1,
    opacity: 1,
    duration: props.duration,
    ease: props.ease,
    delay: props.delay,
    onComplete: () => emit('complete'),
    scrollTrigger: {
      trigger: el,
      start: `top ${startPct}%`,
      toggleActions: 'play none none none',
      once: true
    }
  });
});

watch(
  () => [
    props.distance,
    props.direction,
    props.reverse,
    props.duration,
    props.ease,
    props.initialOpacity,
    props.animateOpacity,
    props.scale,
    props.threshold,
    props.delay
  ],
  () => {
    const el = containerRef.value;
    if (!el) return;

    ScrollTrigger.getAll().forEach(t => t.kill());
    gsap.killTweensOf(el);

    const axis = props.direction === 'horizontal' ? 'x' : 'y';
    const offset = props.reverse ? -props.distance : props.distance;
    const startPct = (1 - props.threshold) * 100;

    gsap.set(el, {
      [axis]: offset,
      scale: props.scale,
      opacity: props.animateOpacity ? props.initialOpacity : 1
    });

    gsap.to(el, {
      [axis]: 0,
      scale: 1,
      opacity: 1,
      duration: props.duration,
      ease: props.ease,
      delay: props.delay,
      onComplete: () => emit('complete'),
      scrollTrigger: {
        trigger: el,
        start: `top ${startPct}%`,
        toggleActions: 'play none none none',
        once: true
      }
    });
  },
  { deep: true }
);

onUnmounted(() => {
  const el = containerRef.value;
  if (el) {
    ScrollTrigger.getAll().forEach(t => t.kill());
    gsap.killTweensOf(el);
  }
});
</script>

<template>
  <div ref="containerRef" :class="`animated-content ${props.className}`">
    <slot />
  </div>
</template>

<style scoped>
/* GSAP will handle all transforms and opacity */
</style>
```

</details>

<details>
<summary>Aurora.vue：极光背景（作者原文件）</summary>

[原始文件](https://github.com/DavidHDev/vue-bits/blob/3766ebeeed37ec35bce29f001b6d101c7b17da35/src/content/Backgrounds/Aurora/Aurora.vue)

```vue
<template>
  <div ref="ctnDom" class="aurora-container" />
</template>

<script setup lang="ts">
import { Color, Mesh, Program, Renderer, Triangle } from 'ogl';
import { onMounted, onUnmounted, ref } from 'vue';

const VERT = `#version 300 es
in vec2 position;
void main() {
  gl_Position = vec4(position, 0.0, 1.0);
}
`;

const FRAG = `#version 300 es
precision highp float;

uniform float uTime;
uniform float uAmplitude;
uniform vec3 uColorStops[3];
uniform vec2 uResolution;
uniform float uBlend;

out vec4 fragColor;

vec3 permute(vec3 x) {
  return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(vec2 v){
  const vec4 C = vec4(
      0.211324865405187, 0.366025403784439,
      -0.577350269189626, 0.024390243902439
  );
  vec2 i  = floor(v + dot(v, C.yy));
  vec2 x0 = v - i + dot(i, C.xx);
  vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);

  vec3 p = permute(
      permute(i.y + vec3(0.0, i1.y, 1.0))
    + i.x + vec3(0.0, i1.x, 1.0)
  );

  vec3 m = max(
      0.5 - vec3(
          dot(x0, x0),
          dot(x12.xy, x12.xy),
          dot(x12.zw, x12.zw)
      ),
      0.0
  );
  m = m * m;
  m = m * m;

  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);

  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

struct ColorStop {
  vec3 color;
  float position;
};

#define COLOR_RAMP(colors, factor, finalColor) {              \\
  int index = 0;                                            \\
  for (int i = 0; i < 2; i++) {                               \\
     ColorStop currentColor = colors[i];                    \\
     bool isInBetween = currentColor.position <= factor;    \\
     index = int(mix(float(index), float(i), float(isInBetween))); \\
  }                                                         \\
  ColorStop currentColor = colors[index];                   \\
  ColorStop nextColor = colors[index + 1];                  \\
  float range = nextColor.position - currentColor.position; \\
  float lerpFactor = (factor - currentColor.position) / range; \\
  finalColor = mix(currentColor.color, nextColor.color, lerpFactor); \\
}

void main() {
  vec2 uv = gl_FragCoord.xy / uResolution;

  ColorStop colors[3];
  colors[0] = ColorStop(uColorStops[0], 0.0);
  colors[1] = ColorStop(uColorStops[1], 0.5);
  colors[2] = ColorStop(uColorStops[2], 1.0);

  vec3 rampColor;
  COLOR_RAMP(colors, uv.x, rampColor);

  float height = snoise(vec2(uv.x * 2.0 + uTime * 0.1, uTime * 0.25)) * 0.5 * uAmplitude;
  height = exp(height);
  height = (uv.y * 2.0 - height + 0.2);
  float intensity = 0.6 * height;

  float midPoint = 0.20;
  float auroraAlpha = smoothstep(midPoint - uBlend * 0.5, midPoint + uBlend * 0.5, intensity);

  vec3 auroraColor = intensity * rampColor;

  fragColor = vec4(auroraColor * auroraAlpha, auroraAlpha);
}
`;

interface AuroraProps {
  colorStops?: string[];
  amplitude?: number;
  blend?: number;
  time?: number;
  speed?: number;
}

const props = withDefaults(defineProps<AuroraProps>(), {
  colorStops: () => ['#171D22', '#7cff67', '#171D22'],
  amplitude: 1.0,
  blend: 0.5,
  speed: 1.0
});

const ctnDom = ref<HTMLDivElement | null>(null);

let animateId = 0;
let renderer: InstanceType<typeof Renderer> | null = null;
let program: InstanceType<typeof Program> | null = null;
let resizeHandler: (() => void) | null = null;

onMounted(() => {
  const ctn = ctnDom.value;
  if (!ctn) return;

  renderer = new Renderer({
    alpha: true,
    premultipliedAlpha: true,
    antialias: true
  });

  const gl = renderer.gl;
  gl.clearColor(0, 0, 0, 0);
  gl.enable(gl.BLEND);
  gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
  gl.canvas.style.backgroundColor = 'transparent';

  resizeHandler = () => {
    if (!ctn || !renderer) return;
    const width = ctn.offsetWidth;
    const height = ctn.offsetHeight;
    renderer.setSize(width, height);
    if (program) {
      program.uniforms.uResolution.value = [width, height];
    }
  };

  window.addEventListener('resize', resizeHandler);

  const geometry = new Triangle(gl);
  if (geometry.attributes.uv) {
    delete geometry.attributes.uv;
  }

  const colorStopsArray = props.colorStops.map((hex: string) => {
    const c = new Color(hex);
    return [c.r, c.g, c.b];
  });

  program = new Program(gl, {
    vertex: VERT,
    fragment: FRAG,
    uniforms: {
      uTime: { value: 0 },
      uAmplitude: { value: props.amplitude },
      uColorStops: { value: colorStopsArray },
      uResolution: { value: [ctn.offsetWidth, ctn.offsetHeight] },
      uBlend: { value: props.blend }
    }
  });

  const mesh = new Mesh(gl, { geometry, program });
  ctn.appendChild(gl.canvas);

  const update = (t: number) => {
    animateId = requestAnimationFrame(update);
    const time = props.time ?? t * 0.01;
    const speed = props.speed ?? 1.0;

    if (program && renderer) {
      program.uniforms.uTime.value = time * speed * 0.1;
      program.uniforms.uAmplitude.value = props.amplitude ?? 1.0;
      program.uniforms.uBlend.value = props.blend ?? 0.5;
      program.uniforms.uColorStops.value = (props.colorStops ?? ['#171D22', '#7cff67', '#171D22']).map(
        (hex: string) => {
          const c = new Color(hex);
          return [c.r, c.g, c.b];
        }
      );
      renderer.render({ scene: mesh });
    }
  };

  animateId = requestAnimationFrame(update);
  resizeHandler();
});

onUnmounted(() => {
  cancelAnimationFrame(animateId);
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler);
  }
  if (renderer) {
    const ctn = ctnDom.value;
    const canvas = renderer.gl.canvas;
    if (ctn && canvas.parentNode === ctn) {
      ctn.removeChild(canvas);
    }
    renderer.gl.getExtension('WEBGL_lose_context')?.loseContext();
  }
});
</script>

<style scoped>
.aurora-container {
  width: 100%;
  height: 100%;
}
</style>
```

</details>

<details>
<summary>ResourceCardExample.vue：场地卡片接入示例（Codex编写）</summary>

将本例与SpotlightCard.vue放在同一目录。父页面传入标题和位置，监听select事件后进入真实的时段查询流程。颜色只是示例，不是最终配色。

```vue
<script setup lang="ts">
// CampusShare用法示例，由Codex编写，不是作者原站代码。
// 与SpotlightCard.vue保存到同一目录；只演示显示和事件，不调用API。
import SpotlightCard from './SpotlightCard.vue';

defineProps<{ title: string; location: string }>();
const emit = defineEmits<{ select: [] }>();
</script>

<template>
  <SpotlightCard
    class-name="bg-[#111114] border-white/10 text-white"
    spotlight-color="rgba(124, 100, 255, 0.16)"
  >
    <article class="relative z-10 flex flex-col gap-4">
      <h2 class="text-xl font-semibold">{{ title }}</h2>
      <p class="text-sm leading-6 text-white/65">{{ location }}</p>
      <button
        type="button"
        class="min-h-11 rounded-xl bg-white px-5 py-3 text-sm font-semibold text-zinc-950 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-violet-300"
        @click="emit('select')"
      >
        查看可约时段
      </button>
    </article>
  </SpotlightCard>
</template>
```

</details>

<details>
<summary>Vue Bits作者许可与版权声明（原文）</summary>

[来源许可](https://github.com/DavidHDev/vue-bits/blob/3766ebeeed37ec35bce29f001b6d101c7b17da35/LICENSE.md)。采用相应源码时在工程中保留完整文本，不改写成标准MIT声明。

```text
MIT + Commons Clause License Condition v1.0

Copyright (c) 2025 David Haz

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, and distribute the Software **as part of an application, website, or product**, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

## Commons Clause Restriction

You may use this Software, including for any commercial purpose, **so long as you do not sell, sublicense, or redistribute the components themselves-whether alone, in a bundle, template, or as a ported version.**

## No Warranty

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

</details>
