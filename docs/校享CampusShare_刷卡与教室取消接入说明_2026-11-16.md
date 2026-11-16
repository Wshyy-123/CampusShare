# 图书馆刷卡与教室取消接入说明

日期：2026-11-16。对应XIA-10、XIA-13，并补齐已确认的图书馆刷卡离场立即释放。保留现有统一主工程、Session/CSRF和资源锁，不新建后端或数据库。PO本轮确认教室开始前可取消，开始后或已签到不能取消。

## 页面操作

1. 使用API模式登录本人账户，从“我的预约”进入具体预约详情。
2. 座位详情出现“图书馆模拟刷卡”，显示具体座位。点击“模拟刷卡入场”登记到馆；已入场后显示“模拟刷卡离场并释放”。两种操作明确分开，不自动切换；不存在座位主动取消按钮。
3. 教室详情在未到开始时间且仍为RESERVED时显示“使用前取消”。取消成功后显示本人取消原因，列表与可用性缓存刷新，符合开放规则的时段可重新预约；开始后及已签到时不能取消。体育原取消行为不变。
4. 教室仍以地点QR签到，开始15分钟未到和已签到到期自动释放沿用VenueLifecycleJob。刷卡仅适用于LIBRARY_SEAT，不适用于讨论室、体育或教室。

## 图书馆规则

- 以当前登录账户和明确的预约、座位处理，不接受外部userId代替登录。按钮是课程项目的模拟校园卡入口，不接真实校园卡设备。
- 到馆窗口为[accepted_at, accepted_at+30分钟)，精确到服务端时间；到30分钟时拒绝，过期预约由既有SeatNoShowJob取消、释放、记录违规。不能用前端时间或错误的存储deadline放宽期限。
- 沿用原故事从预约成功计30分钟的规则，不额外改成从使用开始计时；在该窗口内允许确认未来时段座位，但已到预约结束不能再确认。
- 首次入场检查座位启用状态及该预约时段阻塞，状态更新和CARD_SIMULATION_ENTRY事件同事务。已确认的同一预约重复入场不重复事件，不切换为离场，不被未到任务取消。
- 已入场后，离场立即COMPLETED并写真实check_out_at与CARD_SIMULATION_EXIT事件，可早于预约结束，不记未到违规；超过预约结束仍可给已到馆记录补实际离场。未入场/取消不能离场，重复离场不重复事件或影响后续预约。
- 带明确预约ID的请求只处理该条记录；不通过LIMIT 1猜测用户要操作的预约，不清除其他预约、教学或停用安排。

## 接口

| 操作 | 请求 |
|---|---|
| 模拟刷卡入场 | POST /api/v1/library/reservations/{reservationId}/check-in |
| 模拟刷卡离场 | POST /api/v1/library/reservations/{reservationId}/check-out |
| 教室/体育使用前取消 | POST /api/v1/reservations/{reservationId}/cancel |

刷卡JSON必须有字符串resourceId和source="CARD_SIMULATION"，由服务端Session确定操作者，POST携带共享CSRF；返回统一ReservationVO。错误座位409、他人/不存在预约404、未登录401、缺CSRF403、缺字段或错误来源400；非法状态/超时409。旧/api/v1/library/check-in?userId=与/library/cancel?id=入口已移除，不再依赖开关隐藏不安全实现。扫码接口继续要求QR，不能用模拟刷卡来源代替。

先锁resource再锁reservation，锁后重读归属、类型、状态和时间，再条件更新，事件失败回滚；与未到、停用、创建预约共用顺序。教室取消只解除本次预约，USER_CANCEL重试幂等，NO_SHOW/RESOURCE_DISABLED不能被改为本人取消，不新增教室处罚。

## 文件与运行

后端修改原CheckInService/CheckInController，复用现有Mapper和事件表；SportsReservationService共享取消方法新增CLASSROOM，保留体育签到签退类型限制。旧LegacyLibraryTerminal替换为LibraryCardPanel，通过useCampus/libraryApi走共享身份与刷新；挂在预约详情，不加独立应用或固定ID终端。

成员拉取最新main后，前端npm ci，使用VITE_DATA_MODE=api；启动主后端并使用已应用001至007迁移的本地库。本次没有新增SQL，不重导已有建表脚本；demo模式不伪装刷卡或取消成功。手机扫码仍需此前说明中的手机可访问地址。

## 验证范围

后端新增24项集成场景，随主工程verify运行；本地合计163项，0失败/错误，3项原MySQL专用跳过。前端新增check-library，纳入check:api。实际启用Spring调度、隔离H2及Vite代理，并用headless Chromium点击登录、入/离场与教室取消；确认另一账户能重约释放后的座位/教室、旧ID重复请求不影响后来预约，检查桌面1440和手机宽390页面、无横向溢出及页面脚本错误。详见verification/Library-classroom-2026-11-16.md与PR自动检查。

原14条故事核心业务现均有主工程接入；不据此填写PingCode已验收。真实手机摄像头扫码、全站所有角色/原44条标准逐项人工验收、实际学校系统均不由这次自动验证替代。
