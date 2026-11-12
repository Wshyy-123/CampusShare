import assert from "node:assert/strict";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { createServer } from "vite";
import { createSSRApp, h } from "vue";
import { renderToString } from "@vue/server-renderer";

const server = await createServer({
  root: resolve(dirname(fileURLToPath(import.meta.url)), ".."),
  envDir: false, mode: "test", appType: "custom",
  server: { middlewareMode: true },
  define: { "import.meta.env.VITE_DATA_MODE": JSON.stringify("api") },
});
const originalFetch = globalThis.fetch;
try {
  const { createApiCampusService } = await server.ssrLoadModule("/src/api/campusApi.ts");
  const { dateAfter } = await server.ssrLoadModule("/src/utils/presentation.ts");
  const http = await server.ssrLoadModule("/src/api/http.ts");
  const resourceId = "9007199254740993";
  const reservationId = "9007199254741991";
  const date = dateAfter(1);
  const range = (from, to) => ({ startTime: `${date}T${from}:00:00+08:00`, endTime: `${date}T${to}:00:00+08:00` });
  const period = range("12", "13");
  const resource = { resourceId, resourceType: "LIBRARY_SEAT", name: "接口座位", location: "图书馆", unitLabel: "S1", enabled: true };
  let disabled = false, rejection = 0, refreshFails = false, writes = 0;
  let staleRelease, staleStarted;
  const notifications = [];
  const ok = data => new Response(JSON.stringify({ code: "OK", data }));
  const fail = (status, code) => new Response(JSON.stringify({ code, message: "停用请求失败" }), { status });
  const availability = () => ({ resource, date, openings: [range("08", "22")],
    blocks: disabled ? [{ ...period, blockType: "DISABLED", reason: "检修" }] : [], reservations: [],
    availablePeriods: disabled ? [range("08", "12"), range("13", "22")] : [range("08", "22")] });
  globalThis.fetch = async (url, options = {}) => {
    assert.equal(options.credentials, "same-origin");
    const path = new URL(String(url), "http://localhost").pathname;
    if (path.endsWith("/auth/csrf")) return ok({ token: "lifecycle-csrf", headerName: "X-XSRF-TOKEN" });
    if (path.endsWith("/disable-periods")) {
      writes++;
      assert.equal(path, `/api/v1/admin/resources/${resourceId}/disable-periods`);
      assert.equal(options.method, "POST");
      assert.equal(options.headers["X-XSRF-TOKEN"], "lifecycle-csrf");
      assert.deepEqual(JSON.parse(options.body), { ...period, reason: "检修" });
      if (rejection) return fail(rejection, rejection === 409 ? "INVALID_STATE" : "FORBIDDEN");
      disabled = true;
      return ok({ resourceId, cancelledReservationIds: [reservationId] });
    }
    if (refreshFails) return fail(500, "INTERNAL_ERROR");
    if (path === "/api/v1/resources") return ok([resource]);
    if (path.includes("/availability")) {
      const requestedDate = new URL(String(url), "http://localhost").searchParams.get("date");
      const snapshot = JSON.parse(JSON.stringify(availability()).replaceAll(date, requestedDate));
      if (staleStarted) {
        const started = staleStarted;
        staleStarted = undefined;
        await new Promise(resolve => { staleRelease = resolve; started(); });
      }
      return ok(snapshot);
    }
    if (path.endsWith("/mine")) return ok({ page: 1, size: 10, total: 1, items: [{
      reservationId, resourceId, ...period, status: disabled ? "CANCELLED" : "RESERVED",
      arrivalDeadline: null, cancelReason: disabled ? "RESOURCE_DISABLED" : null,
    }] });
    throw new Error(`Unexpected request ${path}`);
  };
  const service = createApiCampusService(message => notifications.push(message));
  await service.load();
  await service.loadAvailability(resourceId, date);
  assert.equal(service.slots(resourceId, date).find(s => s.start === 12).available, true);
  for (const status of [403, 409, 500]) {
    rejection = status;
    await assert.rejects(service.disableResource(resourceId, period.startTime, period.endTime, "检修"), e => e.status === status);
    assert.equal(notifications.length, 0);
    assert.equal(service.state.bookings[0].state, "RESERVED");
  }
  const previousWrites = writes;
  await assert.rejects(service.disableResource(resourceId, period.endTime, period.startTime, "检修"));
  await assert.rejects(service.disableResource(resourceId, period.startTime, period.endTime, " "));
  assert.equal(writes, previousWrites);
  rejection = 0;
  // A query sent before the mutation must not leave the pre-disable snapshot cached.
  const started = new Promise(resolve => { staleStarted = resolve; });
  const stale = service.loadAvailability(resourceId, date);
  await started;
  const mutation = service.disableResource(resourceId, period.startTime, period.endTime, "  检修  ");
  staleRelease();
  await stale;
  const result = await mutation;
  assert.equal(result.cancelledReservationIds[0], reservationId);
  assert.equal(service.state.bookings[0].cancelReason, "RESOURCE_DISABLED");
  assert.equal(service.slots(resourceId, date).find(s => s.start === 12).available, false);
  assert.ok(service.blocks(resourceId, date).some(b => b.label === "停用维护"));
  assert.equal(notifications.length, 1);
  const Badge = (await server.ssrLoadModule("/src/components/StatusBadge.vue")).default;
  for (const [cancelReason, label] of [["RESOURCE_DISABLED", "资源停用"], ["NO_SHOW", "未到超时"]]) {
    assert.ok((await renderToString(createSSRApp({ render: () => h(Badge, { status: "CANCELLED", cancelReason }) }))).includes(label));
  }
  refreshFails = true;
  await service.disableResource(resourceId, period.startTime, period.endTime, "检修");
  assert.match(service.state.error, /停用已成功.*刷新失败/);
  assert.equal(service.slots(resourceId, date).length, 0);
  refreshFails = false;
  http.setUnauthorizedHandler(() => service.reset());
  rejection = 401;
  await assert.rejects(service.disableResource(resourceId, period.startTime, period.endTime, "检修"));
  assert.equal(service.state.ready, false);
  assert.equal(service.state.bookings.length, 0);
  console.log("PASS: disable POST, shared CSRF, opaque IDs, errors, stale-query refresh, cancellation labels and auth clearing");
} finally {
  globalThis.fetch = originalFetch;
  await server.close();
}
console.log("Lifecycle frontend checks passed with mocked HTTP; real server/browser checks are separate.");
