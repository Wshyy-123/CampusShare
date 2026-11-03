import type { ApiEnvelope } from '../types/reservation'

const DEBUG_USER_STORAGE_KEY = 'campusshare.debugUserId'
const DEBUG_USER_HEADER = 'X-Debug-User-Id'

export class ApiError extends Error {
  readonly httpStatus: number
  readonly code: string
  readonly requestId: string

  constructor(httpStatus: number, code: string, message: string, requestId: string) {
    super(message)
    this.name = 'ApiError'
    this.httpStatus = httpStatus
    this.code = code
    this.requestId = requestId
  }
}

// 登录（XIA-1）落地前的临时身份，仅在开发环境由后端接受；见 docs/api/sports-attendance.yaml
export function getDebugUserId(): string {
  return window.localStorage.getItem(DEBUG_USER_STORAGE_KEY) ?? '1'
}

export function setDebugUserId(userId: string): void {
  window.localStorage.setItem(DEBUG_USER_STORAGE_KEY, userId)
}

/**
 * 统一请求封装：解析 code/message/data/requestId，失败抛 ApiError，页面只处理业务提示。
 */
export async function request<T>(path: string, init: RequestInit = {}): Promise<ApiEnvelope<T>> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  headers.set(DEBUG_USER_HEADER, getDebugUserId())
  if (init.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', '网络请求失败，请确认后端已启动', '')
  }

  let envelope: ApiEnvelope<T> | null = null
  try {
    envelope = (await response.json()) as ApiEnvelope<T>
  } catch {
    envelope = null
  }

  if (!response.ok || envelope === null || envelope.code !== 'OK') {
    throw new ApiError(
      response.status,
      envelope?.code ?? `HTTP_${response.status}`,
      envelope?.message ?? `请求失败（HTTP ${response.status}）`,
      envelope?.requestId ?? '',
    )
  }
  return envelope
}
