// 同学增加主功能时，在这里注册页面、标题与导航；局部表单使用弹窗，不额外开浏览器窗口。
export const navigationPages = [
  {
    to: "/resources",
    name: "resources",
    label: "发现空间",
    icon: "grid",
    component: () => import("../views/ResourceExplorer.vue"),
  },
  {
    to: "/bookings",
    name: "bookings",
    label: "我的预约",
    icon: "calendar",
    component: () => import("../views/MyReservations.vue"),
  },
  {
    to: "/admin/schedules",
    name: "schedules",
    label: "时段管理",
    icon: "settings",
    roles: ["RESOURCE_ADMIN"],
    component: () => import("../views/ScheduleManage.vue"),
  },
  {
    to: "/admin/resources",
    name: "resource-management",
    label: "资源管理",
    icon: "settings",
    roles: ["RESOURCE_ADMIN"],
    component: () => import("../views/ResourceManage.vue"),
  },
];
