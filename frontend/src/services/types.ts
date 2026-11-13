import type {
  Schedule,
  ScheduleCreatePayload,
  ScheduleUpdatePayload,
} from "../types/api";
import type { Booking, Occupancy, Space } from "../types/campus";

export type DataMode = "demo" | "api";
export interface TimeSlot {
  start: number;
  end: number;
  available: boolean;
  reason: string;
}
export interface OccupancyBlock {
  id: string;
  start: number;
  end: number;
  label: string;
  style: string;
}
export interface CampusState {
  resources: Space[];
  bookings: Booking[];
  occupancy: Occupancy[];
  schedules: Schedule[];
  loading: boolean;
  error: string;
  ready: boolean;
  notice: string;
  scheduleWritable: boolean;
  bookingTotal: number;
  bookingPage: number;
  bookingPageSize: number;
}

// XIA-2 资源管理页使用的展示模型：保留原始资源类型和编号标签，便于回填表单。
export interface ManagedResource {
  id: string;
  name: string;
  location: string;
  resourceType: string;
  unitLabel: string;
  enabled: boolean;
}
export interface ManagedResourceQuery {
  page: number;
  size: number;
  keyword?: string;
  resourceType?: string;
}
export interface ManagedResourcePage {
  items: ManagedResource[];
  total: number;
  page: number;
  size: number;
}
export interface ResourceDraft {
  name: string;
  location: string;
  resourceType: string;
  unitLabel: string;
  enabled: boolean;
}

export interface DisableResourceResult {
  resourceId: string;
  cancelledReservationIds: string[];
}

// 展示层服务契约。后端DTO通过API适配层转换，不能将前端分组或小时数字直接写进数据库。
export interface CampusService {
  mode: DataMode;
  state: CampusState;
  load(): Promise<void>;
  reset(): void;
  loadBookings(status?: string, page?: number, size?: number): Promise<void>;
  loadAvailability(resourceId: string, date: string): Promise<void>;
  // API模式须读取服务端返回的可用性缓存；不能在浏览器代替服务端裁定冲突。
  slots(resourceId: string, date: string): TimeSlot[];
  blocks(resourceId: string, date: string): OccupancyBlock[];
  reserve(resourceId: string, date: string, start: number): Promise<boolean>;
  disableResource(
    resourceId: string,
    startTime: string,
    endTime: string,
    reason: string,
  ): Promise<DisableResourceResult>;
  loadSchedules(date: string, resourceId?: string): Promise<void>;
  createSchedule(payload: ScheduleCreatePayload): Promise<boolean>;
  updateSchedule(
    scheduleId: string,
    payload: ScheduleUpdatePayload,
  ): Promise<boolean>;
  deleteSchedule(scheduleId: string, type: Schedule["type"]): Promise<boolean>;
  addOccupancy(
    resourceId: string,
    date: string,
    start: number,
    end: number,
    kind: Occupancy["kind"],
    note: string,
  ): Promise<boolean>;
  // 资源维护：真实模式由服务端校验 RESOURCE_ADMIN，演示模式只改本地样例。
  loadManagedResources(
    query: ManagedResourceQuery,
  ): Promise<ManagedResourcePage>;
  saveResource(
    resourceId: string | null,
    draft: ResourceDraft,
  ): Promise<ManagedResource>;
}
