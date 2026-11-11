import type {
  Schedule,
  ScheduleCreatePayload,
  ScheduleUpdatePayload,
} from "../types/api";
import { queryString, request } from "./http";

export const schedulesApi = {
  list(params: { resourceId?: string; date?: string }): Promise<Schedule[]> {
    return request<Schedule[]>(`/admin/schedules${queryString(params)}`);
  },

  create(payload: ScheduleCreatePayload): Promise<Schedule> {
    const resourceId = encodeURIComponent(payload.resourceId);
    const body =
      payload.type === "OPEN"
        ? { startTime: payload.startTime, endTime: payload.endTime, note: payload.note }
        : { startTime: payload.startTime, endTime: payload.endTime, reason: payload.note };
    const path =
      payload.type === "OPEN"
        ? `/admin/resources/${resourceId}/openings`
        : `/admin/resources/${resourceId}/teaching-blocks`;
    return request<Schedule>(path, { method: "POST", body: JSON.stringify(body) });
  },

  update(scheduleId: string, payload: ScheduleUpdatePayload): Promise<Schedule> {
    return request<Schedule>(
      `/admin/schedules/${encodeURIComponent(scheduleId)}`,
      { method: "PUT", body: JSON.stringify(payload) },
    );
  },

  remove(scheduleId: string): Promise<void> {
    return request<void>(`/admin/schedules/${encodeURIComponent(scheduleId)}`, {
      method: "DELETE",
    });
  },
};
