// 前端展示模型；SPORTS 是界面分组，不替代后端具体资源类型。
export type SpaceKind = "LIBRARY_SEAT" | "CLASSROOM" | "SPORTS";
export type BookingState =
  "RESERVED" | "CHECKED_IN" | "COMPLETED" | "CANCELLED";
export interface Space {
  id: number;
  name: string;
  code: string;
  kind: SpaceKind;
  location: string;
  capacity: number;
  tags: string[];
  open: number;
  close: number;
  disabled?: boolean;
  variant: number;
}
export interface Occupancy {
  id: number;
  resourceId: number;
  date: string;
  start: number;
  end: number;
  kind: "TEACHING" | "EXTERNAL" | "MAINTENANCE";
  note: string;
}
export interface Booking {
  id: number;
  resourceId: number;
  date: string;
  start: number;
  end: number;
  state: BookingState;
}
