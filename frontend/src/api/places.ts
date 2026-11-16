import type { PlaceAttendance } from "../types/api";
import { request, queryString } from "./http";
export const placesApi = {
  get(resourceId: string, reservationId?: string) {
    return request<PlaceAttendance>(
      `/places/${encodeURIComponent(resourceId)}${queryString({ reservationId })}`,
    );
  },
};
