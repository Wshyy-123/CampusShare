import type { Availability, Resource } from '../types/api'
import { queryString, request } from './http'

export const resourcesApi = {
  list(type?: string, location?: string): Promise<Resource[]> {
    return request<Resource[]>(`/resources${queryString({ type, location })}`)
  },

  availability(resourceId: string, date: string): Promise<Availability> {
    return request<Availability>(`/resources/${resourceId}/availability${queryString({ date })}`)
  },
}
