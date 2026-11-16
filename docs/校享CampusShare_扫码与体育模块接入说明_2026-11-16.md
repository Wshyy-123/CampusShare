# 扫码与体育模块接入说明

2026-11-16后续：图书馆模拟刷卡入/离场及教室使用前取消统一按[刷卡与教室取消接入说明](校享CampusShare_刷卡与教室取消接入说明_2026-11-16.md)，已替换旧信任userId入口；已有001至007无需新增SQL。历史未接入描述以此更新，验收边界保持如实记录。

日期：2026-11-16。以同学feature/XIA-7-XIA-8为来源，保留历史并修正当前主工程。业务依据仍是《项目背景与业务规则》，导航、共享身份和数据遵循现有主工程约定。

## 当前功能

- 体育本人使用前取消、重复取消幂等，保留原因与事件；座位和教室不能借体育取消接口取消。
- 每个教室、球桌和场地有稳定的`/places/{resourceId}`地点链接。资源详情可显示/保存对应地点二维码，二维码携带具体地点，不包含账户。
- 手机系统相机或支持识别网址的扫码工具扫描现场码，进入网页。未登录先登录，再回到原地点；页面可见后自动核验并POST签到。GET及页面服务端预览不写状态，重复扫码不自动签退。
- 后端取当前账户，验证本人预约、扫描地点、资源类型、开始时间、15分钟期限、结束时间、状态和停用/占用；签到保存操作者、服务端时间、预约及QR来源。静态码不独立证明物理在场。
- 教室/体育开始起15分钟未签到自动取消，体育记违规，教室不新增处罚。已签到的教室/体育到end_at自动COMPLETED，不记未到违规，也不伪造check_out_at。
- 体育在预约结束后可扫描地点码并显式选择记录签退；提前签退不能完成并释放。自动完成后补签退仅补记录，不产生第二条完成事件。

## 时间与并发

签到窗口为`[start_at, min(start_at+15分钟,end_at))`。10分钟预约结束后也不能再签到；未签到记录仍按开始起15分钟处理，不提前增加未到处罚，预约占用不延伸到end_at之后。

所有写入口与后台遵循资源→预约锁顺序，锁后重读，用状态条件写入；状态、事件、违规一致事务提交。后台按资源和状态分批100条、单条失败回滚，下轮重试，启动补查积压。正常结束与手动签退并发只产生一次完成。

## 运行和手机访问

- Java21、Node24、npm、MySQL8.4基线不变。本次无新SQL，已有001至007的库不需要额外迁移，不能重复导入建表脚本。
- 前端新增qrcode及类型依赖，拉取后在frontend执行`npm ci`。默认demo不登记真实到场；设置`VITE_DATA_MODE=api`并运行主后端。
- 手机与开发电脑同网，前端以`npm run dev -- --host 0.0.0.0 --port 5173 --strictPort`启动；用手机能够访问的电脑局域网地址或已部署站点打开资源页，再生成地点码。Vite仍将/api请求代理给主后端。
- 可在个人.env.local设置`VITE_PUBLIC_BASE_URL`为手机可访问的站点根地址后重启；不填使用当前浏览器地址。localhost/127.0.0.1属于当前设备，不能把电脑的回环地址当手机可用地址。
- 正式任务配置`campusshare.venue-lifecycle.enabled=true`，默认10秒扫描；测试可在独立库临时加速，不能改15分钟业务期限。座位30分钟任务沿用原配置。旧信任userId的图书馆入口继续关闭。

## 接口和成员开发入口

| 操作 | 接口 |
|---|---|
| 识别地点、查询当前账户可操作预约 | GET /api/v1/places/{resourceId}，可选本人reservationId |
| 体育本人使用前取消 | POST /api/v1/reservations/{reservationId}/cancel |
| 教室/体育地点签到 | POST /api/v1/reservations/{reservationId}/check-in |
| 体育结束后登记签退 | POST /api/v1/reservations/{reservationId}/check-out |

签到/签退请求体必须有字符串`resourceId`与`source:"QR"`，身份来自Session，写请求带共享CSRF；不能只传预约ID就当扫码。契约见docs/api/sports-attendance.yaml。事件的reason保留QR_CHECK_IN/QR_CHECK_OUT与地点ID，状态数据复用现有字段。

前端扩展`useCampus`、`campusApi`、`placesApi`及`PlaceAttendanceView`，地点码生成复用`PlaceCodeDialog`。后端沿用同学的SportsReservationService、SportsNoShowService，后者兼管教室/体育生命周期，统一由job/VenueLifecycleJob调度。旧SportsTimeoutTask已由新任务替换，不并行再开一套扫描。

## 验证与剩余工作

具体结果见verification/Sports-integration-2026-11-16.md，原作者的旧截图、测试计划和XLSX保留为历史材料，不能覆盖新接口约束。本次没有独立手机真机验收，不能仅凭代码、PNG机器解码或API联调认定真实扫码验收完成。

原故事中体育取消/签到签退/未到核心接入已有；图书馆刷卡入场和刷卡离场、教室主动取消仍是后续工作。其他成员从最新main继续扩展，不能恢复旧提前签退立即释放、固定userId或单靠前端时间的实现。
