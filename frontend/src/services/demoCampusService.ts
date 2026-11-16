import { reactive } from "vue";
import { spaces } from "../data/preview";
import { localDate } from "../utils/presentation";
import type {
  CampusService,
  ManagedResource,
  ManagedResourcePage,
  ManagedResourceQuery,
  ResourceDraft,
} from "./types";
import type { Occupancy, Booking, SpaceKind } from "../types/campus";

const demoTypes: Record<SpaceKind, string> = {
  LIBRARY_SEAT: "LIBRARY_SEAT",
  CLASSROOM: "CLASSROOM",
  DISCUSSION_ROOM: "DISCUSSION_ROOM",
  SPORTS: "BADMINTON",
};
function kindOf(resourceType: string): SpaceKind {
  return resourceType === "LIBRARY_SEAT" ||
    resourceType === "CLASSROOM" ||
    resourceType === "DISCUSSION_ROOM"
    ? resourceType
    : "SPORTS";
}
import type {
  Schedule,
  ScheduleCreatePayload,
  ScheduleUpdatePayload,
} from "../types/api";

export function createDemoCampusService(
  notify: (message: string) => void = () => {},
): CampusService {
  const today = localDate();
  let nextDemoId = 2000;
  const state = reactive({
    resources: structuredClone(spaces),
    loading: false,
    error: "",
    ready: true,
    scheduleWritable: true,
    bookingTotal: 3,
    bookingPage: 1,
    bookingPageSize: 10,
    bookings: [
      {
        id: "1001",
        resourceId: "2",
        date: today,
        start: 18,
        end: 20,
        state: "RESERVED",
      },
      {
        id: "1002",
        resourceId: "1",
        date: today,
        start: 9,
        end: 12,
        state: "CHECKED_IN",
      },
      {
        id: "1003",
        resourceId: "6",
        date: today,
        start: 8,
        end: 9,
        state: "COMPLETED",
      },
    ] as Booking[],
    occupancy: [
      {
        id: "1",
        resourceId: "2",
        date: today,
        start: 8,
        end: 12,
        kind: "TEACHING",
        note: "模拟教务课表 · 软件工程课程",
      },
      {
        id: "2",
        resourceId: "3",
        date: today,
        start: 14,
        end: 16,
        kind: "EXTERNAL",
        note: "模拟场馆占用 · 社团训练",
      },
      {
        id: "3",
        resourceId: "6",
        date: today,
        start: 10,
        end: 12,
        kind: "TEACHING",
        note: "模拟体育教学占用",
      },
      {
        id: "4",
        resourceId: "1",
        date: today,
        start: 18,
        end: 19,
        kind: "MAINTENANCE",
        note: "模拟日常维护",
      },
    ] as Occupancy[],
    schedules: [
      {
        scheduleId: "s1",
        resourceId: "2",
        resourceName: "教学楼 · 301 教室",
        type: "TEACHING",
        startTime: `${today}T08:00:00+08:00`,
        endTime: `${today}T12:00:00+08:00`,
        note: "模拟教务课表 · 软件工程课程",
      },
      {
        scheduleId: "s2",
        resourceId: "6",
        resourceName: "北体育馆 · 乒乓球 3 号桌",
        type: "TEACHING",
        startTime: `${today}T10:00:00+08:00`,
        endTime: `${today}T12:00:00+08:00`,
        note: "模拟体育教学占用",
      },
      {
        scheduleId: "s3",
        resourceId: "2",
        resourceName: "教学楼 · 301 教室",
        type: "OPEN",
        startTime: `${today}T08:00:00+08:00`,
        endTime: `${today}T22:00:00+08:00`,
        note: "日常开放",
      },
    ] as Schedule[],
    notice: "",
  });

  // 资源管理演示数据：从当前样例资源生成，保存只更新本次页面内存。
  const managed = reactive<ManagedResource[]>(
    state.resources.map((space) => ({
      id: space.id,
      name: space.name,
      location: space.location,
      resourceType: space.resourceType ?? demoTypes[space.kind],
      unitLabel: space.code,
      enabled: !space.disabled,
    })),
  );

  async function loadManagedResources(
    query: ManagedResourceQuery,
  ): Promise<ManagedResourcePage> {
    const keyword = query.keyword?.trim().toLowerCase() ?? "";
    const filtered = managed.filter(
      (item) =>
        (!keyword ||
          [item.name, item.location, item.unitLabel].some((text) =>
            text.toLowerCase().includes(keyword),
          )) &&
        (!query.resourceType || item.resourceType === query.resourceType),
    );
    const start = (query.page - 1) * query.size;
    return {
      items: filtered.slice(start, start + query.size),
      total: filtered.length,
      page: query.page,
      size: query.size,
    };
  }

  async function saveResource(
    resourceId: string | null,
    draft: ResourceDraft,
  ): Promise<ManagedResource> {
    if (resourceId === null) {
      const created: ManagedResource = { id: String(++nextDemoId), ...draft };
      managed.push(created);
      state.resources.push({
        id: created.id,
        name: created.name,
        code: created.unitLabel,
        kind: kindOf(created.resourceType),
        resourceType: created.resourceType,
        location: created.location,
        tags: [],
        open: 8,
        close: 22,
        variant: 1,
      });
      return created;
    }
    const target = managed.find((item) => item.id === resourceId);
    if (!target) throw new Error("演示资源不存在，请刷新后重试");
    Object.assign(target, draft);
    const space = state.resources.find((item) => item.id === resourceId);
    if (space)
      Object.assign(space, {
        name: draft.name,
        location: draft.location,
        code: draft.unitLabel,
        kind: kindOf(draft.resourceType),
        resourceType: draft.resourceType,
        disabled: !draft.enabled,
      });
    return target;
  }

  function blocks(resourceId: string, date: string) {
    return [
      ...state.occupancy
        .filter((p) => p.resourceId === resourceId && p.date === date)
        .map((p) => ({
          ...p,
          label:
            p.kind === "TEACHING"
              ? "教学占用"
              : p.kind === "EXTERNAL"
                ? "外部占用"
                : "停用维护",
          style: p.kind.toLowerCase(),
        })),
      ...state.bookings
        .filter((b) => b.state === "RESERVED" || b.state === "CHECKED_IN")
        .filter((b) => b.resourceId === resourceId && b.date === date)
        .map((b) => ({ ...b, label: "系统预约", style: "reserved" })),
    ];
  }

  function slots(resourceId: string, date: string) {
    const space = state.resources.find((s) => s.id === resourceId);
    if (!space || !/^\d{4}-\d{2}-\d{2}$/.test(date)) return [];
    return Array.from({ length: space.close - space.open }, (_, i) => {
      const start = space.open + i;
      const block = blocks(resourceId, date).find(
        (p) => start < p.end && start + 1 > p.start,
      );
      return {
        start,
        end: start + 1,
        available: !space.disabled && !block,
        reason: space.disabled ? "场地维护中" : (block?.label ?? "可预约"),
      };
    });
  }

  async function reserve(
    resourceId: string,
    date: string,
    start: number,
  ): Promise<boolean> {
    if (date < localDate()) {
      notify("请选择今天或之后的日期。");
      return false;
    }
    if (!slots(resourceId, date).find((s) => s.start === start)?.available) {
      notify("该演示时段已被占用，请重新选择。");
      return false;
    }
    state.bookings.unshift({
      id: String(++nextDemoId),
      resourceId,
      date,
      start,
      end: start + 1,
      state: "RESERVED",
    });
    notify("已添加演示预约，可在“我的预约”查看。仅保存在本次页面中。");
    return true;
  }

  async function addOccupancy(
    resourceId: string,
    date: string,
    start: number,
    end: number,
    kind: Occupancy["kind"],
    note: string,
  ): Promise<boolean> {
    if (
      !state.resources.some((s) => s.id === resourceId) ||
      !/^\d{4}-\d{2}-\d{2}$/.test(date) ||
      !Number.isFinite(start) ||
      !Number.isFinite(end) ||
      start >= end ||
      start < 8 ||
      end > 22
    )
      return false;
    if (
      state.bookings
        .filter((b) => b.state === "RESERVED" || b.state === "CHECKED_IN")
        .some(
          (b) =>
            b.resourceId === resourceId &&
            b.date === date &&
            start < b.end &&
            end > b.start,
        )
    ) {
      notify("该时段与已有演示预约冲突，请换一个时间。");
      return false;
    }
    state.occupancy.push({
      id: String(++nextDemoId),
      resourceId,
      date,
      start,
      end,
      kind,
      note: note || "管理员添加的演示占用",
    });
    notify("演示占用已添加，资源详情中的时段同步更新。");
    return true;
  }

  async function loadSchedules(
    _date: string,
    _resourceId?: string,
  ): Promise<void> {
    // 演示数据仅保存在页面内存，列表由页面按筛选条件过滤。
  }

  async function createSchedule(
    payload: ScheduleCreatePayload,
  ): Promise<boolean> {
    const resource = state.resources.find((s) => s.id === payload.resourceId);
    if (!resource) {
      notify("请选择有效资源。");
      return false;
    }
    if (
      !payload.startTime ||
      !payload.endTime ||
      Date.parse(payload.startTime) >= Date.parse(payload.endTime)
    ) {
      notify("开始时间必须早于结束时间。");
      return false;
    }
    state.schedules.push({
      scheduleId: String(++nextDemoId),
      resourceId: payload.resourceId,
      resourceName: resource.name,
      type: payload.type,
      startTime: payload.startTime,
      endTime: payload.endTime,
      note: payload.note ?? "",
    });
    notify("演示时段安排已保存，仅保存在本次页面中。");
    return true;
  }

  async function updateSchedule(
    scheduleId: string,
    payload: ScheduleUpdatePayload,
  ): Promise<boolean> {
    const index = state.schedules.findIndex((s) => s.scheduleId === scheduleId);
    if (index < 0) {
      notify("未找到要修改的时段安排。");
      return false;
    }
    if (
      !payload.startTime ||
      !payload.endTime ||
      Date.parse(payload.startTime) >= Date.parse(payload.endTime)
    ) {
      notify("开始时间必须早于结束时间。");
      return false;
    }
    const prev = state.schedules[index];
    state.schedules[index] = {
      ...prev,
      type: payload.type,
      startTime: payload.startTime,
      endTime: payload.endTime,
      note: payload.note ?? "",
    };
    notify("演示时段安排已更新。");
    return true;
  }

  async function deleteSchedule(scheduleId: string): Promise<boolean> {
    const index = state.schedules.findIndex((s) => s.scheduleId === scheduleId);
    if (index < 0) {
      notify("未找到要删除的时段安排。");
      return false;
    }
    state.schedules.splice(index, 1);
    notify("演示时段安排已删除。");
    return true;
  }

  return {
    mode: "demo",
    state,
    load: async () => {},
    reset: () => {},
    loadBookings: async () => {
      state.bookingTotal = state.bookings.length;
    },
    loadAvailability: async () => {},
    blocks,
    slots,
    reserve,
    loadManagedResources,
    saveResource,
    addOccupancy,
    disableResource: async () => {
      throw new Error("停用场地需要使用后端接口模式；演示数据不会写入数据库。");
    },
    loadSchedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
  };
}
