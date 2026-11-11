import assert from "node:assert/strict";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { createServer } from "vite";
import { createSSRApp, h } from "vue";
import { renderToString } from "@vue/server-renderer";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
async function runtime(mode) {
  return createServer({
    root,
    mode: "test",
    envDir: false,
    appType: "custom",
    server: { middlewareMode: true },
    define: { "import.meta.env.VITE_DATA_MODE": JSON.stringify(mode) },
    plugins: [
      {
        name: "baseline-check-runtime",
        resolveId(id) {
          if (id === "virtual:baseline-router") return "\0baseline-router";
        },
        load(id) {
          if (id === "\0baseline-router")
            return "export { createRouter, createMemoryHistory } from 'vue-router'";
        },
      },
    ],
  });
}

const server = await runtime("demo");
const originalFetch = globalThis.fetch;
try {
  const { createDemoCampusService } = await server.ssrLoadModule(
    "/src/services/demoCampusService.ts",
  );
  const { localDate } = await server.ssrLoadModule(
    "/src/utils/presentation.ts",
  );
  const date = localDate();
  const demo = createDemoCampusService();
  assert.equal(demo.state.resources.length, 6);
  assert.deepEqual(demo.slots("2", ""), []);
  assert.equal(await demo.reserve("2", date, 9), false, "teaching conflict");
  assert.equal(await demo.reserve("3", date, 14), false, "external conflict");
  assert.equal(
    await demo.reserve("1", date, 18),
    false,
    "maintenance conflict",
  );
  assert.equal(await demo.reserve("5", date, 12), false, "disabled resource");
  assert.equal(
    await demo.reserve("2", date, 18),
    false,
    "existing reservation",
  );
  assert.equal(await demo.reserve("2", date, 12), true);
  assert.equal(await demo.reserve("2", date, 12), false, "duplicate");
  assert.equal(
    demo.slots("2", date).find((s) => s.start === 13).available,
    true,
    "adjacent boundary",
  );
  assert.equal(
    await demo.addOccupancy("2", date, 12, 14, "TEACHING", ""),
    false,
  );
  assert.equal(
    await demo.addOccupancy("2", date, 13, 14, "TEACHING", ""),
    true,
  );
  assert.equal(
    demo.slots("2", date).find((s) => s.start === 13).available,
    false,
  );
  assert.equal(
    await demo.addOccupancy("2", date, 14, 13, "TEACHING", ""),
    false,
  );
  console.log(
    "PASS: demo exclusions, conflicts, adjacent intervals and shared occupancy",
  );

  const { request } = await server.ssrLoadModule("/src/api/request.ts");
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    throw new Error("offline");
  };
  await assert.rejects(request("/reservations", { method: "POST" }), {
    code: "CSRF_REQUIRED",
  });
  assert.equal(calls, 0);
  await assert.rejects(request("//example.invalid"), { code: "INVALID_PATH" });
  await assert.rejects(request("/resources"), { code: "NETWORK_ERROR" });
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ code: "FORBIDDEN", message: "无权限" }), {
      status: 403,
    });
  await assert.rejects(request("/resources"), {
    code: "FORBIDDEN",
    status: 403,
  });
  globalThis.fetch = async () => new Response("<html>broken</html>");
  await assert.rejects(request("/resources"), { code: "BAD_RESPONSE" });
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "/api/v1/reservations");
    assert.equal(options.credentials, "same-origin");
    assert.equal(options.headers["X-CSRF-DEMO"], "test-token");
    assert.equal(options.headers.Authorization, undefined);
    return new Response(JSON.stringify({ code: "OK", data: { id: 42 } }));
  };
  assert.deepEqual(
    await request("/reservations", {
      method: "POST",
      body: { resourceId: 2 },
      csrf: { headerName: "X-CSRF-DEMO", token: "test-token" },
    }),
    { id: 42 },
  );
  globalThis.fetch = originalFetch;
  console.log(
    "PASS: request errors, no silent success, session/CSRF transport",
  );

  const { useCampus } = await server.ssrLoadModule(
    "/src/composables/useCampus.ts",
  );
  const campus = useCampus();
  await campus.initialize();
  assert.equal(await campus.reserve("2", date, 12), true);
  const App = (await server.ssrLoadModule("/src/App.vue")).default;
  const { navigationPages } = await server.ssrLoadModule(
    "/src/config/pages.ts",
  );
  assert.equal(
    new Set(navigationPages.map((p) => p.to)).size,
    navigationPages.length,
  );
  assert.equal(
    new Set(navigationPages.map((p) => p.name)).size,
    navigationPages.length,
  );
  const { createRouter, createMemoryHistory } = await server.ssrLoadModule(
    "virtual:baseline-router",
  );
  const expected = {
    "/resources": ["靠窗 A12", "羽毛球 2 号场"],
    "/bookings": ["本地演示预约", "12:00"],
    "/admin/schedules": ["占用时段管理", "模拟教务课表"],
  };
  for (const page of navigationPages) {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: navigationPages.map((p) => ({
        path: p.to,
        name: p.name,
        component: p.component,
        meta: { title: p.label },
      })),
    });
    await router.push(page.to);
    await router.isReady();
    const app = createSSRApp({ render: () => h(App) }).use(router);
    const html = await renderToString(app);
    for (const content of [...expected[page.to], "前端交互演示"])
      assert.ok(html.includes(content), `${page.to}: ${content}`);
    console.log(`PASS: ${page.to} route and populated component rendering`);
  }
} finally {
  globalThis.fetch = originalFetch;
  await server.close();
}

const apiRuntime = await runtime("api");
try {
  const { useCampus } = await apiRuntime.ssrLoadModule(
    "/src/composables/useCampus.ts",
  );
  const api = useCampus();
  assert.equal(api.isDemo, false);
  await api.initialize();
  assert.equal(api.state.ready, false);
  assert.ok(api.state.error.includes("无法连接"));
  assert.deepEqual(api.state.resources, []);
  assert.deepEqual(api.state.bookings, []);
  assert.equal(await api.reserve("2", "2099-01-01", 12), false);
  assert.deepEqual(api.slots("2", "2099-01-01"), []);
  console.log(
    "PASS: offline API mode stays unavailable and never falls back to demo",
  );
} finally {
  await apiRuntime.close();
}
console.log(
  "Baseline checks passed. Browser layout, real API, authentication and lifecycle are not covered.",
);
