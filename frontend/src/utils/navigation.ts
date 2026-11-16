export function safeReturnPath(value: unknown): string {
  if (
    typeof value !== "string" ||
    !value.startsWith("/") ||
    value.startsWith("//") ||
    /[\\\u0000-\u001f]/.test(value) ||
    /^\/login(?:$|[?#])/.test(value)
  )
    return "/resources";
  return value;
}
