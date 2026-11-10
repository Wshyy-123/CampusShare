<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { errorMessage } from '../api/http'
import { resourcesApi } from '../api/resources'
import type { Resource } from '../types/api'

const router = useRouter()
const resources = ref<Resource[]>([])
const loading = ref(true)
const error = ref('')
const type = ref('')
const location = ref('')

const typeOptions = [
  { value: '', label: '全部类型' },
  { value: 'LIBRARY_SEAT', label: '图书馆座位' },
  { value: 'BADMINTON', label: '羽毛球场' },
  { value: 'TABLE_TENNIS', label: '乒乓球台' },
  { value: 'TENNIS', label: '网球场' },
  { value: 'DISCUSSION_ROOM', label: '讨论室' },
  { value: 'CLASSROOM', label: '教室' },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    resources.value = await resourcesApi.list(type.value || undefined, location.value.trim() || undefined)
  } catch (exception) {
    error.value = errorMessage(exception)
  } finally {
    loading.value = false
  }
}

function typeLabel(value: string): string {
  return typeOptions.find((item) => item.value === value)?.label ?? value
}

function openReserve(resource: Resource) {
  void router.push({ name: 'reserve', params: { id: resource.resourceId } })
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-head">
      <div>
        <h1>资源预约</h1>
        <p>选择开放中的场地或座位，进入详情后提交预约。</p>
      </div>
      <RouterLink class="btn btn-ghost" to="/reservations">我的预约</RouterLink>
    </div>

    <form class="card filter-card" @submit.prevent="load">
      <label class="field">
        <span>资源类型</span>
        <select v-model="type" class="select" @change="load">
          <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label class="field">
        <span>位置</span>
        <input v-model="location" class="input" placeholder="例如 图书馆 / 体育馆" />
      </label>
      <button class="btn btn-primary" type="submit" :disabled="loading">查询</button>
    </form>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div v-if="loading" class="card empty">正在加载资源…</div>

    <div v-else-if="resources.length === 0" class="card empty">
      <strong>没有找到符合条件的资源</strong>
      <p class="small">换一个类型或位置再试试。</p>
    </div>

    <div v-else class="resource-grid">
      <article v-for="resource in resources" :key="resource.resourceId" class="card resource-card">
        <div class="resource-card-head">
          <span class="pill">{{ typeLabel(resource.resourceType) }}</span>
          <span class="pill pill-success">{{ resource.enabled ? '开放中' : '已停用' }}</span>
        </div>
        <div>
          <h2>{{ resource.name }}</h2>
          <p class="small">{{ resource.location }} · {{ resource.unitLabel }}</p>
        </div>
        <button class="btn btn-primary btn-block" type="button" @click="openReserve(resource)">
          查看可约时段
        </button>
      </article>
    </div>
  </section>
</template>
