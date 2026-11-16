import assert from "node:assert/strict";
import { createServer } from "vite";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import QRCode from "qrcode";
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
  plugins: [
    {
      name: "attendance-memory-router",
      transform(code, id) {
        if (
          id
            .replaceAll("\\\\", "/")
            .split("?")[0]
            .endsWith("/src/router/index.ts")
        )
          return code.replaceAll("createWebHistory", "createMemoryHistory");
      },
    },
  ],
});
const originalFetch = globalThis.fetch;
try {
  const resourceId = "9007199254740993",
    reservationId = "9007199254740995";
  const resource = {
    resourceId,
    name: "扫码球场",
    location: "测试体育馆",
    resourceType: "BADMINTON",
    unitLabel: "2号场",
    enabled: true,
  };
  const day = "2030-01-02",
    startTime = `${day}T10:00:00+08:00`,
    endTime = `${day}T11:00:00+08:00`;
  let logged = false,
    status = "RESERVED",
    reason = null,
    mutationCount = 0;
  const booking = () => ({
    reservationId,
    resourceId,
    resourceName: resource.name,
    resourceType: resource.resourceType,
    location: resource.location,
    unitLabel: resource.unitLabel,
    startTime,
    endTime,
    status,
    acceptedAt: `${day}T09:59:00+08:00`,
    arrivalDeadline: `${day}T10:15:00+08:00`,
    checkInAt:
      status === "CHECKED_IN" || status === "COMPLETED" ? startTime : null,
    checkOutAt: status === "COMPLETED" ? endTime : null,
    cancelReason: reason,
    createdAt: startTime,
  });
  const ok = (data) =>
    new Response(JSON.stringify({ code: "OK", data }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  globalThis.fetch = async (url, options = {}) => {
    const target = new URL(String(url), "http://localhost"),
      path = target.pathname;
    if (path.endsWith("/auth/csrf"))
      return ok({ token: "qr-token", headerName: "X-CSRF-TOKEN" });
    if (path.endsWith("/auth/login")) {
      logged = true;
      return ok({
        id: "1",
        account: "student01",
        displayName: "测试学生",
        role: "STUDENT",
      });
    }
    if (!logged)
      return new Response(
        JSON.stringify({ code: "AUTH_REQUIRED", message: "请登录" }),
        { status: 401 },
      );
    if (path.endsWith("/auth/me"))
      return ok({
        id: "1",
        account: "student01",
        displayName: "测试学生",
        role: "STUDENT",
      });
    if (path.startsWith("/api/v1/places/")) {
      assert.equal(options.method ?? "GET", "GET");
      return ok({
        place: resource,
        reservation: booking(),
        serverTime: startTime,
      });
    }
    if (options.method === "POST" && path.includes("/reservations/")) {
      assert.equal(options.headers["X-CSRF-TOKEN"], "qr-token");
      mutationCount++;
      if (path.endsWith("/cancel")) {
        status = "CANCELLED";
        reason = "USER_CANCEL";
      } else {
        assert.deepEqual(JSON.parse(options.body), {
          resourceId,
          source: "QR",
        });
        status = path.endsWith("/check-in") ? "CHECKED_IN" : "COMPLETED";
      }
      return ok(booking());
    }
    if (path === "/api/v1/resources") return ok([resource]);
    if (path.endsWith("/mine"))
      return ok({ page: 1, size: 10, total: 1, items: [booking()] });
    if (path.endsWith("/availability")) {
      const date = target.searchParams.get("date");
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
        availablePeriods:
          status === "COMPLETED" || status === "CANCELLED" ? [range] : [],
      });
    }
    throw new Error(`Unexpected request ${path}`);
  };
  const { placeCodeUrl } = await server.ssrLoadModule(
    "/src/utils/placeCode.ts",
  );
  const url = placeCodeUrl(resourceId, "https://campus.example/");
  assert.equal(url, `https://campus.example/places/${resourceId}`);
  assert.throws(() => placeCodeUrl(resourceId, "javascript:alert(1)"));
  assert.match(await QRCode.toDataURL(url), /^data:image\/png;base64,/);
  const { default: router } = await server.ssrLoadModule(
    "/src/router/index.ts",
  );
  await router.push(`/places/${resourceId}`);
  assert.equal(router.currentRoute.value.path, "/login");
  assert.equal(
    router.currentRoute.value.query.redirect,
    `/places/${resourceId}`,
  );
  const auth = await server.ssrLoadModule("/src/stores/auth.ts");
  await auth.login("student01", "test");
  await router.push(String(router.currentRoute.value.query.redirect));
  assert.equal(router.currentRoute.value.name, "place-attendance");
  const { RouterView } = await import("vue-router");
  const before = mutationCount;
  await renderToString(
    createSSRApp({ render: () => h(RouterView) }).use(router),
  );
  assert.equal(mutationCount, before, "SSR preview must not post attendance");
  const { placesApi } = await server.ssrLoadModule("/src/api/places.ts");
  await placesApi.get(resourceId);
  assert.equal(mutationCount, before);
  const { useCampus } = await server.ssrLoadModule(
    "/src/composables/useCampus.ts",
  );
  const campus = useCampus();
  assert.ok(
    await campus.checkInAtPlace(reservationId, { resourceId, source: "QR" }),
  );
  assert.equal(campus.state.bookings[0].state, "CHECKED_IN");
  assert.ok(
    await campus.checkOutAtPlace(reservationId, { resourceId, source: "QR" }),
  );
  assert.equal(campus.state.bookings[0].state, "COMPLETED");
  assert.ok(await campus.cancelReservation(reservationId));
  assert.equal(campus.state.bookings[0].cancelReason, "USER_CANCEL");
  console.log(
    "PASS: concrete-place QR URL/PNG, login return path, GET/SSR read-only behavior, CSRF scan payload and shared state refresh",
  );
} finally {
  globalThis.fetch = originalFetch;
  await server.close();
}
