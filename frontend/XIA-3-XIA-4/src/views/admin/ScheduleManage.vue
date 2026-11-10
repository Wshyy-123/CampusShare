<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  listSchedules,
  createSchedules,
  updateSchedule,
  deleteSchedule
} from '../../api/schedule'
import { ApiError } from '../../api/request'
import type { SchedulePeriod, SchedulePeriodType } from '../../types'

// 联调时已知的 unitId（XIA-3 demo 数据）。后续接入资源下拉接口后改为动态加载。
const KNOWN_UNITS = [
  { id: 1, name: '图书馆 A101 座位 1' },
  { id: 2, name: '图书馆 A101 座位 2' },
  { id: 9, name: '教学楼 301 教室' }
]

const unitId = ref<number>(1)
const date = ref<string>(new Date().toISOString().slice(0, 10))

const loading = ref(false)
const list = ref<SchedulePeriod[]>([])
const error = ref('')
const success = ref('')

// 新增表单
const showForm = ref(false)
const formType = ref<SchedulePeriodType>('TEACHING')
const formUnitIds = ref<number[]>([1])
const formStart = ref('') // datetime-local
const formEnd = ref('')
const formNote = ref('')
const editingId = ref<number | null>(null)
const submitting = ref(false)

const typeLabel = (t: SchedulePeriodType) => (t === 'OPEN' ? '开放时段' : '教学占用')
const typeTag = (t: SchedulePeriodType) =>
  t === 'OPEN' ? 'tag-success' : 'tag-danger'

function toLocalInput(iso: string): string {
  // OffsetDateTime -> datetime-local 值（YYYY-MM-DDTHH:mm）
  if (!iso) return ''
  return new Date(iso).toISOString().slice(0, 16)
}

function toOffsetIso(local: string): string {
  // datetime-local -> OffsetDateTime（+08:00）
  const d = new Date(local)
  const offsetMin = d.getTimezoneOffset()
  d.setMinutes(d.getMinutes() - offsetMin)
  return d.toISOString().replace('Z', '+08:00')
}

async function loadList() {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    list.value = await listSchedules(unitId.value, date.value)
  } catch (e) {
    list.value = []
    error.value = e instanceof ApiError ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  formType.value = 'TEACHING'
  formUnitIds.value = [unitId.value]
  formStart.value = ''
  formEnd.value = ''
  formNote.value = ''
  showForm.value = true
}

function openEdit(item: SchedulePeriod) {
  editingId.value = item.id
  formType.value = item.type
  formUnitIds.value = [item.unitId]
  formStart.value = toLocalInput(item.startAt)
  formEnd.value = toLocalInput(item.endAt)
  formNote.value = item.note ?? ''
  showForm.value = true
}

function toggleUnit(id: number) {
  const i = formUnitIds.value.indexOf(id)
  if (i >= 0) formUnitIds.value.splice(i, 1)
  else formUnitIds.value.push(id)
}

const formValid = computed(() => {
  if (formUnitIds.value.length === 0) return false
  if (!formStart.value || !formEnd.value) return false
  return new Date(formStart.value) < new Date(formEnd.value)
})

async function submitForm() {
  if (!formValid.value) return
  submitting.value = true
  error.value = ''
  try {
    const payload = {
      type: formType.value,
      startAt: toOffsetIso(formStart.value),
      endAt: toOffsetIso(formEnd.value),
      note: formNote.value || undefined
    }
    if (editingId.value !== null) {
      await updateSchedule(editingId.value, payload)
      success.value = '时段修改成功'
    } else {
      await createSchedules({ ...payload, unitIds: formUnitIds.value })
      success.value = '时段保存成功'
    }
    showForm.value = false
    await loadList()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '保存失败'
  } finally {
    submitting.value = false
  }
}

async function removeItem(item: SchedulePeriod) {
  if (!confirm(`确认删除 ${typeLabel(item.type)} ${item.startAt} ~ ${item.endAt}？`)) return
  try {
    await deleteSchedule(item.id)
    success.value = '时段删除成功'
    await loadList()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '删除失败'
  }
}

onMounted(loadList)
</script>

<template>
  <section class="card">
    <h2 style="margin-top:0">时段管理（XIA-3）</h2>
    <p class="muted">
      维护开放时段（OPEN）与教学占用时段（TEACHING）。仅资源管理员可操作，开始时间必须早于结束时间。
    </p>

    <div class="filter-row">
      <label>
        预约单位
        <select v-model.number="unitId" @change="loadList" class="input">
          <option v-for="u in KNOWN_UNITS" :key="u.id" :value="u.id">{{ u.name }}</option>
        </select>
      </label>
      <label>
        日期
        <input v-model="date" type="date" class="input" @change="loadList" />
      </label>
      <button class="btn btn-primary" @click="openCreate">+ 新增时段</button>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="success" class="tag tag-success" style="margin-top:8px">{{ success }}</p>

    <table v-if="!loading" class="grid" style="margin-top:16px">
      <thead>
        <tr>
          <th>类型</th>
          <th>开始</th>
          <th>结束</th>
          <th>备注</th>
          <th style="width:140px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="list.length === 0">
          <td colspan="5" class="muted" style="text-align:center;padding:24px">
            当日暂无时段安排
          </td>
        </tr>
        <tr v-for="item in list" :key="item.id">
          <td><span class="tag" :class="typeTag(item.type)">{{ typeLabel(item.type) }}</span></td>
          <td class="mono">{{ item.startAt.replace('T', ' ').slice(0, 16) }}</td>
          <td class="mono">{{ item.endAt.replace('T', ' ').slice(0, 16) }}</td>
          <td class="muted">{{ item.note || '-' }}</td>
          <td>
            <button class="btn btn-sm" @click="openEdit(item)">编辑</button>
            <button class="btn btn-sm btn-danger" @click="removeItem(item)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else class="muted">加载中…</p>
  </section>

  <!-- 新增/编辑弹窗 -->
  <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
    <div class="modal card">
      <h3 style="margin-top:0">{{ editingId ? '编辑时段' : '新增时段' }}</h3>

      <label class="field">
        类型
        <select v-model="formType" class="input">
          <option value="TEACHING">教学占用（TEACHING）</option>
          <option value="OPEN">开放时段（OPEN）</option>
        </select>
      </label>

      <div class="field">
        <div class="muted">应用到预约单位（可多选）</div>
        <label v-for="u in KNOWN_UNITS" :key="u.id" style="margin-right:12px;font-size:14px">
          <input
            type="checkbox"
            :checked="formUnitIds.includes(u.id)"
            @change="toggleUnit(u.id)"
          />
          {{ u.name }}
        </label>
      </div>

      <label class="field">
        开始时间
        <input v-model="formStart" type="datetime-local" class="input" />
      </label>
      <label class="field">
        结束时间
        <input v-model="formEnd" type="datetime-local" class="input" />
      </label>
      <p v-if="formStart && formEnd && !formValid" class="error-text">
        开始时间必须早于结束时间
      </p>

      <label class="field">
        备注
        <input v-model="formNote" class="input" maxlength="200" placeholder="如：课程占用" />
      </label>

      <div class="modal-actions">
        <button class="btn" @click="showForm = false">取消</button>
        <button class="btn btn-primary" :disabled="!formValid || submitting" @click="submitForm">
          {{ submitting ? '保存中…' : '保存' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.filter-row {
  display: flex;
  gap: 16px;
  align-items: end;
  flex-wrap: wrap;
  margin: 16px 0 8px;
}
.filter-row label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: var(--text-secondary); }
.mono { font-family: var(--mono); font-size: 13px; }
.modal-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center; z-index: 100;
}
.modal { width: 420px; max-width: 92vw; }
.field { display: block; margin-bottom: 12px; font-size: 13px; color: var(--text-secondary); }
.field .input { display: block; width: 100%; margin-top: 4px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
</style>
