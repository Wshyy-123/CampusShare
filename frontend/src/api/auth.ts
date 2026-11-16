import type { CurrentUser } from "../types/api";
import { clearCsrfToken, request } from "./http";
import { ApiError } from "./request";

function currentUser(value: CurrentUser): CurrentUser {
  if (
    !value ||
    typeof value.id !== "string" ||
    !/^[1-9][0-9]*$/.test(value.id) ||
    typeof value.account !== "string" ||
    typeof value.displayName !== "string" ||
    !["STUDENT", "TEACHER", "RESOURCE_ADMIN"].includes(value.role)
  )
    throw new ApiError("BAD_RESPONSE", "用户身份响应格式错误");
  return value;
}

export const authApi = {
  async me(): Promise<CurrentUser> {
    return currentUser(await request<CurrentUser>("/auth/me"));
  },

  async login(account: string, password: string): Promise<CurrentUser> {
    const user = await request<CurrentUser>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ account, password }),
    });
    clearCsrfToken();
    return currentUser(user);
  },

  async logout(): Promise<void> {
    await request<void>("/auth/logout", { method: "POST" });
    clearCsrfToken();
  },
};
