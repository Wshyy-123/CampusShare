# PingCode 工作项 XIA-4

## 标题
查询资源和可用时段

## 建议任务拆分
- XIA-4-1：资源列表、筛选与资源详情接口
- XIA-4-2：读取 OPEN/TEACHING、预约、停用数据
- XIA-4-3：实现时间区间合并与差集算法
- XIA-4-4：实现资源/预约单位按日期的可用时段接口
- XIA-4-5：空状态、停用状态和联调测试

## 建议提交
- `XIA-4 feat: add resource query APIs`
- `XIA-4 feat: calculate available time ranges`
- `XIA-4 test: cover interval merge and subtraction`

## PR 验收
- 指定资源和日期可返回开放、占用、可用结果；
- 教学占用、有效预约、停用区间均从开放时间中扣除；
- 无可用时段时返回明确消息；
- DISABLED 资源不可预约；
- 查询计算由后端完成，不依赖前端自行判断。
