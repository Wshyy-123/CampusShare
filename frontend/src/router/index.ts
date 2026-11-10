import { createRouter, createWebHistory } from "vue-router";
import { navigationPages } from "../config/pages";

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/resources" },
    ...navigationPages.map((page) => ({
      path: page.to,
      name: page.name,
      component: page.component,
      meta: { title: page.label },
    })),
    { path: "/:pathMatch(.*)*", redirect: "/resources" },
  ],
  scrollBehavior: () => ({ top: 0 }),
});
