import type {
  PlaceScanPayload,
  CreateReservationPayload,
  PageData,
  Reservation,
} from "../types/api";
import { queryString, request } from "./http";

export const reservationsApi = {
  create(payload: CreateReservationPayload): Promise<Reservation> {
    return request<Reservation>("/reservations", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  mine(params: {
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PageData<Reservation>> {
    return request<PageData<Reservation>>(
      `/reservations/mine${queryString(params)}`,
    );
  },

  detail(reservationId: string): Promise<Reservation> {
    return request<Reservation>(
      `/reservations/${encodeURIComponent(reservationId)}`,
    );
  },

  // XIA-7/13：体育或教室预约使用前取消，成功后返回最新预约详情
  cancel(reservationId: string): Promise<Reservation> {
    return request<Reservation>(
      `/reservations/${encodeURIComponent(reservationId)}/cancel`,
      { method: "POST" },
    );
  },

  // XIA-8：体育扫码签到
  checkIn(reservationId: string, scan: PlaceScanPayload): Promise<Reservation> {
    return request<Reservation>(
      `/reservations/${encodeURIComponent(reservationId)}/check-in`,
      { method: "POST", body: JSON.stringify(scan) },
    );
  },

  // XIA-8：体育扫码签退
  checkOut(
    reservationId: string,
    scan: PlaceScanPayload,
  ): Promise<Reservation> {
    return request<Reservation>(
      `/reservations/${encodeURIComponent(reservationId)}/check-out`,
      { method: "POST", body: JSON.stringify(scan) },
    );
  },
};
