export class ApiError extends Error {
  code: string;
  status: number;
  constructor(code: string, message: string, status = 0) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  body?: unknown;
  // 由认证接口返回的headerName/token提供；登录后刷新令牌，不能写死用户ID或Basic凭证。
  csrf?: { headerName: string; token: string };
  signal?: AbortSignal;
}

export async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  if (!path.startsWith("/") || path.startsWith("//"))
    throw new ApiError("INVALID_PATH", "接口路径必须是 /api/v1 下的相对路径");
  const method = options.method ?? "GET";
  const headers: Record<string, string> = { Accept: "application/json" };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (method !== "GET") {
    if (!options.csrf?.headerName || !options.csrf.token)
      throw new ApiError("CSRF_REQUIRED", "写操作前需要从认证接口取得CSRF令牌");
    headers[options.csrf.headerName] = options.csrf.token;
  }
  let response: Response;
  try {
    response = await fetch(`/api/v1${path}`, {
      method,
      headers,
      credentials: "same-origin",
      body:
        options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal ?? AbortSignal.timeout(15000),
    });
  } catch {
    throw new ApiError(
      "NETWORK_ERROR",
      "无法连接后端或请求已取消，请检查连接后重试",
    );
  }
  if (response.status === 204 && response.ok) return undefined as T;
  let result: unknown;
  try {
    result = await response.json();
  } catch {
    throw new ApiError(
      "BAD_RESPONSE",
      `接口未返回JSON（HTTP ${response.status}）`,
      response.status,
    );
  }
  if (!result || typeof result !== "object")
    throw new ApiError("BAD_RESPONSE", "接口响应格式错误", response.status);
  const payload = result as {
    code?: unknown;
    message?: unknown;
    data?: unknown;
  };
  if (!response.ok || payload.code !== "OK")
    throw new ApiError(
      typeof payload.code === "string" ? payload.code : "HTTP_ERROR",
      typeof payload.message === "string"
        ? payload.message
        : `请求失败（HTTP ${response.status}）`,
      response.status,
    );
  // DTO的结构校验和映射由campusApi逐方法实现，此处泛型不等于已验证服务端字段。
  return payload.data as T;
}
