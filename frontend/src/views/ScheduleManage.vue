<script setup lang="ts">
import { computed, ref, watch, onMounted } from "vue";
import AppIcon from "../components/common/AppIcon.vue";
import { localDate, formatDateTime } from "../utils/presentation";
import { useCampus } from "../composables/useCampus";
import { authState } from "../stores/auth";
import type { Schedule, ScheduleType } from "../types/api";

const { state, isDemo, loadSchedules, createSchedule, updateSchedule, deleteSchedule } =
  useCampus();
const spaces = computed(() => state.resources);
const submitting = ref(false);
const loading = ref(false);
const error = ref("");
const date = ref(localDate());
const resourceFilter = ref("");

const formDialog = ref<HTMLDialogElement | null>(null);
const editingId = ref<string | null>(null);
const formResource = ref("");
const formType = ref<ScheduleType>("TEACHING");
const formDate = ref(localDate());
const formStart = ref("09:00");
const formEnd = ref("10:00");
const formNote = ref("");

const typeLabels: Record<ScheduleType, string> = {
  OPEN: "开放时间",
  TEACHING: "教学占用",
};
const hourOptions = Array.from({ length: 15 }, (_, i) => `${String(8 + i).padStart(2, "0")}:00`);

const canWrite = computed(
  () => isDemo || authState.user?.role === "RESOURCE_ADMIN",
);

const records = computed(() =>
  state.schedules.filter((item) => {
    const itemDate = item.startTime.slice(0, 10);
    if (itemDate !== date.value) return false;
    if (resourceFilter.value && item.resourceId !== resourceFilter.value) return false;
    return true;
  }),
);

async function refresh() {
  loading.value = true;
  error.value = "";
  await loadSchedules(date.value, resourceFilter.value || undefined);
  loading.value = false;
}

onMounted(refresh);
watch([date, resourceFilter], refresh);

function openCreate() {
  editingId.value = null;
  formResource.value = spaces.value[0]?.id ?? "";
  formType.value = "TEACHING";
  formDate.value = date.value;
  formStart.value = "09:00";
  formEnd.value = "10:00";
  formNote.value = "";
  error.value = "";
  formDialog.value?.showModal();
}

function openEdit(record: Schedule) {
  editingId.value = record.scheduleId;
  formResource.value = record.resourceId;
  formType.value = record.type;
  formDate.value = record.startTime.slice(0, 10);
  formStart.value = record.startTime.slice(11, 16);
  formEnd.value = record.endTime.slice(11, 16);
  formNote.value = record.note;
  error.value = "";
  formDialog.value?.showModal();
}

function closeDialog() {
  formDialog.value?.close();
}

function localIssue(): string {
  if (!canWrite.value) return "当前账号没有时段维护权限。";
  if (!formResource.value) return "请选择资源。";
  if (!formDate.value || !/^\d{4}-\d{2}-\d{2}$/.test(formDate.value))
    return "请选择有效日期。";
  if (formStart.value >= formEnd.value) return "结束时间必须晚于开始时间。";
  return "";
}

function buildTimes() {
  return {
    startTime: `${formDate.value}T${formStart.value}:00+08:00`,
    endTime: `${formDate.value}T${formEnd.value}:00+08:00`,
  };
}

async function submit() {
  error.value = localIssue();
  if (error.value) return;
  submitting.value = true;
  const { startTime, endTime } = buildTimes();
  try {
    const ok = editingId.value
      ? await updateSchedule(editingId.value, {
          type: formType.value,
          startTime,
          endTime,
          note: formNote.value,
        })
      : await createSchedule({
          resourceId: formResource.value,
          type: formType.value,
          startTime,
          endTime,
          note: formNote.value,
        });
    if (ok) {
      closeDialog();
      await refresh();
    } else {
      error.value = "保存未成功，请检查时段冲突或接口错误后重试。";
    }
  } finally {
    submitting.value = false;
  }
}

async function remove(record: Schedule) {
  if (!canWrite.value) return;
  if (!window.confirm(`确定删除「${record.resourceName}」的该条${typeLabels[record.type]}安排吗？`))
    return;
  const ok = await deleteSchedule(record.scheduleId);
  if (ok) await refresh();
}
</script>

<template>
  <section class="page-intro">
    <span class="eyebrow">MAKE ROOM FOR EVERYONE</span>
    <h1>有序安排，<span class="gradient-text">共享空间。</span></h1>
    <p>集中维护开放时间与教学占用，让空闲时间一目了然。</p>
  </section>
  <div class="section-heading">
    <div>
      <h2>占用时段管理</h2>
      <p class="muted">
        {{
          isDemo
            ? "管理员界面预览 · 无真实权限和数据库操作"
            : "仅资源管理员可维护开放时间与教学占用，冲突时段由后端校验"
        }}
      </p>
    </div>
    <button
      class="button primary"
      :disabled="!canWrite"
      @click="openCreate"
    >
      <AppIcon name="plus" :size="18" />新增时段安排
    </button>
  </div>
  <div class="admin-toolbar panel">
    <label>查看日期<input v-model="date" class="input" type="date" /></label>
    <label
      >预约资源<select v-model="resourceFilter" class="input">
        <option value="">全部资源</option>
        <option v-for="space in spaces" :key="space.id" :value="space.id">
          {{ space.name }}
        </option>
      </select></label
    >
    <div class="admin-tip">
      <AppIcon name="info" :size="17" />开放时间与教学占用会与系统预约联合判断可约时段。
    </div>
  </div>
  <div class="table-wrap panel">
    <table class="schedule-table">
      <thead>
        <tr>
          <th>资源 / 位置</th>
          <th>时段类型</th>
          <th>时段</th>
          <th>说明</th>
          <th v-if="canWrite">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="record in records" :key="record.scheduleId">
          <td>
            <strong>{{ record.resourceName }}</strong
            ><small>{{ record.resourceId }}</small>
          </td>
          <td>
            <span class="type-tag" :class="record.type.toLowerCase()">{{
              typeLabels[record.type]
            }}</span>
          </td>
          <td class="tabular">
            {{ formatDateTime(record.startTime) }}—{{ record.endTime.slice(11, 16) }}
          </td>
          <td>{{ record.note }}</td>
          <td v-if="canWrite" class="table-actions">
            <button class="button secondary" @click="openEdit(record)">编辑</button>
            <button class="button danger" @click="remove(record)">删除</button>
          </td>
        </tr>
        <tr v-if="!records.length">
          <td :colspan="canWrite ? 5 : 4" class="table-empty">
            所选日期与资源没有时段安排。可由资源管理员新增开放时间或教学占用。
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <section class="admin-rules">
    <div>
      <AppIcon name="classroom" :size="23" />
      <h3>教学优先</h3>
      <p>教务教学占用与系统预约共同决定教室可约时间。</p>
    </div>
    <div>
      <AppIcon name="sport" :size="23" />
      <h3>场馆统筹</h3>
      <p>体育场馆教学占用时段应在可约时段中排除。</p>
    </div>
    <div>
      <AppIcon name="shield" :size="23" />
      <h3>冲突校验</h3>
      <p>同一资源同类时段重叠时，后端拒绝保存。</p>
    </div>
  </section>
  <dialog ref="formDialog" class="modal" aria-labelledby="schedule-form-title">
    <form @submit.prevent="submit">
      <div class="modal-heading">
        <div>
          <span class="eyebrow">SCHEDULE PREVIEW</span>
          <h2 id="schedule-form-title">
            {{ editingId ? "编辑时段安排" : "新增时段安排" }}
          </h2>
        </div>
        <button
          type="button"
          class="icon-button"
          aria-label="关闭表单"
          @click="closeDialog"
        >
          <AppIcon name="close" />
        </button>
      </div>
      <p class="muted form-description">
        {{ isDemo ? "仅更新本次页面内的演示数据，刷新后重置。" : "保存后由后端校验权限、资源与时段冲突。" }}
      </p>
      <label class="form-field"
        >预约资源
        <select v-model="formResource" class="input" :disabled="!!editingId">
          <option v-for="space in spaces" :key="space.id" :value="space.id">
            {{ space.name }}
          </option>
        </select></label
      >
      <div class="form-columns">
        <label class="form-field"
          >日期<input
            v-model="formDate"
            type="date"
            class="input"
            required /></label
        ><label class="form-field"
          >时段类型<select
            v-model="formType"
            class="input"
            :disabled="!!editingId">
            <option value="OPEN">开放时间</option>
            <option value="TEACHING">教学占用</option>
          </select></label
        >
      </div>
      <div class="form-columns">
        <label class="form-field"
          >开始时间<select v-model="formStart" class="input">
            <option v-for="hour in hourOptions" :key="hour" :value="hour">
              {{ hour }}
            </option>
          </select></label
        ><label class="form-field"
          >结束时间<select v-model="formEnd" class="input">
            <option v-for="hour in hourOptions" :key="hour" :value="hour">
              {{ hour }}
            </option>
          </select></label
        >
      </div>
      <label class="form-field"
        >{{ formType === "TEACHING" ? "课程/占用说明" : "备注" }}<input
          v-model="formNote"
          class="input"
          maxlength="200"
          :placeholder="formType === 'TEACHING' ? '例如：软件工程课程、体育教学' : '例如：节假日调整、临时开放'"
      /></label>
      <p v-if="error" class="error-message" role="alert">{{ error }}</p>
      <div class="modal-actions">
        <button class="button secondary" type="button" @click="closeDialog">
          取消</button
        ><button
          class="button primary"
          type="submit"
          :disabled="submitting || !canWrite"
        >
          {{ editingId ? "保存修改" : "保存时段安排" }}
        </button>
      </div>
    </form>
  </dialog>
</template>
