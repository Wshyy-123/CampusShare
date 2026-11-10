// XIA-3 管理员时段管理接口
import { request } from './request'
import type {
  SchedulePeriod,
  ScheduleCreateRequest,
  ScheduleUpdateRequest
} from '../types'

// 独立联调时用 admin/admin123；接入主后端后改为 Session Cookie
const ADMIN_AUTH = { username: 'admin', password: 'admin123' }

export function listSchedules(unitId: number, date: string) {
  return request<SchedulePeriod[]>(
    `/admin/schedules?unitId=${unitId}&date=${date}`,
    { auth: ADMIN_AUTH }
  )
}

export function createSchedules(req: ScheduleCreateRequest) {
  return request<SchedulePeriod[]>(`/admin/schedules`, {
    method: 'POST',
    body: req,
    auth: ADMIN_AUTH
  })
}

export function updateSchedule(id: number, req: ScheduleUpdateRequest) {
  return request<SchedulePeriod>(`/admin/schedules/${id}`, {
    method: 'PUT',
    body: req,
    auth: ADMIN_AUTH
  })
}

export function deleteSchedule(id: number) {
  return request<void>(`/admin/schedules/${id}`, {
    method: 'DELETE',
    auth: ADMIN_AUTH
  })
}
