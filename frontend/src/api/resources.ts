import type {
  Availability,
  PageData,
  Resource,
  ResourcePayload,
} from "../types/api";
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

// XIA-2 管理端资源读写；服务端按 RESOURCE_ADMIN 角色独立校验。
export const adminResourcesApi = {
  list(query: {
    page: number;
    size: number;
    keyword?: string;
    type?: string;
  }): Promise<PageData<Resource>> {
    return request<PageData<Resource>>(
      `/admin/resources${queryString(query)}`,
    );
  },

  create(payload: ResourcePayload): Promise<Resource> {
    return request<Resource>("/admin/resources", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  update(resourceId: string, payload: ResourcePayload): Promise<Resource> {
    return request<Resource>(
      `/admin/resources/${encodeURIComponent(resourceId)}`,
      { method: "PUT", body: JSON.stringify(payload) },
    );
  },
};
