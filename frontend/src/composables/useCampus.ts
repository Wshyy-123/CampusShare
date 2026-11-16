import { computed } from "vue";
import { createDemoCampusService } from "../services/demoCampusService";
import { createApiCampusService } from "../api/campusApi";
import { ApiError } from "../api/request";
import type { ManagedResourceQuery, ResourceDraft } from "../services/types";

let noticeTimer: ReturnType<typeof setTimeout> | undefined;
export function notify(message: string) {
  service.state.notice = message;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => {
    service.state.notice = "";
  }, 5000);
}

const configuredMode = import.meta.env.VITE_DATA_MODE ?? "demo";
const service =
  configuredMode === "demo"
    ? createDemoCampusService(notify)
    : createApiCampusService(notify);
let initializePromise: Promise<void> | undefined;
if (!["demo", "api"].includes(configuredMode))
  service.state.error =
    "VITE_DATA_MODE只能为demo或api；配置错误时不会加载演示数据。";

async function run<T>(action: () => Promise<T>): Promise<T | undefined> {
  service.state.error = "";
  try {
    return await action();
  } catch (error) {
    if (error instanceof ApiError && error.code === "AUTH_CHANGED")
      return undefined;
    service.state.error =
      error instanceof Error ? error.message : "操作失败，请重试";
    notify(service.state.error);
    return undefined;
  }
}

export function useCampus() {
  return {
    state: service.state,
    isDemo: service.mode === "demo",
    activeBookings: computed(() =>
      service.state.bookings.filter(
        (b) => b.state === "RESERVED" || b.state === "CHECKED_IN",
      ),
    ),
    initialize: async (force = false) => {
      if (
        !["demo", "api"].includes(configuredMode) ||
        (!force && service.state.ready)
      )
        return;
      if (initializePromise) return initializePromise;
      service.state.loading = true;
      const task = (async () => {
        await run(() => service.load());
      })().finally(() => {
        if (initializePromise === task) {
          service.state.loading = false;
          initializePromise = undefined;
        }
      });
      initializePromise = task;
      return task;
    },
    reset: () => {
      clearTimeout(noticeTimer);
      initializePromise = undefined;
      service.reset();
    },
    loadBookings: async (...args: Parameters<typeof service.loadBookings>) =>
      (await run(async () => {
        await service.loadBookings(...args);
        return true;
      })) ?? false,
    loadAvailability: async (id: string, date: string) =>
      (await run(async () => {
        await service.loadAvailability(id, date);
        return true;
      })) ?? false,
    slots: service.slots,
    blocks: service.blocks,
    reserve: async (...args: Parameters<typeof service.reserve>) =>
      (await run(() => service.reserve(...args))) ?? false,
    disableResource: async (
      ...args: Parameters<typeof service.disableResource>
    ) => run(() => service.disableResource(...args)),
    addOccupancy: async (...args: Parameters<typeof service.addOccupancy>) =>
      (await run(() => service.addOccupancy(...args))) ?? false,
    loadManagedResources: async (query: ManagedResourceQuery) =>
      run(() => service.loadManagedResources(query)),
    saveResource: async (resourceId: string | null, draft: ResourceDraft) =>
      run(() => service.saveResource(resourceId, draft)),
    loadSchedules: async (...args: Parameters<typeof service.loadSchedules>) =>
      (await run(async () => {
        await service.loadSchedules(...args);
        return true;
      })) ?? false,
    createSchedule: async (
      ...args: Parameters<typeof service.createSchedule>
    ) => (await run(() => service.createSchedule(...args))) ?? false,
    updateSchedule: async (
      ...args: Parameters<typeof service.updateSchedule>
    ) => (await run(() => service.updateSchedule(...args))) ?? false,
    deleteSchedule: async (
      ...args: Parameters<typeof service.deleteSchedule>
    ) => (await run(() => service.deleteSchedule(...args))) ?? false,
  };
}
