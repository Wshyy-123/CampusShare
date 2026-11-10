import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { clearAuth } from './stores/auth'
import { setUnauthorizedHandler } from './api/http'
import './style.css'

setUnauthorizedHandler(() => {
  clearAuth()
  if (router.currentRoute.value.meta.requiresAuth) {
    void router.replace({
      name: 'login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
  }
})

createApp(App).use(router).mount('#app')
