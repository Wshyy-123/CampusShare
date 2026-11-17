import assert from "node:assert/strict";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { createServer } from "vite";
import { createSSRApp, h } from "vue";
import { renderToString } from "@vue/server-renderer";
const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const server = await createServer({
  root,
  mode: "test",
  envDir: false,
  appType: "custom",
  server: { middlewareMode: true },
  define: { "import.meta.env.VITE_DATA_MODE": JSON.stringify("api") },
});
const originalFetch = globalThis.fetch;
const originalNow = Date.now;
try {
  const seatId = "9007199254740993",
    roomId = "9007199254740994";
  const seatBooking = "9007199254741001",
    roomBooking = "9007199254741002";
  const resources = [
    {
      resourceId: seatId,
      name: "图书馆座位",
      location: "测试馆",
      resourceType: "LIBRARY_SEAT",
      unitLabel: "A-01",
      enabled: true,
    },
    {
      resourceId: roomId,
      name: "教室",
      location: "测试楼",
      resourceType: "CLASSROOM",
      unitLabel: "整间",
      enabled: true,
    },
  ];
  const statuses = new Map([
    [seatBooking, "RESERVED"],
    [roomBooking, "RESERVED"],
  ]);
  const date = "2030-01-02",
    startTime = `${date}T10:00:00+08:00`,
    endTime = `${date}T11:00:00+08:00`;
  Date.now = () => Date.parse(`${date}T08:00:00+08:00`);
  const booking = (id) => {
    const r = resources[id === seatBooking ? 0 : 1],
      status = statuses.get(id);
    return {
      reservationId: id,
      ...r,
      resourceName: r.name,
      status,
      startTime,
      endTime,
      acceptedAt: `${date}T09:55:00+08:00`,
      arrivalDeadline: `${date}T10:25:00+08:00`,
      createdAt: startTime,
      checkInAt:
        status === "CHECKED_IN" || status === "COMPLETED" ? startTime : null,
      checkOutAt: status === "COMPLETED" ? `${date}T10:10:00+08:00` : null,
      cancelReason: status === "CANCELLED" ? "USER_CANCEL" : null,
    };
  };
  const ok = (data) =>
    new Response(JSON.stringify({ code: "OK", data }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  let writes = 0,
    hold,
    deferred;
  globalThis.fetch = async (url, options = {}) => {
    const u = new URL(String(url), "http://localhost"),
      path = u.pathname;
    if (path.endsWith("/auth/csrf"))
      return ok({ token: "library-csrf", headerName: "X-CSRF-TOKEN" });
    if (path.endsWith("/auth/login") || path.endsWith("/auth/me"))
      return ok({
        id: "1",
        account: "student01",
        displayName: "学生",
        role: "STUDENT",
      });
    if (options.method === "POST" && path.includes("/reservations/")) {
      writes++;
      assert.equal(options.headers["X-CSRF-TOKEN"], "library-csrf");
      const id = path.split("/").at(-2);
      if (path.endsWith("/cancel")) {
        assert.equal(id, roomBooking);
        statuses.set(id, "CANCELLED");
        return ok(booking(id));
      }
      assert.ok(path.startsWith("/api/v1/library/reservations/"));
      assert.deepEqual(JSON.parse(options.body), {
        resourceId: seatId,
        source: "CARD_SIMULATION",
      });
      assert.equal(id, seatBooking);
      if (hold) return deferred;
      statuses.set(id, path.endsWith("/check-in") ? "CHECKED_IN" : "COMPLETED");
      return ok(booking(id));
    }
    if (path === "/api/v1/resources") return ok(resources);
    if (path.endsWith("/mine"))
      return ok({
        page: 1,
        size: 10,
        total: 2,
        items: [booking(seatBooking), booking(roomBooking)],
      });
    if (path.endsWith("/availability")) {
      const resource = resources.find((r) => path.includes(r.resourceId)),
        id = resource.resourceId === seatId ? seatBooking : roomBooking;
      const date = u.searchParams.get("date");
      const range = {
        startTime: `${date}T10:00:00+08:00`,
        endTime: `${date}T11:00:00+08:00`,
      };
      return ok({
        resource,
        date,
        openings: [range],
        blocks: [],
        reservations: [],
        availablePeriods: ["COMPLETED", "CANCELLED"].includes(statuses.get(id))
          ? [range]
          : [],
      });
    }
    throw new Error(`Unexpected request ${path}`);
  };
  const auth = await server.ssrLoadModule("/src/stores/auth.ts");
  await auth.login("student01", "test");
  const { createApiCampusService } = await server.ssrLoadModule(
    "/src/api/campusApi.ts",
  );
  const service = createApiCampusService(() => {});
  await service.load();
  await service.loadAvailability(seatId, date);
  await service.loadAvailability(roomId, date);
  assert.ok(service.slots(seatId, date).every((s) => !s.available));
  const scan = { resourceId: seatId, source: "CARD_SIMULATION" };
  assert.equal(
    (await service.enterLibrary(seatBooking, scan)).status,
    "CHECKED_IN",
  );
  assert.equal(
    service.state.bookings.find((b) => b.id === seatBooking).state,
    "CHECKED_IN",
  );
  assert.equal(
    (await service.leaveLibrary(seatBooking, scan)).status,
    "COMPLETED",
  );
  assert.ok(service.slots(seatId, date).some((s) => s.available));
  assert.equal(
    (await service.cancelReservation(roomBooking)).cancelReason,
    "USER_CANCEL",
  );
  assert.ok(service.slots(roomId, date).some((s) => s.available));
  const { default: Panel } = await server.ssrLoadModule(
    "/src/components/LibraryCardPanel.vue",
  );
  statuses.set(seatBooking, "RESERVED");
  let html = await renderToString(
    createSSRApp({
      render: () => h(Panel, { reservation: booking(seatBooking) }),
    }),
  );
  assert.match(html, /模拟刷卡入场/);
  assert.doesNotMatch(html, />.*取消预约.*<\/button>/);
  statuses.set(seatBooking, "CHECKED_IN");
  html = await renderToString(
    createSSRApp({
      render: () => h(Panel, { reservation: booking(seatBooking) }),
    }),
  );
  assert.match(html, /模拟刷卡离场并释放/);
  assert.doesNotMatch(html, />模拟刷卡入场</);
  const { createDemoCampusService } = await server.ssrLoadModule(
    "/src/services/demoCampusService.ts",
  );
  const demo = createDemoCampusService(() => {});
  const before = writes;
  await assert.rejects(demo.enterLibrary(seatBooking, scan));
  await assert.rejects(demo.leaveLibrary(seatBooking, scan));
  assert.equal(writes, before);
  deferred = new Promise((resolve) => {
    hold = resolve;
  });
  const pending = service.enterLibrary(seatBooking, scan);
  await new Promise((resolve) => setTimeout(resolve, 10));
  service.reset();
  hold(ok(booking(seatBooking)));
  await assert.rejects(pending, (e) => e.code === "AUTH_CHANGED");
  assert.equal(service.state.bookings.length, 0);
  console.log(
    "PASS: library Session/CSRF contract, opaque IDs, explicit entry/exit controls, seat/classroom availability refresh, demo rejection and stale account isolation",
  );
} finally {
  Date.now = originalNow;
  globalThis.fetch = originalFetch;
  await server.close();
}
