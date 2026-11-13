import assert from "node:assert/strict";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { createServer } from "vite";
import { createSSRApp, h } from "vue";
import { renderToString } from "@vue/server-renderer";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const server = await createServer({
  root,
  envDir: false,
  mode: "test",
  appType: "custom",
  server: { middlewareMode: true },
  define: { "import.meta.env.VITE_DATA_MODE": JSON.stringify("api") },
  plugins: [
    {
      name: "api-check-router",
      resolveId(id) {
        if (id === "virtual:api-check-router") return "\0api-check-router";
      },
      load(id) {
        if (id === "\0api-check-router")
          return "export { createRouter, createMemoryHistory } from 'vue-router'";
      },
    },
  ],
});
const originalFetch = globalThis.fetch;
try {
  const { localDate, dateAfter } = await server.ssrLoadModule(
    "/src/utils/presentation.ts",
  );
  const { safeReturnPath } = await server.ssrLoadModule(
    "/src/utils/navigation.ts",
  );
  assert.equal(safeReturnPath("https://example.invalid"), "/resources");
  assert.equal(safeReturnPath("//example.invalid"), "/resources");
  assert.equal(safeReturnPath("/login?redirect=/login"), "/resources");
  assert.equal(safeReturnPath("/bookings/123"), "/bookings/123");
  const largeId = "9007199254740993";
  const resources = [
    {
      resourceId: "101",
      name: "接口座位",
      location: "接口图书馆",
      resourceType: "LIBRARY_SEAT",
      unitLabel: "S-01",
      enabled: true,
    },
    {
      resourceId: largeId,
      name: "接口羽毛球场",
      location: "接口体育馆",
      resourceType: "BADMINTON",
      unitLabel: "B-01",
      enabled: true,
    },
  ];
  const disabled = {
    resourceId: "777",
    name: "停用的历史教室",
    location: "接口教学楼",
    resourceType: "CLASSROOM",
    unitLabel: "R-01",
    enabled: false,
  };
  let rows = [];
  const adminRows = [resources[0], disabled];
  const requests = new Map();
  let csrf = 0;
  let dropNext = false;
  let rejectCsrf = false;
  let offlineLogout = false;
  let rejectAll = false;
  let deferredMine;
  let startedMine;
  const success = (data) =>
    new Response(JSON.stringify({ code: "OK", data }), {
      headers: { "Content-Type": "application/json" },
    });
  const booking = (id, resourceId, startTime, endTime) => ({
    reservationId: id,
    resourceId,
    resourceName: "接口返回预约",
    resourceType: "BADMINTON",
    location: "接口体育馆",
    unitLabel: "B-01",
    startTime,
    endTime,
    status: "RESERVED",
    acceptedAt: `${localDate()}T09:00:00+08:00`,
    arrivalDeadline: startTime,
    checkInAt: null,
    checkOutAt: null,
    cancelReason: null,
    createdAt: `${localDate()}T09:00:00+08:00`,
  });
  rows.push(
    booking(
      "9007199254741990",
      "777",
      `${dateAfter(1)}T18:00:00+08:00`,
      `${dateAfter(1)}T19:00:00+08:00`,
    ),
  );
  const postBodies = [];
  globalThis.fetch = async (url, options = {}) => {
    assert.equal(options.credentials, "same-origin");
    if (rejectAll)
      return new Response(
        JSON.stringify({ code: "AUTH_REQUIRED", message: "会话过期" }),
        { status: 401 },
      );
    const parsed = new URL(String(url), "http://localhost");
    const path = parsed.pathname;
    if (path === "/api/v1/auth/csrf")
      return success({ token: `csrf-${++csrf}`, headerName: "X-XSRF-TOKEN" });
    if (options.method !== "GET")
      assert.equal(options.headers["X-XSRF-TOKEN"], `csrf-${csrf}`);
    if (path === "/api/v1/auth/login" || path === "/api/v1/auth/me")
      return success({
        id: "1",
        account: "student01",
        displayName: "接口测试学生",
        role: "STUDENT",
      });
    if (path === "/api/v1/auth/logout") {
      if (offlineLogout) throw new Error("offline");
      return success(null);
    }
    if (path === "/api/v1/resources") return success(resources);
    if (path === "/api/v1/admin/resources") {
      if (options.method === "POST") {
        const body = JSON.parse(options.body);
        assert.equal(body.userId, undefined);
        const created = { resourceId: "901", ...body };
        adminRows.unshift(created);
        return success(created);
      }
      const page = Number(parsed.searchParams.get("page") || 1);
      const size = Number(parsed.searchParams.get("size") || 20);
      return success({
        items: adminRows.slice((page - 1) * size, page * size),
        total: adminRows.length,
        page,
        size,
      });
    }
    if (
      path.startsWith("/api/v1/admin/resources/") &&
      options.method === "PUT"
    ) {
      const body = JSON.parse(options.body);
      const resourceId = path.split("/").pop();
      const index = adminRows.findIndex((row) => row.resourceId === resourceId);
      adminRows[index] = { ...adminRows[index], ...body };
      return success(adminRows[index]);
    }
    if (path === "/api/v1/resources/777") return success(disabled);
    if (path.includes("/availability")) {
      const resourceId = path.split("/")[4];
      const resource =
        resourceId === "777"
          ? disabled
          : resources.find((r) => r.resourceId === resourceId);
      const date = parsed.searchParams.get("date");
      const range = (start, end) => ({
        startTime: `${date}T${start}:00+08:00`,
        endTime: `${date}T${end}:00+08:00`,
      });
      return success({
        resource,
        date,
        openings: [range("08:00", "22:00")],
        blocks: [
          {
            ...range("10:00", "12:00"),
            blockType: "TEACHING",
            reason: "服务端课表",
          },
        ],
        reservations: [],
        availablePeriods: resource.enabled
          ? [range("08:00", "10:00"), range("12:00", "22:00")]
          : [],
      });
    }
    if (path === "/api/v1/reservations/mine") {
      if (startedMine) {
        startedMine();
        startedMine = undefined;
        await new Promise((resolve) => {
          deferredMine = resolve;
        });
      }
      const selected = rows.filter(
        (r) =>
          !parsed.searchParams.get("status") ||
          r.status === parsed.searchParams.get("status"),
      );
      const page = Number(parsed.searchParams.get("page") || 1),
        size = Number(parsed.searchParams.get("size") || 10);
      return success({
        items: selected.slice((page - 1) * size, page * size),
        total: selected.length,
        page,
        size,
      });
    }
    if (path === "/api/v1/reservations" && options.method === "POST") {
      const body = JSON.parse(options.body);
      postBodies.push(body);
      assert.equal(typeof body.resourceId, "string");
      assert.equal(body.userId, undefined);
      if (rejectCsrf) {
        rejectCsrf = false;
        return new Response(
          JSON.stringify({ code: "CSRF_INVALID", message: "过期令牌" }),
          { status: 403 },
        );
      }
      let result = requests.get(body.clientRequestId);
      if (!result) {
        result = booking(
          String(9000 + requests.size),
          body.resourceId,
          body.startTime,
          body.endTime,
        );
        requests.set(body.clientRequestId, result);
        rows.unshift(result);
      }
      if (dropNext) {
        dropNext = false;
        throw new Error("response lost after commit");
      }
      return success(result);
    }
    if (path === "/api/v1/admin/schedules" && options.method === "GET") {
      return success([]);
    }
    if (path.includes("/openings") || path.includes("/teaching-blocks")) {
      // 学生账号在服务端应被拒绝；这里按 403 返回，验证前端只读边界不把写操作当成功。
      return new Response(
        JSON.stringify({ code: "FORBIDDEN", message: "无权限维护时段安排" }),
        { status: 403 },
      );
    }
    if (path.match(/^\/api\/v1\/admin\/schedules\/\d+$/)) {
      return new Response(
        JSON.stringify({ code: "FORBIDDEN", message: "无权限维护时段安排" }),
        { status: 403 },
      );
    }
    throw new Error(`unhandled test URL ${url}`);
  };
  const auth = await server.ssrLoadModule("/src/stores/auth.ts");
  const http = await server.ssrLoadModule("/src/api/http.ts");
  await auth.login("student01", "fixture-password");
  const { useCampus } = await server.ssrLoadModule(
    "/src/composables/useCampus.ts",
  );
  const campus = useCampus();
  await campus.initialize();
  assert.equal(campus.isDemo, false);
  assert.equal(campus.state.ready, true);
  assert.equal(campus.state.resources.length, 3);
  assert.ok(campus.state.resources.some((r) => r.id === largeId));
  assert.ok(campus.state.resources.some((r) => r.id === "777" && r.disabled));
  assert.equal(
    campus.state.resources.find((r) => r.id === largeId).capacity,
    undefined,
  );
  assert.equal(campus.state.bookings[0].id, "9007199254741990");
  await campus.loadAvailability(largeId, dateAfter(1));
  assert.equal(
    campus.slots(largeId, dateAfter(1)).find((s) => s.start === 10).available,
    false,
  );
  assert.equal(
    campus.slots(largeId, dateAfter(1)).find((s) => s.start === 12).available,
    true,
  );
  console.log(
    "PASS: real-mode DTO mapping, opaque BIGINT IDs, disabled history resources and server free periods",
  );
  dropNext = true;
  assert.equal(await campus.reserve(largeId, dateAfter(1), 12), false);
  assert.equal(await campus.reserve(largeId, dateAfter(1), 12), true);
  assert.equal(postBodies[0].clientRequestId, postBodies[1].clientRequestId);
  assert.equal(requests.size, 1);
  rejectCsrf = true;
  assert.equal(await campus.reserve(largeId, dateAfter(1), 13), true);
  assert.equal(
    postBodies.at(-1).clientRequestId,
    postBodies.at(-2).clientRequestId,
  );
  assert.equal(requests.size, 2);
  assert.ok(csrf >= 3);
  await campus.loadBookings("COMPLETED", 1, 10);
  assert.equal(campus.state.bookingTotal, 0);
  assert.deepEqual(campus.state.bookings, []);
  await campus.loadBookings(undefined, 1, 10);
  assert.equal(campus.state.bookingTotal, 3);
  assert.equal(campus.state.scheduleWritable, true);
  assert.equal(
    await campus.addOccupancy(largeId, dateAfter(1), 16, 17, "EXTERNAL", ""),
    false,
  );
  await campus.loadSchedules(dateAfter(1), largeId);
  assert.deepEqual(campus.state.schedules, []);
  assert.equal(
    await campus.createSchedule({
      resourceId: largeId,
      type: "TEACHING",
      startTime: `${dateAfter(1)}T10:00:00+08:00`,
      endTime: `${dateAfter(1)}T12:00:00+08:00`,
      note: "学生尝试占用",
    }),
    false,
  );
  console.log(
    "PASS: lost-response idempotent retry, CSRF refresh, server pagination and read-only schedule boundary",
  );
  const managedPage = await campus.loadManagedResources({ page: 1, size: 20 });
  assert.equal(managedPage.total, 2);
  assert.equal(managedPage.items[1].enabled, false);
  const createdResource = await campus.saveResource(null, {
    name: "接口新增资源",
    location: "接口图书馆",
    resourceType: "LIBRARY_SEAT",
    unitLabel: "S-02",
    enabled: true,
  });
  assert.equal(createdResource.id, "901");
  assert.equal(
    (await campus.loadManagedResources({ page: 1, size: 20 })).total,
    3,
  );
  const updatedResource = await campus.saveResource("777", {
    name: "停用的历史教室",
    location: "接口教学楼",
    resourceType: "CLASSROOM",
    unitLabel: "R-01",
    enabled: true,
  });
  assert.equal(updatedResource.enabled, true);
  console.log(
    "PASS: admin resource list, create and update use server results",
  );
  const { createRouter, createMemoryHistory } = await server.ssrLoadModule(
    "virtual:api-check-router",
  );
  const { navigationPages } = await server.ssrLoadModule(
    "/src/config/pages.ts",
  );
  const App = (await server.ssrLoadModule("/src/App.vue")).default;
  const router = createRouter({
    history: createMemoryHistory(),
    routes: navigationPages.map((p) => ({
      path: p.to,
      component: p.component,
      meta: { title: p.label },
    })),
  });
  await router.push("/resources");
  await router.isReady();
  const html = await renderToString(
    createSSRApp({ render: () => h(App) }).use(router),
  );
  assert.ok(html.includes("接口羽毛球场"));
  assert.ok(html.includes("后端接口模式"));
  assert.ok(!html.includes('href="/admin/schedules"'));
  assert.ok(!html.includes('href="/admin/resources"'));
  offlineLogout = true;
  await assert.rejects(auth.logout());
  assert.ok(auth.authState.user);
  offlineLogout = false;
  let startedResolve;
  const started = new Promise((resolve) => {
    startedResolve = resolve;
  });
  startedMine = startedResolve;
  const stale = campus.loadBookings(undefined, 1, 10);
  await started;
  campus.reset();
  deferredMine();
  await stale;
  assert.deepEqual(campus.state.bookings, []);
  assert.deepEqual(campus.state.resources, []);
  http.setUnauthorizedHandler(() => {
    auth.clearAuth();
    campus.reset();
  });
  rejectAll = true;
  await campus.initialize(true);
  assert.equal(auth.authState.user, null);
  assert.equal(campus.state.ready, false);
  assert.deepEqual(campus.state.resources, []);
  assert.deepEqual(campus.state.bookings, []);
  console.log(
    "PASS: protected menu rendering, safe return paths, logout failure, stale-response isolation and 401 data clearing",
  );
} finally {
  globalThis.fetch = originalFetch;
  await server.close();
}
console.log(
  "API adapter checks passed with mocked HTTP. Real server/database/browser checks remain separate.",
);
