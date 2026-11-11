import { reactive } from "vue";
import { resourcesApi } from "./resources";
import { reservationsApi } from "./reservations";
import { schedulesApi } from "./schedules";
import { HttpError } from "./http";
import { localDate } from "../utils/presentation";
import type {
  Availability,
  Resource,
  Reservation,
  Interval,
  Schedule,
  ScheduleCreatePayload,
  ScheduleUpdatePayload,
} from "../types/api";
import type { Booking, Space, SpaceKind } from "../types/campus";
import type {
  CampusService,
  CampusState,
  OccupancyBlock,
} from "../services/types";

const hourMs = 3600000;
const kinds: Record<string, SpaceKind> = {
  LIBRARY_SEAT: "LIBRARY_SEAT",
  CLASSROOM: "CLASSROOM",
  BADMINTON: "SPORTS",
  TABLE_TENNIS: "SPORTS",
  TENNIS: "SPORTS",
  DISCUSSION_ROOM: "DISCUSSION_ROOM",
};
function requireValue(condition: unknown): asserts condition {
  if (!condition)
    throw new HttpError(
      "BAD_RESPONSE",
      "服务端返回的数据格式不符合当前接口约定",
    );
}
function id(value: unknown): asserts value is string {
  requireValue(typeof value === "string" && /^[1-9][0-9]*$/.test(value));
}
function instant(value: unknown): asserts value is string {
  requireValue(
    typeof value === "string" &&
      /(?:Z|[+-]\d{2}:\d{2})$/.test(value) &&
      Number.isFinite(Date.parse(value)),
  );
}
function hour(value: string, date: string) {
  return (Date.parse(value) - Date.parse(`${date}T00:00:00+08:00`)) / hourMs;
}
function interval(value: Interval) {
  requireValue(value && typeof value === "object");
  instant(value.startTime);
  instant(value.endTime);
  requireValue(Date.parse(value.startTime) < Date.parse(value.endTime));
}
function scheduleToState(value: unknown): Schedule {
  requireValue(value && typeof value === "object");
  const v = value as Record<string, unknown>;
  id(v.scheduleId);
  id(v.resourceId);
  instant(v.startTime);
  instant(v.endTime);
  requireValue(["OPEN", "TEACHING"].includes(v.type as string));
  requireValue(typeof v.resourceName === "string");
  requireValue(v.note === null || typeof v.note === "string");
  return {
    scheduleId: v.scheduleId,
    resourceId: v.resourceId,
    resourceName: v.resourceName,
    type: v.type as Schedule["type"],
    startTime: v.startTime,
    endTime: v.endTime,
    note: v.note ?? "",
  };
}
export function resourceToSpace(value: Resource, variant = 1): Space {
  requireValue(value && typeof value === "object");
  id(value.resourceId);
  requireValue(
    typeof value.name === "string" &&
      typeof value.location === "string" &&
      typeof value.unitLabel === "string" &&
      typeof value.enabled === "boolean" &&
      Object.hasOwn(kinds, value.resourceType),
  );
  return {
    id: value.resourceId,
    name: value.name,
    code: value.unitLabel || value.resourceId,
    location: value.location,
    kind: kinds[value.resourceType]!,
    resourceType: value.resourceType,
    tags: value.unitLabel ? [value.unitLabel] : [],
    open: 0,
    close: 0,
    disabled: !value.enabled,
    variant,
  };
}
export function reservationToBooking(value: Reservation): Booking {
  requireValue(value && typeof value === "object");
  id(value.reservationId);
  id(value.resourceId);
  instant(value.startTime);
  instant(value.endTime);
  requireValue(
    ["RESERVED", "CHECKED_IN", "COMPLETED", "CANCELLED"].includes(
      value.status,
    ) && Date.parse(value.startTime) < Date.parse(value.endTime),
  );
  if (value.arrivalDeadline !== null) instant(value.arrivalDeadline);
  const date = localDate(new Date(value.startTime));
  return {
    id: value.reservationId,
    resourceId: value.resourceId,
    date,
    start: hour(value.startTime, date),
    end: hour(value.endTime, date),
    startTime: value.startTime,
    endTime: value.endTime,
    arrivalDeadline: value.arrivalDeadline,
    state: value.status,
  };
}

export function createApiCampusService(
  notify: (message: string) => void = () => {},
): CampusService {
  const state = reactive<CampusState>({
    resources: [],
    bookings: [],
    occupancy: [],
    schedules: [],
    loading: false,
    ready: false,
    error: "",
    notice: "",
    scheduleWritable: false,
    bookingTotal: 0,
    bookingPage: 1,
    bookingPageSize: 10,
  });
  const cache = reactive<Record<string, Availability>>({});
  const pending = new Map<string, string>();
  const inFlight = new Map<string, Promise<void>>();
  let epoch = 0;
  let bookingRequest = 0;
  function reset() {
    epoch++;
    bookingRequest++;
    pending.clear();
    inFlight.clear();
    for (const key of Object.keys(cache)) delete cache[key];
    Object.assign(state, {
      resources: [],
      bookings: [],
      occupancy: [],
      schedules: [],
      ready: false,
      loading: false,
      error: "",
      notice: "",
      bookingTotal: 0,
      bookingPage: 1,
      bookingPageSize: 10,
    });
  }
  function current(version: number) {
    if (version !== epoch)
      throw new HttpError("AUTH_CHANGED", "登录状态已变化，请重新加载");
  }
  async function loadBookings(status?: string, page = 1, size = 10) {
    const version = epoch;
    const sequence = ++bookingRequest;
    const result = await reservationsApi.mine({ status, page, size });
    current(version);
    requireValue(
      result &&
        Array.isArray(result.items) &&
        Number.isSafeInteger(result.total) &&
        result.total >= 0 &&
        result.page === page &&
        result.size === size,
    );
    const bookings = result.items.map(reservationToBooking);
    const missing = [...new Set(bookings.map((b) => b.resourceId))].filter(
      (resourceId) => !state.resources.some((s) => s.id === resourceId),
    );
    const extras = await Promise.all(
      missing.map(async (resourceId) => {
        const value = await resourcesApi.get(resourceId);
        requireValue(value.resourceId === resourceId);
        return resourceToSpace(value);
      }),
    );
    current(version);
    if (sequence !== bookingRequest) return;
    for (const extra of extras)
      if (!state.resources.some((s) => s.id === extra.id))
        state.resources.push(extra);
    Object.assign(state, {
      bookings,
      bookingTotal: result.total,
      bookingPage: result.page,
      bookingPageSize: result.size,
    });
  }
  async function loadAvailability(
    resourceId: string,
    date: string,
  ): Promise<void> {
    id(resourceId);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date))
      throw new HttpError("INVALID_ARGUMENT", "请选择有效日期");
    const key = `${resourceId}/${date}`;
    if (inFlight.has(key)) return inFlight.get(key)!;
    delete cache[key];
    const version = epoch;
    const task = (async () => {
      const value = await resourcesApi.availability(resourceId, date);
      current(version);
      requireValue(
        value &&
          value.resource?.resourceId === resourceId &&
          value.date === date &&
          Array.isArray(value.openings) &&
          Array.isArray(value.blocks) &&
          Array.isArray(value.reservations) &&
          Array.isArray(value.availablePeriods),
      );
      value.openings.forEach(interval);
      value.blocks.forEach(interval);
      value.reservations.forEach(interval);
      value.availablePeriods.forEach(interval);
      const space = resourceToSpace(value.resource);
      const original = state.resources.find((s) => s.id === resourceId);
      if (original) {
        space.variant = original.variant;
        Object.assign(original, space, {
          open: value.openings.length
            ? Math.max(
                0,
                Math.min(...value.openings.map((v) => hour(v.startTime, date))),
              )
            : 0,
          close: value.openings.length
            ? Math.min(
                24,
                Math.max(...value.openings.map((v) => hour(v.endTime, date))),
              )
            : 0,
        });
      }
      cache[key] = value;
      state.occupancy = state.occupancy.filter(
        (p) => p.resourceId !== resourceId || p.date !== date,
      );
      state.occupancy.push(
        ...value.blocks.map((b, i) => ({
          id: `${key}/block-${i}`,
          resourceId,
          date,
          start: Math.max(0, hour(b.startTime, date)),
          end: Math.min(24, hour(b.endTime, date)),
          kind:
            b.blockType === "TEACHING"
              ? ("TEACHING" as const)
              : b.blockType === "EXTERNAL"
                ? ("EXTERNAL" as const)
                : ("MAINTENANCE" as const),
          note: b.reason,
        })),
      );
    })().finally(() => {
      if (version === epoch) inFlight.delete(key);
    });
    inFlight.set(key, task);
    return task;
  }
  function blocks(resourceId: string, date: string): OccupancyBlock[] {
    const value = cache[`${resourceId}/${date}`];
    if (!value) return [];
    return [
      ...value.blocks.map((b, i) => ({
        id: `block-${i}`,
        start: Math.max(0, hour(b.startTime, date)),
        end: Math.min(24, hour(b.endTime, date)),
        label:
          b.blockType === "TEACHING"
            ? "教学占用"
            : b.blockType === "EXTERNAL"
              ? "外部占用"
              : "停用维护",
        style:
          b.blockType === "TEACHING"
            ? "teaching"
            : b.blockType === "EXTERNAL"
              ? "external"
              : "maintenance",
      })),
      ...value.reservations.map((b) => ({
        id: b.reservationId,
        start: Math.max(0, hour(b.startTime, date)),
        end: Math.min(24, hour(b.endTime, date)),
        label: "系统预约",
        style: "reserved",
      })),
    ];
  }
  function slots(resourceId: string, date: string) {
    const value = cache[`${resourceId}/${date}`];
    if (!value || !value.openings.length) return [];
    const lower = Math.max(
      0,
      Math.floor(
        Math.min(...value.openings.map((v) => hour(v.startTime, date))),
      ),
    );
    const upper = Math.min(
      24,
      Math.ceil(Math.max(...value.openings.map((v) => hour(v.endTime, date)))),
    );
    return Array.from({ length: Math.max(0, upper - lower) }, (_, i) => {
      const start = lower + i;
      const end = start + 1;
      const available =
        value.resource.enabled &&
        value.availablePeriods.some(
          (v) =>
            hour(v.startTime, date) <= start && hour(v.endTime, date) >= end,
        );
      return {
        start,
        end,
        available,
        reason: !value.resource.enabled
          ? "场地已停用"
          : available
            ? "可预约"
            : (blocks(resourceId, date).find(
                (b) => start < b.end && end > b.start,
              )?.label ?? "未开放或已过时"),
      };
    });
  }
  async function load() {
    const version = epoch;
    state.ready = false;
    const result = await resourcesApi.list();
    current(version);
    requireValue(Array.isArray(result));
    const resources = result.map((r, i) => resourceToSpace(r, i + 1));
    state.resources = resources;
    await loadBookings();
    current(version);
    await Promise.all(
      state.resources.map((r) => loadAvailability(r.id, localDate())),
    );
    current(version);
    state.ready = true;
    state.error = "";
  }
  async function reserve(resourceId: string, date: string, start: number) {
    if (!slots(resourceId, date).some((s) => s.start === start && s.available))
      throw new HttpError("RESOURCE_UNAVAILABLE", "该时段不可预约，请重新加载");
    const version = epoch;
    const key = `${resourceId}/${date}/${start}`;
    const clientRequestId = pending.get(key) ?? crypto.randomUUID();
    pending.set(key, clientRequestId);
    const startTime = new Date(
      Date.parse(`${date}T00:00:00+08:00`) + start * hourMs,
    ).toISOString();
    const endTime = new Date(Date.parse(startTime) + hourMs).toISOString();
    let booking: Booking;
    try {
      booking = reservationToBooking(
        await reservationsApi.create({
          resourceId,
          startTime,
          endTime,
          clientRequestId,
        }),
      );
    } catch (error) {
      if (error instanceof HttpError && error.status === 409)
        await loadAvailability(resourceId, date).catch(() => {});
      throw error;
    }
    current(version);
    pending.delete(key);
    state.bookings = [
      booking,
      ...state.bookings.filter((b) => b.id !== booking.id),
    ];
    const refresh = await Promise.allSettled([
      loadBookings(),
      loadAvailability(resourceId, date),
    ]);
    current(version);
    if (refresh.some((r) => r.status === "rejected"))
      state.error = "预约已成功，但列表刷新失败，请重新加载后查看。";
    notify("预约已由后端确认，可在“我的预约”查看。");
    return true;
  }
  async function loadSchedules(date: string, resourceId?: string): Promise<void> {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date))
      throw new HttpError("INVALID_ARGUMENT", "请选择有效日期");
    const result = await schedulesApi.list({ date, resourceId });
    requireValue(Array.isArray(result));
    state.schedules = result.map(scheduleToState);
  }
  async function createSchedule(payload: ScheduleCreatePayload): Promise<boolean> {
    await schedulesApi.create(payload);
    notify("时段安排已由后端保存。");
    return true;
  }
  async function updateSchedule(
    scheduleId: string,
    payload: ScheduleUpdatePayload,
  ): Promise<boolean> {
    await schedulesApi.update(scheduleId, payload);
    notify("时段安排已由后端更新。");
    return true;
  }
  async function deleteSchedule(scheduleId: string): Promise<boolean> {
    await schedulesApi.remove(scheduleId);
    notify("时段安排已由后端删除。");
    return true;
  }
  return {
    mode: "api",
    state,
    reset,
    load,
    loadBookings,
    loadAvailability,
    slots,
    blocks,
    reserve,
    addOccupancy: async () => {
      throw new HttpError(
        "NOT_IMPLEMENTED",
        "占用维护写接口尚未接入，当前只能查看服务端占用",
      );
    },
    loadSchedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
  };
}
