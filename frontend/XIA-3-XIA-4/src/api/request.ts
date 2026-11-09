// 统一 fetch 封装：自动拼接 /api/v1、解析 ApiResponse、处理错误
import type { ApiResponse } from '../types'

const BASE = '/api/v1'

export class ApiError extends Error {
  code: string
  status: number
  constructor(code: string, message: string, status: number) {
    super(message)
    this.code = code
    this.status = status
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  // 鉴权：Session Cookie 由浏览器自动携带；这里预留 basic auth 用于独立联调
  auth?: { username: string; password: string }
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth } = options

  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    headers['Authorization'] = 'Basic ' + btoa(`${auth.username}:${auth.password}`)
  }

  let resp: Response
  try {
    resp = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      credentials: 'same-origin'
    })
  } catch (e) {
    throw new ApiError('NETWORK_ERROR', '网络错误，请确认后端已启动', 0)
  }

  // 204 / 空响应
  if (resp.status === 204) return undefined as T

  let json: ApiResponse<T>
  try {
    json = (await resp.json()) as ApiResponse<T>
  } catch {
    throw new ApiError('BAD_RESPONSE', `后端返回非 JSON (HTTP ${resp.status})`, resp.status)
  }

  if (!resp.ok || json.code !== 'OK') {
    throw new ApiError(json.code || `HTTP_${resp.status}`, json.message || '请求失败', resp.status)
  }
  return json.data
}
