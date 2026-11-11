import type { Availability, Resource } from "../types/api";
import { queryString, request } from "./http";

export const resourcesApi = {
  list(type?: string, location?: string): Promise<Resource[]> {
    return request<Resource[]>(`/resources${queryString({ type, location })}`);
  },

  get(resourceId: string): Promise<Resource> {
    return request<Resource>(`/resources/${encodeURIComponent(resourceId)}`);
  },

  availability(resourceId: string, date: string): Promise<Availability> {
    return request<Availability>(
      `/resources/${encodeURIComponent(resourceId)}/availability${queryString({ date })}`,
    );
  },
};
