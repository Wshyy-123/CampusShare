import { localDate } from "./presentation";

export function isBookingDay(date: string, now = Date.now()): boolean {
  return date === localDate(new Date(now));
}

/** Hours in the campus timezone; never interpret a browser's local timezone as campus time. */
export function slotStart(date: string, hour: number): number {
  return Date.parse(`${date}T00:00:00+08:00`) + hour * 3_600_000;
}

export function hasStarted(
  date: string,
  hour: number,
  now = Date.now(),
): boolean {
  const start = slotStart(date, hour);
  return !Number.isFinite(start) || start < now;
}
