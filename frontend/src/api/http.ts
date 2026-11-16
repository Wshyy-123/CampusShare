import { request as transport, ApiError } from "./request";
export { ApiError as HttpError } from "./request";
let csrfToken: { token: string; headerName: string } | undefined;
let csrfPromise: Promise<{ token: string; headerName: string }> | undefined;
let unauthorizedHandler: (() => void) | undefined;
let csrfGeneration = 0;
export function setUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler;
}
export function clearCsrfToken() {
  csrfGeneration++;
  csrfToken = undefined;
  csrfPromise = undefined;
}
async function csrf() {
  if (csrfToken) return csrfToken;
  if (!csrfPromise) {
    const generation = csrfGeneration;
    csrfPromise = transport<{ token: string; headerName: string }>("/auth/csrf")
      .then((value) => {
        if (
          !value ||
          typeof value.token !== "string" ||
          !value.token ||
          typeof value.headerName !== "string" ||
          !/^[A-Za-z0-9-]+$/.test(value.headerName)
        )
          throw new ApiError("BAD_RESPONSE", "CSRF响应格式错误");
        if (generation !== csrfGeneration)
          throw new ApiError("AUTH_CHANGED", "登录状态已变化，请重试");
        csrfToken = value;
        return value;
      })
      .catch((error) => {
        if (generation === csrfGeneration) csrfPromise = undefined;
        throw error;
      });
  }
  return csrfPromise;
}
export async function request<T>(
  path: string,
  options: RequestInit = {},
  retried = false,
): Promise<T> {
  const requestGeneration = csrfGeneration;
  const method = (options.method ?? "GET").toUpperCase();
  if (!["GET", "POST", "PUT", "PATCH", "DELETE"].includes(method))
    throw new ApiError("INVALID_METHOD", "不支持的请求方法");
  const token = method === "GET" ? undefined : await csrf();
  try {
    return await transport<T>(path, {
      method: method as "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
      body:
        typeof options.body === "string" ? JSON.parse(options.body) : undefined,
      csrf: token,
      signal: options.signal ?? undefined,
    });
  } catch (error) {
    if (
      error instanceof ApiError &&
      error.status === 403 &&
      error.code === "CSRF_INVALID" &&
      !retried
    ) {
      clearCsrfToken();
      return request<T>(path, options, true);
    }
    if (error instanceof ApiError && error.status === 401) {
      if (requestGeneration !== csrfGeneration)
        throw new ApiError("AUTH_CHANGED", "登录状态已变化");
      clearCsrfToken();
      unauthorizedHandler?.();
    }
    throw error;
  }
}
export function queryString(
  params: Record<string, string | number | undefined>,
) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params))
    if (value !== undefined && value !== "") search.set(key, String(value));
  return search.size ? `?${search}` : "";
}
export function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "操作失败，请稍后重试";
}
