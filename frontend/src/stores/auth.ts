import { reactive } from "vue";
import { authApi } from "../api/auth";
import type { CurrentUser } from "../types/api";

export const authState = reactive({
  user: null as CurrentUser | null,
  loading: true,
  initialized: false,
  error: "",
});

let restorePromise: Promise<void> | null = null;

export function restoreAuth(): Promise<void> {
  if (authState.initialized) {
    return Promise.resolve();
  }
  if (!restorePromise) {
    restorePromise = authApi
      .me()
      .then((user) => {
        authState.user = user;
        authState.error = "";
      })
      .catch((error: unknown) => {
        authState.user = null;
        authState.error =
          error instanceof Error ? error.message : "无法恢复登录状态";
      })
      .finally(() => {
        authState.loading = false;
        authState.initialized = true;
      });
  }
  return restorePromise;
}

export async function login(
  account: string,
  password: string,
): Promise<CurrentUser> {
  const user = await authApi.login(account, password);
  authState.user = user;
  authState.error = "";
  authState.loading = false;
  authState.initialized = true;
  return user;
}

export async function logout(): Promise<void> {
  await authApi.logout();
  clearAuth();
}

export function clearAuth(): void {
  authState.user = null;
  authState.loading = false;
  authState.initialized = true;
}

export function roleLabel(role: string): string {
  if (role === "STUDENT") return "学生";
  if (role === "TEACHER") return "教师";
  if (role === "RESOURCE_ADMIN") return "资源管理员";
  return role;
}
