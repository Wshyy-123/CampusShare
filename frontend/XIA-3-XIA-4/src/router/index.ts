import { createRouter, createWebHistory } from 'vue-router'
import ResourceList from '../views/resource/ResourceList.vue'
import ResourceDetail from '../views/resource/ResourceDetail.vue'
import ScheduleManage from '../views/admin/ScheduleManage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/resources' },
    { path: '/resources', name: 'resource-list', component: ResourceList },
    { path: '/resources/:id', name: 'resource-detail', component: ResourceDetail },
    { path: '/admin/schedules', name: 'schedule-manage', component: ScheduleManage },
    { path: '/:pathMatch(.*)*', redirect: '/resources' }
  ]
})

export default router
