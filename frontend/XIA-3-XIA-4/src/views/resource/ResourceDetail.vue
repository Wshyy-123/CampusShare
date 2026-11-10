<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getResourceAvailability } from '../../api/resource'
import { ApiError } from '../../api/request'
import type { ResourceAvailability, UnitAvailability, TimeRange } from '../../types'

const route = useRoute()
const router = useRouter()
const resourceId = Number(route.params.id)

const date = ref<string>(new Date().toISOString().slice(0, 10))
const loading = ref(false)
const error = ref('')
const data = ref<ResourceAvailability | null>(null)

// 时间轴显示范围：06:00 - 23:00
const DAY_START_HOUR = 6
const DAY_END_HOUR = 23
const DAY_SPAN_HOURS = DAY_END_HOUR - DAY_START_HOUR // 17

function timeOfDay(iso: string): number {
  // 返回当天的小时（小数）
  const d = new Date(iso)
  return d.getHours() + d.getMinutes() / 60
}

function fmtTime(iso: string): string {
  const d = new Date(iso)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

function blockStyle(r: TimeRange) {
  const start = Math.max(timeOfDay(r.startAt), DAY_START_HOUR)
  const end = Math.min(timeOfDay(r.endAt), DAY_END_HOUR)
  return {
    left: `${((start - DAY_START_HOUR) / DAY_SPAN_HOURS) * 100}%`,
    width: `${((end - start) / DAY_SPAN_HOURS) * 100}%`
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await getResourceAvailability(resourceId, date.value)
  } catch (e) {
    data.value = null
    error.value = e instanceof ApiError ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

const hourTicks = computed(() => {
  const arr: number[] = []
  for (let h = DAY_START_HOUR; h <= DAY_END_HOUR; h += 3) arr.push(h)
  return arr
})
</script>

<template>
  <section>
    <button class="btn btn-sm" @click="router.push('/resources')">← 返回列表</button>

    <div class="card" style="margin-top:12px">
      <div class="row">
        <h2 v-if="data" style="margin:0">{{ data.resourceName }}</h2>
        <h2 v-else style="margin:0">资源详情</h2>
        <span v-if="data" class="tag" :class="data.resourceStatus === 'DISABLED' ? 'tag-danger' : 'tag-success'">
          {{ data.resourceStatus === 'DISABLED' ? '已停用' : '可用' }}
        </span>
      </div>
      <p class="muted" v-if="data">类型：{{ data.resourceType }}</p>

      <div class="filter-row">
        <label>
          日期
          <input v-model="date" type="date" class="input" @change="load" />
        </label>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>
    </div>

    <div v-if="loading" class="card muted" style="margin-top:12px">加载中…</div>

    <div v-else-if="data" style="margin-top:12px; display:flex; flex-direction:column; gap:12px">
      <div v-for="u in data.units" :key="u.unitId" class="card">
        <div class="row">
          <h3 style="margin:0">
            {{ u.unitName }}
            <span class="muted" style="font-weight:400;font-size:13px">
              ({{ u.unitCode || `unit #${u.unitId}` }})
            </span>
          </h3>
          <span class="tag" :class="u.unitStatus === 'DISABLED' ? 'tag-danger' : (u.hasAvailablePeriod ? 'tag-success' : 'tag-warning')">
            {{ u.unitStatus === 'DISABLED' ? '已停用' : (u.hasAvailablePeriod ? '可预约' : '暂无可约') }}
          </span>
        </div>
        <p class="muted" style="margin:6px 0 12px">{{ u.message }}</p>

        <!-- 时间轴 -->
        <div class="timeline">
          <div class="track">
            <div
              v-for="(r, i) in u.teachingPeriods"
              :key="'t'+i"
              class="seg seg-teaching"
              :style="blockStyle(r)"
              :title="`教学占用 ${fmtTime(r.startAt)}-${fmtTime(r.endAt)}`"
            />
            <div
              v-for="(r, i) in u.reservationPeriods"
              :key="'r'+i"
              class="seg seg-reserved"
              :style="blockStyle(r)"
              :title="`已预约 ${fmtTime(r.startAt)}-${fmtTime(r.endAt)}`"
            />
            <div
              v-for="(r, i) in u.blockedPeriods"
              :key="'b'+i"
              class="seg seg-blocked"
              :style="blockStyle(r)"
              :title="`停用/维护 ${fmtTime(r.startAt)}-${fmtTime(r.endAt)}`"
            />
          </div>
          <div class="ticks">
            <span v-for="h in hourTicks" :key="h" :style="{ left: `${((h - DAY_START_HOUR) / DAY_SPAN_HOURS) * 100}%` }">
              {{ String(h).padStart(2, '0') }}:00
            </span>
          </div>
        </div>

        <!-- 图例 -->
        <div class="legend">
          <span class="legend-item"><i class="dot dot-open" /> 开放时段</span>
          <span class="legend-item"><i class="dot dot-teaching" /> 教学占用</span>
          <span class="legend-item"><i class="dot dot-reserved" /> 已预约</span>
          <span class="legend-item"><i class="dot dot-blocked" /> 停用维护</span>
          <span class="legend-item"><i class="dot dot-free" /> 可预约</span>
        </div>

        <!-- 可约时段列表 -->
        <div style="margin-top:12px">
          <div v-if="u.availablePeriods.length === 0" class="muted">
            {{ u.unitStatus === 'DISABLED' ? '该单位已停用，不可预约。' : '当日暂无可预约时段。' }}
          </div>
          <div v-else>
            <div class="muted" style="margin-bottom:6px">可预约时段：</div>
            <div style="display:flex; flex-wrap:wrap; gap:6px">
              <span
                v-for="(r, i) in u.availablePeriods"
                :key="i"
                class="tag tag-success"
                style="font-size:13px; padding:4px 10px"
              >
                {{ fmtTime(r.startAt) }} – {{ fmtTime(r.endAt) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.row { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.filter-row { display: flex; gap: 10px; margin-top: 14px; }
.filter-row label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: var(--text-secondary); }

.timeline { margin: 10px 0 4px; }
.track {
  position: relative;
  height: 28px;
  background: var(--success-soft);
  border-radius: 6px;
  overflow: hidden;
}
.seg { position: absolute; top: 0; bottom: 0; }
.seg-teaching { background: rgba(255, 69, 58, 0.55); }
.seg-reserved { background: rgba(10, 132, 255, 0.55); }
.seg-blocked { background: rgba(191, 90, 242, 0.55); }
.ticks {
  position: relative;
  height: 16px;
  font-size: 10px;
  color: var(--text-muted);
  font-family: var(--mono);
  margin-top: 2px;
}
.ticks span { position: absolute; transform: translateX(-50%); }

.legend {
  display: flex; flex-wrap: wrap; gap: 12px;
  font-size: 12px; color: var(--text-secondary); margin-top: 8px;
}
.legend-item { display: inline-flex; align-items: center; gap: 4px; }
.dot { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
.dot-open { background: var(--success-soft); border: 1px solid var(--success); }
.dot-teaching { background: rgba(255, 69, 58, 0.55); }
.dot-reserved { background: rgba(10, 132, 255, 0.55); }
.dot-blocked { background: rgba(191, 90, 242, 0.55); }
.dot-free { background: var(--success); }
</style>
