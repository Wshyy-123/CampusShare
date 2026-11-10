<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { errorMessage } from '../api/http'
import { reservationsApi } from '../api/reservations'
import type { PageData, Reservation } from '../types/api'
import StatusBadge from '../components/StatusBadge.vue'

const status = ref('')
const page = ref(1)
const size = 10
const data = ref<PageData<Reservation> | null>(null)
const loading = ref(true)
const error = ref('')

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'RESERVED', label: '已预约' },
  { value: 'CHECKED_IN', label: '已确认到馆' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

const totalPages = computed(() => {
  if (!data.value) return 1
  return Math.max(1, Math.ceil(data.value.total / data.value.size))
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await reservationsApi.mine({
      status: status.value || undefined,
      page: page.value,
      size,
    })
  } catch (exception) {
    error.value = errorMessage(exception)
  } finally {
    loading.value = false
  }
}

function changeStatus(value: string) {
  status.value = value
  page.value = 1
  void load()
}

function goPage(next: number) {
  if (next < 1 || next > totalPages.value) return
  page.value = next
  void load()
}

function formatDateTime(iso: string): string {
  return `${iso.slice(0, 10)} ${iso.slice(11, 16)}`
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-head">
      <div>
        <h1>我的预约</h1>
        <p>只展示当前登录用户有权查看的预约及其资源、时段和状态。</p>
      </div>
      <RouterLink class="btn btn-primary" to="/resources">去预约</RouterLink>
    </div>

    <div class="filter-chips">
      <button
        v-for="option in statusOptions"
        :key="option.value"
        type="button"
        class="chip"
        :class="{ 'chip-active': status === option.value }"
        @click="changeStatus(option.value)"
      >
        {{ option.label }}
      </button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="loading" class="card empty">正在加载我的预约…</div>

    <div v-else-if="!data || data.items.length === 0" class="card empty">
      <strong>暂无预约记录</strong>
      <p class="small">从资源列表选择一个场地或座位开始预约。</p>
      <RouterLink class="btn btn-primary" to="/resources">浏览资源</RouterLink>
    </div>

    <template v-else>
      <div class="reservation-list">
        <RouterLink
          v-for="item in data.items"
          :key="item.reservationId"
          class="card reservation-card"
          :to="`/reservations/${item.reservationId}`"
        >
          <div class="reservation-main">
            <div class="reservation-title">
              <h2>{{ item.resourceName }}</h2>
              <StatusBadge :status="item.status" :cancel-reason="item.cancelReason" :resource-type="item.resourceType" />
            </div>
            <p class="reservation-time">
              {{ formatDateTime(item.startTime) }} – {{ item.endTime.slice(11, 16) }}
              · {{ item.location }} · {{ item.unitLabel }}
            </p>
            <p v-if="item.status === 'RESERVED'" class="small">
              最晚确认时间：{{ formatDateTime(item.arrivalDeadline) }}
            </p>
            <p v-else-if="item.status === 'CANCELLED'" class="small">
              取消原因：{{ item.cancelReason ?? '—' }}
            </p>
          </div>
          <div class="reservation-side">
            <span class="small mono">#{{ item.reservationId }}</span>
            <span class="detail-link">查看详情 →</span>
          </div>
        </RouterLink>
      </div>

      <div class="pager">
        <span>共 {{ data.total }} 条 · 第 {{ data.page }} / {{ totalPages }} 页</span>
        <div class="inline-actions">
          <button class="btn btn-ghost btn-small" type="button" :disabled="page <= 1" @click="goPage(page - 1)">
            上一页
          </button>
          <button
            class="btn btn-ghost btn-small"
            type="button"
            :disabled="page >= totalPages"
            @click="goPage(page + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </template>
  </section>
</template>
