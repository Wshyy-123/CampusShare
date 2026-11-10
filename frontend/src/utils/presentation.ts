export const kindLabels = {
  LIBRARY_SEAT: "图书馆座位",
  CLASSROOM: "空闲教室",
  SPORTS: "体育场所",
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
  return `${String(Math.floor(hour)).padStart(2, "0")}:${hour % 1 ? "30" : "00"}`;
}

export const rules = {
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
