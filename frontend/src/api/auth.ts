import type { CurrentUser } from '../types/api'
import { clearCsrfToken, request } from './http'

export const authApi = {
  me(): Promise<CurrentUser> {
    return request<CurrentUser>('/auth/me')
  },

  login(account: string, password: string): Promise<CurrentUser> {
    return request<CurrentUser>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ account, password }),
    })
  },

  async logout(): Promise<void> {
    await request<void>('/auth/logout', { method: 'POST' })
    clearCsrfToken()
  },
}
