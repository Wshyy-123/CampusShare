import { computed } from "vue";
import { createDemoCampusService } from "../services/demoCampusService";
import { createApiCampusService } from "../api/campusApi";

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
    : createApiCampusService();
if (!["demo", "api"].includes(configuredMode))
  service.state.error =
    "VITE_DATA_MODE只能为demo或api；配置错误时不会加载演示数据。";

async function run<T>(action: () => Promise<T>): Promise<T | undefined> {
  try {
    return await action();
  } catch (error) {
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
    initialize: async () => {
      if (!["demo", "api"].includes(configuredMode)) return;
      service.state.loading = true;
      await run(() => service.load());
      service.state.loading = false;
    },
    loadAvailability: (id: number, date: string) =>
      run(() => service.loadAvailability(id, date)),
    slots: service.slots,
    blocks: service.blocks,
    reserve: async (...args: Parameters<typeof service.reserve>) =>
      (await run(() => service.reserve(...args))) ?? false,
    addOccupancy: async (...args: Parameters<typeof service.addOccupancy>) =>
      (await run(() => service.addOccupancy(...args))) ?? false,
  };
}
