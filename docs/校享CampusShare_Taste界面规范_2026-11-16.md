# CampusShare Taste界面规范

日期：2026-11-16。本轮PO要求使用Taste Skill优化现有页面。改动基于ba542d5，当前保存在本机feature/taste-ui-2026-11-16，尚未提交或同步GitHub。后续若发布，以实际PR与提交记录为准。

## 设计依据和取舍

已实时检索并读取作者Leonxlnx的Taste Skill：主仓库https://github.com/Leonxlnx/taste-skill ，下载快照f6b76575f949707b4e3c37cd86cb44b0b98643e8，对应2.5.1；同时阅读作者保留的原v1界面设计指导https://github.com/Leonxlnx/taste-skill-classics 。原文在课程工作区.artifacts/taste_redesign_2026-11-16留存，未全局安装或运行第三方安装脚本。

本产品是已有业务的Vue预约应用，本轮参考颜色校准、字体层级、真实产品内容、减少装饰和卡片嵌套、控件反馈、响应式、键盘与减少动态效果等要求。保留既有导航、API认证、业务接口和模块；不改成React/Tailwind，不把预约工作台变成满屏营销展示站，不引入持续动画或虚构统计。未声称运行或通过2.5面向营销站的完整kit gate。

## 统一视觉

- 保留深色方向：背景#111413，表面#191e1b，边框#343c36；主文字#edf1e9、辅助文字#a5afa7，唯一主强调色#bfdc9c。警告和失败保留有语义的状态颜色。
- 英数使用本地可变Geist，中文使用设备的微软雅黑/PingFang等无衬线字体；不依赖外部字体服务。完整OFL许可在frontend/public/fonts/Geist-OFL.txt。
- 页面主标题控制字号和行高；使用统一间距、圆角、按钮和表单，不在标题加大面积渐变或发光。
- 发现空间首屏提供资源入口和本人当前记录中的预约提醒；统计来自共享服务，不伪造数量、评分、在线人数或学校场地事实。
- 场地图片为公开授权的类别示意，不是学校或具体资源的实景。来源、作者与许可见frontend/THIRD_PARTY_NOTICES.md；素材本地保存，加载失败保留原插画回退。
- 主操作与次操作清楚区分；保留禁用、错误、空结果、加载状态和键盘焦点。动效只用于短暂进入和hover/press，并尊重prefers-reduced-motion。
- 手机压缩首屏装饰，保留分类、搜索、日期、签到与提交操作；管理表格在容器内滚动，不使整个页面横向溢出。

## 修改入口

- frontend/src/styles/product.css：本轮统一产品样式，在main.ts中加载于原theme.css之后。
- frontend/src/styles/theme.css：保留原共用布局与控件结构，旧紫色硬编码统一为中性底色。
- frontend/src/App.vue：共享外壳、导航、登录状态、帮助说明和素材说明。
- ResourceExplorer、LoginView、MyReservations、ReservationDetailView及创建/管理页：各页面内容层级与操作文案。
- SpaceScene：本地图像；IllustratedSpaceScene保留原插画；新增具体类型时先核对资源类型，不修改后端数据来配合展示。
- useCampus、API服务、路由与业务状态仍按既有约定扩展。资源/预约ID仍为字符串。

## 实际验证

本机phpStudy MySQL5.7、真实后端与Vite代理，使用Chromium检查学生搜索/空结果/清除筛选、从完整表单创建座位预约、模拟入场与离场完成、教室取消、地点二维码PNG与落地页自动签到；检查管理员资源/时段列表及新增弹窗，未执行管理员写操作。测试使用student02，新增记录11、12、13，保留历史，不改student01原记录。

1440、1024、820、768、390、320宽度的主要页面无整页横向溢出，未出现页面脚本错误；字体和全部图片能加载，减少动态效果设置生效。桌面/手机尺寸截图与browser-result.json在.artifacts/taste_redesign_2026-11-16。本机尺寸检查不等于真实手机摄像头扫码。本轮不改数据库迁移或后端代码，不重跑Java测试或声称新增MySQL8.4验收。
