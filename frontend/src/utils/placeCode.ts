export function placeCodeUrl(resourceId: string, origin: string): string {
  if (!/^[1-9][0-9]*$/.test(resourceId)) throw new Error("资源编号无效");
  const base = new URL(origin);
  if (
    !["http:", "https:"].includes(base.protocol) ||
    base.username ||
    base.password ||
    base.search ||
    base.hash
  )
    throw new Error("站点地址无效");
  if (!base.pathname.endsWith("/")) base.pathname += "/";
  return new URL(`places/${resourceId}`, base).href;
}
