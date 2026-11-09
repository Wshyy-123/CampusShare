// XIA-4 资源查询与可用时段接口
import { request } from './request'
import type {
  Page,
  ResourceListItem,
  ResourceDetail,
  ResourceAvailability,
  UnitAvailability,
  ResourceType
} from '../types'

export function searchResources(params: {
  keyword?: string
  type?: ResourceType | ''
  page?: number
  size?: number
}) {
  const qs = new URLSearchParams()
  if (params.keyword) qs.set('keyword', params.keyword)
  if (params.type) qs.set('type', params.type)
  qs.set('page', String(params.page ?? 1))
  qs.set('size', String(params.size ?? 20))
  return request<Page<ResourceListItem>>(`/resources?${qs.toString()}`)
}

export function getResourceDetail(resourceId: number) {
  return request<ResourceDetail>(`/resources/${resourceId}`)
}

export function getResourceAvailability(resourceId: number, date: string) {
  return request<ResourceAvailability>(
    `/resources/${resourceId}/availability?date=${date}`
  )
}

export function getUnitAvailability(unitId: number, date: string) {
  return request<UnitAvailability>(
    `/resources/units/${unitId}/availability?date=${date}`
  )
}
