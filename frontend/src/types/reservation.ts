export type ReservationStatus = 'RESERVED' | 'CHECKED_IN' | 'COMPLETED' | 'CANCELLED'

/** 与 docs/api/sports-attendance.yaml 的 ReservationDetail 一致 */
export interface ReservationDetail {
  reservationId: string
  resourceId: string
  resourceType: string | null
  status: ReservationStatus
  cancelReason: string | null
  startTime: string
  endTime: string
  checkInAt: string | null
  checkOutAt: string | null
}

export interface ApiEnvelope<T> {
  code: string
  message: string
  data: T | null
  requestId: string
}
