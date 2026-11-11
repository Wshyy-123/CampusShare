export const kindLabels = {
  LIBRARY_SEAT: "图书馆座位",
  CLASSROOM: "空闲教室",
  SPORTS: "体育场所",
  DISCUSSION_ROOM: "图书馆讨论室",
};
export const stateLabels = {
  RESERVED: "待签到",
  CHECKED_IN: "使用中",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
};

export function localDate(date = new Date()): string {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);
  const value = (type: string) =>
    parts.find((part) => part.type === type)?.value ?? "";
  return `${value("year")}-${value("month")}-${value("day")}`;
}

export function formatHour(hour: number): string {
  const minutes = Math.round(hour * 60);
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
}

export function dateAfter(days: number): string {
  return localDate(
    new Date(Date.parse(`${localDate()}T12:00:00+08:00`) + days * 86400000),
  );
}
export function formatDateTime(iso: string): string {
  const date = localDate(new Date(iso));
  const minutes =
    (Date.parse(iso) - Date.parse(`${date}T00:00:00+08:00`)) / 60000;
  return `${date} ${formatHour(minutes / 60)}`;
}

export const rules = {
  DISCUSSION_ROOM: [
    "按整间讨论室预约，最晚在使用日期前一自然日提交。",
    "当天不能预约当天；不套用座位30分钟或教室/体育15分钟签到规则。",
  ],
  LIBRARY_SEAT: [
    "预约成功起 30 分钟内模拟刷卡入场。",
    "不支持主动取消；入场后刷卡离场立即结束并释放座位。",
  ],
  CLASSROOM: [
    "预约账户扫描对应教室码签到。",
    "开始后 15 分钟未签到取消释放；预约结束自动完成。",
  ],
  SPORTS: [
    "预约账户扫描对应球桌或场地码签到。",
    "开始后 15 分钟未签到取消释放并记录违规；预约结束自动完成。",
  ],
};
