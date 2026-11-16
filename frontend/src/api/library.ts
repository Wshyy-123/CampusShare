import type { CardSwipePayload, Reservation } from "../types/api";
import { request } from "./http";

export const libraryApi = {
  enter(reservationId: string, swipe: CardSwipePayload): Promise<Reservation> {
    return request(
      `/library/reservations/${encodeURIComponent(reservationId)}/check-in`,
      {
        method: "POST",
        body: JSON.stringify(swipe),
      },
    );
  },
  leave(reservationId: string, swipe: CardSwipePayload): Promise<Reservation> {
    return request(
      `/library/reservations/${encodeURIComponent(reservationId)}/check-out`,
      {
        method: "POST",
        body: JSON.stringify(swipe),
      },
    );
  },
};
