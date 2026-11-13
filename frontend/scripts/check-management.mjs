import assert from "node:assert/strict";
import { createServer } from "vite";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
const server = await createServer({
  root: resolve(dirname(fileURLToPath(import.meta.url)), ".."),
  server: { middlewareMode: true },
  appType: "custom",
});
const originalFetch = globalThis.fetch;
try {
  const { createApiCampusService } = await server.ssrLoadModule(
    "/src/api/campusApi.ts",
  );
  const resourceId = "9007199254740993";
  const resource = {
    resourceId,
    name: "接口教室",
    location: "教学楼",
    resourceType: "CLASSROOM",
    unitLabel: "301",
    enabled: true,
  };
  const day = "2030-01-02";
  const range = {
    startTime: `${day}T09:00:00+08:00`,
    endTime: `${day}T10:00:00+08:00`,
  };
  const row = {
    scheduleId: "1",
    resourceId,
    resourceName: resource.name,
    type: "TEACHING",
    ...range,
    note: "课程",
  };
  let blocked = false,
    deletedType,
    release,
    started;
  let deferred = false;
  const ok = (data) =>
    new Response(JSON.stringify({ code: "OK", data }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  globalThis.fetch = async (url, options = {}) => {
    const target = new URL(String(url), "http://localhost");
    const path = target.pathname;
    if (path.endsWith("/auth/csrf"))
      return ok({ token: "token", headerName: "X-CSRF-TOKEN" });
    if (options.method && options.method !== "GET")
      assert.equal(options.headers["X-CSRF-TOKEN"], "token");
    if (path.endsWith("/teaching-blocks")) {
      blocked = true;
      return ok(row);
    }
    if (path.endsWith("/schedules/1") && options.method === "PUT")
      return ok({ ...row, note: "更新" });
    if (path.endsWith("/schedules/1") && options.method === "DELETE") {
      deletedType = target.searchParams.get("type");
      blocked = false;
      return ok(null);
    }
    if (path.endsWith("/admin/schedules")) {
      const data = [
        { ...row, type: "OPEN" },
        row,
        { ...row, scheduleId: "2", type: "DISABLED" },
      ];
      if (deferred) {
        deferred = false;
        await new Promise((resolve) => {
          release = resolve;
          started();
        });
      }
      return ok(data);
    }
    if (path === "/api/v1/resources") return ok([resource]);
    if (path.endsWith("/mine"))
      return ok({ page: 1, size: 10, total: 0, items: [] });
    if (path.endsWith("/availability"))
      return ok({
        resource,
        date: target.searchParams.get("date"),
        openings: [range],
        blocks: blocked
          ? [{ ...range, blockType: "TEACHING", reason: "课程" }]
          : [],
        reservations: [],
        availablePeriods: blocked ? [] : [range],
      });
    throw new Error(`Unexpected path ${path}`);
  };
  const service = createApiCampusService();
  await service.load();
  await service.loadAvailability(resourceId, day);
  assert.equal(service.slots(resourceId, day)[0].available, true);
  await service.createSchedule({
    resourceId,
    type: "TEACHING",
    ...range,
    note: "课程",
  });
  assert.equal(service.slots(resourceId, day)[0].available, false);
  await service.loadSchedules(day, resourceId);
  assert.equal(service.state.schedules.length, 3);
  assert.deepEqual(
    service.state.schedules.map((row) => row.type),
    ["OPEN", "TEACHING", "DISABLED"],
  );
  await service.updateSchedule("1", {
    type: "TEACHING",
    ...range,
    note: "更新",
  });
  await service.deleteSchedule("1", "TEACHING");
  assert.equal(deletedType, "TEACHING");
  assert.equal(service.slots(resourceId, day)[0].available, true);
  deferred = true;
  const signal = new Promise((resolve) => {
    started = resolve;
  });
  const stale = service.loadSchedules(day, resourceId);
  await signal;
  service.reset();
  release();
  await assert.rejects(stale, (error) => error.code === "AUTH_CHANGED");
  assert.deepEqual(service.state.schedules, []);
  console.log(
    "PASS: typed schedule deletion, same-ID read models, mutation availability refresh and stale account response isolation",
  );
} finally {
  globalThis.fetch = originalFetch;
  await server.close();
}
