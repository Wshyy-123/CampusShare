import { reactive } from "vue";
import type { CampusService, CampusState } from "../services/types";

export const API_NOT_INTEGRATED =
  "真实接口尚未接入。请在 api/campusApi.ts 对接共享后端；此模式不加载演示数据，也不会模拟操作成功。";

// 明确的接入点。待实际接口、DTO和身份统一后实现，不猜测当前并列模块的地址和字段。
export function createApiCampusService(): CampusService {
  const state = reactive<CampusState>({
    resources: [],
    bookings: [],
    occupancy: [],
    loading: false,
    ready: false,
    error: API_NOT_INTEGRATED,
    notice: "",
  });
  const notIntegrated = async (): Promise<never> => {
    throw new Error(API_NOT_INTEGRATED);
  };
  return {
    mode: "api",
    state,
    load: notIntegrated,
    loadAvailability: notIntegrated,
    slots: () => [],
    blocks: () => [],
    reserve: notIntegrated,
    addOccupancy: notIntegrated,
  };
}
