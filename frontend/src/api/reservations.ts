import { request } from './http'
import type { ApiEnvelope, ReservationDetail } from '../types/reservation'

const BASE_PATH = '/api/v1/reservations'

export function getReservation(reservationId: string): Promise<ApiEnvelope<ReservationDetail>> {
  return request<ReservationDetail>(`${BASE_PATH}/${reservationId}`)
}

export function cancelReservation(reservationId: string): Promise<ApiEnvelope<ReservationDetail>> {
  return request<ReservationDetail>(`${BASE_PATH}/${reservationId}/cancel`, { method: 'POST' })
}

export function checkInReservation(reservationId: string): Promise<ApiEnvelope<ReservationDetail>> {
  return request<ReservationDetail>(`${BASE_PATH}/${reservationId}/check-in`, { method: 'POST' })
}

export function checkOutReservation(reservationId: string): Promise<ApiEnvelope<ReservationDetail>> {
  return request<ReservationDetail>(`${BASE_PATH}/${reservationId}/check-out`, { method: 'POST' })
}
