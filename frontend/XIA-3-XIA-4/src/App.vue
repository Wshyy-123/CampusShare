<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header class="topbar">
    <div class="topbar-inner">
      <RouterLink to="/" class="brand">校享 CampusShare</RouterLink>
      <nav class="nav">
        <RouterLink to="/resources" class="nav-link">资源查询</RouterLink>
        <RouterLink to="/admin/schedules" class="nav-link">时段管理</RouterLink>
      </nav>
    </div>
  </header>
  <main class="page">
    <RouterView />
  </main>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: color-mix(in srgb, var(--bg) 85%, transparent);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}
.topbar-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  font-weight: 600;
  color: var(--text);
  text-decoration: none;
}
.nav { display: flex; gap: 16px; }
.nav-link {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
}
.nav-link:hover { color: var(--text); }
</style>
