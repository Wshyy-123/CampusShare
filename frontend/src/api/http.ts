import type { ApiResponse } from '../types/api'

const API_BASE = '/api/v1'

let csrfToken: string | null = null
let csrfHeaderName = 'X-XSRF-TOKEN'
let unauthorizedHandler: (() => void) | null = null

export class HttpError extends Error {
  readonly status: number
  readonly code?: string
  readonly requestId?: string

  constructor(message: string, status: number, code?: string, requestId?: string) {
    super(message)
    this.name = 'HttpError'
    this.status = status
    this.code = code
    this.requestId = requestId
  }
}

export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler
}

async function readJson<T>(response: Response): Promise<ApiResponse<T> | null> {
  try {
    return (await response.json()) as ApiResponse<T>
  } catch {
    return null
  }
}

async function loadCsrfToken(): Promise<void> {
  if (csrfToken) {
    return
  }
  const response = await fetch(`${API_BASE}/auth/csrf`, { credentials: 'include' })
  const payload = await readJson<{ token: string; headerName: string }>(response)
  if (!response.ok || !payload) {
    throw new HttpError('无法获取 CSRF 令牌，请刷新页面重试', response.status, payload?.code, payload?.requestId)
  }
  csrfToken = payload.data.token
  csrfHeaderName = payload.data.headerName
}

export function clearCsrfToken(): void {
  csrfToken = null
}

export async function request<T>(path: string, options: RequestInit = {}, retried = false): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const needsCsrf = method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS'
  if (needsCsrf) {
    await loadCsrfToken()
  }

  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (needsCsrf && csrfToken) {
    headers.set(csrfHeaderName, csrfToken)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
      credentials: 'include',
    })
  } catch {
    throw new HttpError('网络连接失败，请检查后端是否启动', 0)
  }

  const payload = await readJson<T>(response)

  if (!response.ok) {
    if (response.status === 403 && payload?.code === 'CSRF_INVALID' && !retried) {
      clearCsrfToken()
      return request<T>(path, options, true)
    }
    if (response.status === 401) {
      unauthorizedHandler?.()
    }
    throw new HttpError(
      payload?.message ?? `请求失败（HTTP ${response.status}）`,
      response.status,
      payload?.code,
      payload?.requestId,
    )
  }

  if (!payload) {
    throw new HttpError('服务端返回了无法解析的数据', response.status)
  }
  return payload.data
}

export function queryString(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(key, String(value))
    }
  }
  const text = search.toString()
  return text ? `?${text}` : ''
}

export function errorMessage(error: unknown): string {
  if (error instanceof HttpError) {
    return error.requestId ? `${error.message}（追踪号 ${error.requestId}）` : error.message
  }
  return '操作失败，请稍后重试'
}
