# PingCode 工作项 XIA-3

## 标题
维护开放与教学占用时段

## 建议任务拆分
- XIA-3-1：设计 `schedule_period` 数据模型
- XIA-3-2：实现新增/修改/删除时段接口
- XIA-3-3：实现日期查询与时间合法性校验
- XIA-3-4：管理员权限和接口联调

## 建议提交
- `XIA-3 feat: add schedule period domain and repository`
- `XIA-3 feat: add admin schedule CRUD APIs`
- `XIA-3 fix: validate schedule start and end time`

## PR 验收
- 非管理员不能写入；
- `startAt >= endAt` 必须失败；
- OPEN/TEACHING 均能新增、查询、修改、删除；
- 数据保存在 `schedule_period`，接口不直接暴露 JPA Entity。
