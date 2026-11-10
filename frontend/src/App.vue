<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authState, logout, restoreAuth, roleLabel } from './stores/auth'

const router = useRouter()

onMounted(() => {
  void restoreAuth()
})

async function handleLogout() {
  await logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header-inner">
        <RouterLink class="brand" to="/resources">
          <span class="brand-mark">校</span>
          <span>
            校享 CampusShare
            <small>校园公共场地预约</small>
          </span>
        </RouterLink>

        <nav v-if="authState.user" class="nav">
          <RouterLink to="/resources">资源预约</RouterLink>
          <RouterLink to="/reservations">我的预约</RouterLink>
        </nav>

        <div class="user-box">
          <template v-if="authState.user">
            <span class="user-name">
              {{ authState.user.displayName }}
              <small>{{ roleLabel(authState.user.role) }}</small>
            </span>
            <button class="btn btn-ghost btn-small" type="button" @click="handleLogout">退出</button>
          </template>
          <RouterLink v-else-if="!authState.loading" class="btn btn-ghost btn-small" to="/login">
            登录
          </RouterLink>
        </div>
      </div>
    </header>

    <main class="container">
      <RouterView />
    </main>

    <footer class="app-footer">
      <span>校享 CampusShare · Scrum 实验</span>
      <span>XIA-5 创建预约 / XIA-6 我的预约</span>
    </footer>
  </div>
</template>
