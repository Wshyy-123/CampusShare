// 与后端 com.campusshare.*.dto 对齐的类型定义
// 时间统一使用 OffsetDateTime 字符串（如 2026-11-04T10:00:00+08:00）

// ---------- 通用 ----------
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export interface Page<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// ---------- XIA-3 时段管理 ----------
export type SchedulePeriodType = 'OPEN' | 'TEACHING'

export interface SchedulePeriod {
  id: number
  unitId: number
  type: SchedulePeriodType
  startAt: string // OffsetDateTime ISO-8601
  endAt: string
  note?: string | null
}

export interface ScheduleCreateRequest {
  unitIds: number[]
  type: SchedulePeriodType
  startAt: string
  endAt: string
  note?: string
}

export interface ScheduleUpdateRequest {
  type: SchedulePeriodType
  startAt: string
  endAt: string
  note?: string
}

// ---------- XIA-4 资源与可用时段 ----------
export type ResourceType =
  | 'LIBRARY_SEAT'
  | 'CLASSROOM'
  | 'BADMINTON'
  | 'BASKETBALL'
  | 'TENNIS'
  | 'STUDY_ROOM'
  | string

export type ResourceStatus = 'ENABLED' | 'DISABLED'

export interface ResourceListItem {
  id: number
  name: string
  location?: string
  type: ResourceType
  status: ResourceStatus
  unitCount: number
}

export interface ResourceUnitSummary {
  id: number
  name: string
  unitCode?: string
  status: ResourceStatus
}

export interface ResourceDetail {
  id: number
  name: string
  location?: string
  type: ResourceType
  status: ResourceStatus
  units: ResourceUnitSummary[]
}

export interface TimeRange {
  startAt: string
  endAt: string
}

export interface UnitAvailability {
  resourceId: number
  resourceName: string
  resourceType: ResourceType
  resourceStatus: ResourceStatus
  unitId: number
  unitName: string
  unitCode?: string
  unitStatus: ResourceStatus
  date: string // LocalDate YYYY-MM-DD
  openPeriods: TimeRange[]
  teachingPeriods: TimeRange[]
  reservationPeriods: TimeRange[]
  blockedPeriods: TimeRange[]
  availablePeriods: TimeRange[]
  hasAvailablePeriod: boolean
  message: string
}

export interface ResourceAvailability {
  resourceId: number
  resourceName: string
  resourceType: ResourceType
  resourceStatus: ResourceStatus
  date: string
  units: UnitAvailability[]
}
