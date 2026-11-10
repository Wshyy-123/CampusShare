import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import ResourceListView from '../views/ResourceListView.vue'
import ReservationCreateView from '../views/ReservationCreateView.vue'
import MyReservationsView from '../views/MyReservationsView.vue'
import ReservationDetailView from '../views/ReservationDetailView.vue'
import { authState, restoreAuth } from '../stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/resources' },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/resources', name: 'resources', component: ResourceListView, meta: { requiresAuth: true } },
  { path: '/reserve/:id', name: 'reserve', component: ReservationCreateView, meta: { requiresAuth: true } },
  {
    path: '/reservations',
    name: 'my-reservations',
    component: MyReservationsView,
    meta: { requiresAuth: true },
  },
  {
    path: '/reservations/:id',
    name: 'reservation-detail',
    component: ReservationDetailView,
    meta: { requiresAuth: true },
  },
  { path: '/:pathMatch(.*)*', redirect: '/resources' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  if (!authState.initialized) {
    await restoreAuth()
  }
  if (to.meta.requiresAuth && !authState.user) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && authState.user) {
    return { name: 'resources' }
  }
  return true
})

export default router
