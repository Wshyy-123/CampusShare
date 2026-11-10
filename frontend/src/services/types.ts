import type { Booking, Occupancy, Space } from "../types/campus";

export type DataMode = "demo" | "api";
export interface TimeSlot {
  start: number;
  end: number;
  available: boolean;
  reason: string;
}
export interface OccupancyBlock {
  id: number;
  start: number;
  end: number;
  label: string;
  style: string;
}
export interface CampusState {
  resources: Space[];
  bookings: Booking[];
  occupancy: Occupancy[];
  loading: boolean;
  error: string;
  ready: boolean;
  notice: string;
}

// 展示层服务契约。后端DTO通过API适配层转换，不能将前端分组或小时数字直接写进数据库。
export interface CampusService {
  mode: DataMode;
  state: CampusState;
  load(): Promise<void>;
  loadAvailability(resourceId: number, date: string): Promise<void>;
  // API模式须读取服务端返回的可用性缓存；不能在浏览器代替服务端裁定冲突。
  slots(resourceId: number, date: string): TimeSlot[];
  blocks(resourceId: number, date: string): OccupancyBlock[];
  reserve(resourceId: number, date: string, start: number): Promise<boolean>;
  addOccupancy(
    resourceId: number,
    date: string,
    start: number,
    end: number,
    kind: Occupancy["kind"],
    note: string,
  ): Promise<boolean>;
}
