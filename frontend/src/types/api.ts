export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  requestId: string;
}

export interface PageData<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface CurrentUser {
  id: string;
  account: string;
  displayName: string;
  role: string;
}

export interface Resource {
  resourceId: string;
  name: string;
  location: string;
  resourceType: string;
  unitLabel: string;
  enabled: boolean;
}

export interface Interval {
  startTime: string;
  endTime: string;
}

export interface Block extends Interval {
  blockType: string;
  reason: string;
}

export interface ReservationSlot extends Interval {
  reservationId: string;
  status: string;
}

export interface Availability {
  resource: Resource;
  date: string;
  openings: Interval[];
  blocks: Block[];
  reservations: ReservationSlot[];
  availablePeriods: Interval[];
}

export type ReservationStatus =
  | "RESERVED"
  | "CHECKED_IN"
  | "COMPLETED"
  | "CANCELLED";

export interface Reservation {
  reservationId: string;
  resourceId: string;
  resourceName: string;
  resourceType: string;
  location: string;
  unitLabel: string;
  startTime: string;
  endTime: string;
  status: ReservationStatus;
  acceptedAt: string;
  arrivalDeadline: string | null;
  checkInAt: string | null;
  checkOutAt: string | null;
  cancelReason: string | null;
  createdAt: string;
}

export interface CreateReservationPayload {
  resourceId: string;
  startTime: string;
  endTime: string;
  clientRequestId: string;
}

// XIA-2 管理员新增/修改资源的请求体，对应 docs/api/admin-resources.yaml。
export interface ResourcePayload {
  name: string;
  location: string;
  resourceType: string;
  unitLabel: string;
  enabled: boolean;
}

export type ScheduleType = "OPEN" | "TEACHING" | "EXTERNAL" | "DISABLED";

export interface Schedule {
  scheduleId: string;
  resourceId: string;
  resourceName: string;
  type: ScheduleType;
  startTime: string;
  endTime: string;
  note: string;
}

export interface ScheduleCreatePayload {
  resourceId: string;
  type: ScheduleType;
  startTime: string;
  endTime: string;
  note?: string;
}

export interface ScheduleUpdatePayload {
  type: ScheduleType;
  startTime: string;
  endTime: string;
  note?: string;
}
