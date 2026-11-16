<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { searchResources } from '../../api/resource'
import { ApiError } from '../../api/request'
import type { ResourceListItem, ResourceType } from '../../types'

const router = useRouter()

const keyword = ref('')
const type = ref<ResourceType | ''>('')
const page = ref(1)
const size = 20

const loading = ref(false)
const items = ref<ResourceListItem[]>([])
const total = ref(0)
const error = ref('')

const typeOptions: { value: ResourceType | ''; label: string }[] = [
  { value: '', label: '全部类型' },
  { value: 'LIBRARY_SEAT', label: '图书馆座位' },
  { value: 'CLASSROOM', label: '教室' },
  { value: 'BADMINTON', label: '羽毛球' },
  { value: 'BASKETBALL', label: '篮球' },
  { value: 'TENNIS', label: '网球' },
  { value: 'STUDY_ROOM', label: '研讨室' }
]

function typeLabel(t: ResourceType): string {
  return typeOptions.find(o => o.value === t)?.label ?? t
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await searchResources({
      keyword: keyword.value || undefined,
      type: type.value,
      page: page.value,
      size
    })
    items.value = res.items
    total.value = res.total
  } catch (e) {
    items.value = []
    error.value = e instanceof ApiError ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function openDetail(id: number) {
  router.push(`/resources/${id}`)
}

onMounted(load)
</script>

<template>
  <section>
    <h2 style="margin-top:0">资源查询（XIA-4）</h2>
    <p class="muted">选择资源查看详情与当日可预约时段。</p>

    <div class="filter-row card">
      <input
        v-model="keyword"
        class="input"
        placeholder="按名称/位置搜索"
        @keyup.enter="search"
      />
      <select v-model="type" class="input" @change="search">
        <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
      </select>
      <button class="btn btn-primary" @click="search">搜索</button>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div v-if="loading" class="card muted">加载中…</div>

    <div v-else-if="items.length === 0" class="card muted" style="text-align:center;padding:40px">
      暂无可预约资源
    </div>

    <div v-else class="grid">
      <div v-for="r in items" :key="r.id" class="card resource-card" @click="openDetail(r.id)">
        <div class="row">
          <h3 style="margin:0">{{ r.name }}</h3>
          <span class="tag" :class="r.status === 'DISABLED' ? 'tag-danger' : 'tag-success'">
            {{ r.status === 'DISABLED' ? '已停用' : '可用' }}
          </span>
        </div>
        <p class="muted" style="margin:6px 0">{{ r.location || '—' }}</p>
        <div class="row" style="justify-content:space-between">
          <span class="tag tag-muted">{{ typeLabel(r.type) }}</span>
          <span class="muted">{{ r.unitCount }} 个预约单位</span>
        </div>
      </div>
    </div>

    <div v-if="total > size" class="pager">
      <button class="btn" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span class="muted">第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页</span>
      <button class="btn" :disabled="page * size >= total" @click="page++; load()">下一页</button>
    </div>
  </section>
</template>

<style scoped>
.filter-row { display: flex; gap: 10px; margin: 16px 0; flex-wrap: wrap; }
.filter-row .input { flex: 1; min-width: 180px; }
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.resource-card { cursor: pointer; transition: transform 0.15s, box-shadow 0.15s; }
.resource-card:hover { transform: translateY(-2px); }
.row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.pager { display: flex; justify-content: center; align-items: center; gap: 14px; margin-top: 24px; }
</style>
