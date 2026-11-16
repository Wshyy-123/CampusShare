import { createRouter, createWebHistory } from "vue-router";
import { navigationPages } from "../config/pages";
import { authState, restoreAuth, clearAuth } from "../stores/auth";
import { setUnauthorizedHandler } from "../api/http";
import { useCampus, notify } from "../composables/useCampus";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/resources" },
    ...navigationPages.map((page) => ({
      path: page.to,
      name: page.name,
      component: page.component,
      meta: {
        title: page.label,
        roles: "roles" in page ? page.roles : undefined,
      },
    })),
    {
      path: "/login",
      name: "login",
      component: () => import("../views/LoginView.vue"),
      meta: { title: "登录", public: true },
    },
    {
      path: "/resources/:id/reserve",
      component: () => import("../views/ReservationCreateView.vue"),
      meta: { title: "预约资源" },
    },
    {
      path: "/bookings/:id",
      component: () => import("../views/ReservationDetailView.vue"),
      meta: { title: "预约详情" },
    },
    { path: "/reservations", redirect: "/bookings" },
    {
      path: "/reservations/:id",
      redirect: (to) => `/bookings/${String(to.params.id)}`,
    },
    { path: "/:pathMatch(.*)*", redirect: "/resources" },
  ],
  scrollBehavior: () => ({ top: 0 }),
});

const campus = useCampus();
setUnauthorizedHandler(() => {
  clearAuth();
  campus.reset();
  if (
    router.currentRoute.value.matched.length &&
    router.currentRoute.value.path !== "/login"
  )
    void router.replace({
      path: "/login",
      query: { redirect: router.currentRoute.value.fullPath },
    });
});
router.beforeEach(async (to) => {
  if (campus.isDemo || to.meta.public) return true;
  await restoreAuth();
  if (!authState.user)
    return { path: "/login", query: { redirect: to.fullPath } };
  if (
    Array.isArray(to.meta.roles) &&
    !to.meta.roles.includes(authState.user.role)
  ) {
    notify("当前账号没有访问该管理页面的权限。");
    return "/resources";
  }
  await campus.initialize();
  if (!authState.user)
    return { path: "/login", query: { redirect: to.fullPath } };
  return true;
});
export default router;
