import { reactive } from "vue";
import { spaces } from "../data/preview";
import { localDate } from "../utils/presentation";
import type { CampusService } from "./types";
import type { Occupancy, Booking } from "../types/campus";

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
    notice: "",
  });

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
    addOccupancy,
    disableResource: async () => {
      throw new Error("停用场地需要使用后端接口模式；演示数据不会写入数据库。");
    },
  };
}
