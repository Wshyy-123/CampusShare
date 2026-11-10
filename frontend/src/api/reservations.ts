import type { CreateReservationPayload, PageData, Reservation } from '../types/api'
import { queryString, request } from './http'

export const reservationsApi = {
  create(payload: CreateReservationPayload): Promise<Reservation> {
    return request<Reservation>('/reservations', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  mine(params: { status?: string; page?: number; size?: number }): Promise<PageData<Reservation>> {
    return request<PageData<Reservation>>(`/reservations/mine${queryString(params)}`)
  },

  detail(reservationId: string): Promise<Reservation> {
    return request<Reservation>(`/reservations/${reservationId}`)
  },
}
