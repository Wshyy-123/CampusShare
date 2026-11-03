<script setup lang="ts">
import { ref } from 'vue'
import LibraryTerminal from './components/LibraryTerminal.vue'
import SportsReservationView from './views/SportsReservationView.vue'

type Tab = 'sports' | 'library'

const activeTab = ref<Tab>('sports')
</script>

<template>
  <div class="page-bg">
    <div class="shell">
      <nav class="tabs" aria-label="功能切换">
        <button
          type="button"
          :class="{ active: activeTab === 'sports' }"
          @click="activeTab = 'sports'"
        >
          体育预约
        </button>
        <button
          type="button"
          :class="{ active: activeTab === 'library' }"
          @click="activeTab = 'library'"
        >
          图书馆刷卡
        </button>
      </nav>

      <SportsReservationView v-if="activeTab === 'sports'" />
      <LibraryTerminal v-else />
    </div>
  </div>
</template>

<style scoped>
.page-bg {
  min-height: 100vh;
  padding: 28px 0 40px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  background: radial-gradient(circle at top, #1e1e24, #0d0d11);
  color: #fff;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}
.shell {
  width: min(480px, 92vw);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.tabs {
  display: flex;
  gap: 6px;
  padding: 6px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}
.tabs button {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #a1a1aa;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.tabs button.active {
  background: rgba(66, 184, 131, 0.18);
  color: #ffffff;
}
</style>
