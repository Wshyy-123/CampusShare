<script setup lang="ts">
import { computed, ref, watch, onMounted } from "vue";
import AppIcon from "../components/common/AppIcon.vue";
import ResourceDisableDialog from "../components/ResourceDisableDialog.vue";
import type { DisableResourceResult } from "../services/types";
import { localDate, formatHour } from "../utils/presentation";
import { useCampus } from "../composables/useCampus";
import type { Occupancy } from "../types/campus";

const { state, isDemo, addOccupancy, loadAvailability } = useCampus();
const spaces = computed(() => state.resources);
const disableDialog = ref<InstanceType<typeof ResourceDisableDialog> | null>(null);
const disableResult = ref<DisableResourceResult | null>(null);
function onDisabled(result: DisableResourceResult, selectedDate: string) {
  disableResult.value = result;
  date.value = selectedDate;
  resourceFilter.value = result.resourceId;
}
const submitting = ref(false);
const date = ref(localDate());
const resourceFilter = ref("");
const formDialog = ref<HTMLDialogElement | null>(null);
const formResource = ref("");
const formDate = ref(localDate());
const formStart = ref(12);
const formEnd = ref(14);
const formKind = ref<Occupancy["kind"]>("TEACHING");
const note = ref("");
const error = ref("");
const labels = {
  TEACHING: "教学占用",
  EXTERNAL: "外部占用",
  MAINTENANCE: "停用维护",
};
const records = computed(() =>
  state.occupancy
    .filter(
      (p) =>
        p.date === date.value &&
        (!resourceFilter.value || p.resourceId === resourceFilter.value),
    )
    .map((p) => ({
      ...p,
      space: spaces.value.find((s) => s.id === p.resourceId)!,
    })),
);
async function refreshOccupancy() {
  if (!isDemo)
    await Promise.all(
      spaces.value.map((space) => loadAvailability(space.id, date.value)),
    );
}
onMounted(refreshOccupancy);
watch([date, () => state.resources], refreshOccupancy);
function openForm() {
  formResource.value =
    spaces.value.find((space) => space.kind === "CLASSROOM" && !space.disabled)
      ?.id ??
    spaces.value[0]?.id ??
    "";
  formDate.value = date.value;
  error.value = "";
  formDialog.value?.showModal();
}
async function submit() {
  error.value = "";
  if (!spaces.value.some((space) => space.id === formResource.value)) {
    error.value = "请选择当前列表中的资源。";
    return;
  }
  if (formStart.value >= formEnd.value) {
    error.value = "结束时间需要晚于开始时间。";
    return;
  }
  submitting.value = true;
  try {
    if (
      await addOccupancy(
        formResource.value,
        formDate.value,
        formStart.value,
        formEnd.value,
        formKind.value,
        note.value,
      )
    ) {
      date.value = formDate.value;
      formDialog.value?.close();
      note.value = "";
    } else error.value = "添加未成功，请检查冲突或接口错误后重试。";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="page-intro">
    <span class="eyebrow">MAKE ROOM FOR EVERYONE</span>
    <h1>有序安排，<span class="gradient-text">共享空间。</span></h1>
    <p>将教学与场馆占用安排集中展示，让空闲时间一目了然。</p>
  </section>
  <div class="section-heading">
    <div>
      <h2>占用时段管理</h2>
      <p class="muted">
        {{
          isDemo
            ? "管理员界面预览 · 无真实权限和数据库操作"
            : "设置停用范围后，系统自动取消受影响且尚未开始使用的预约"
        }}
      </p>
    </div>
    <div class="inline-actions">
    <button class="button primary" :disabled="isDemo || !state.ready" @click="disableDialog?.open(date, resourceFilter)">
      <AppIcon name="shield" :size="18" />停用场地
    </button>
    <button
      class="button primary"
      :disabled="!state.ready || !state.scheduleWritable"
      @click="openForm"
    >
      <AppIcon name="plus" :size="18" />新增模拟占用
    </button>
    </div>
  </div>
  <p v-if="disableResult" class="panel form-description disable-result" role="status">
    停用已生效，自动取消 {{ disableResult.cancelledReservationIds.length }} 条受影响预约；取消原因及历史已保留。
  </p>
  <div class="admin-toolbar panel">
    <label>查看日期<input v-model="date" class="input" type="date" /></label
    ><label
      >预约资源<select v-model="resourceFilter" class="input">
        <option value="">全部资源</option>
        <option v-for="space in spaces" :key="space.id" :value="space.id">
          {{ space.name }}
        </option>
      </select></label
    >
    <div class="admin-tip">
      <AppIcon name="info" :size="17" />{{ isDemo ? "模拟占用会同步到资源详情时间轴。" : "停用时段持续不可预约；解除预约占用不会重新开放场地。" }}
    </div>
  </div>
  <div class="table-wrap panel">
    <table class="schedule-table">
      <thead>
        <tr>
          <th>资源 / 位置</th>
          <th>占用类型</th>
          <th>占用时段</th>
          <th>说明</th>
          <th>数据来源</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="record in records" :key="record.id">
          <td>
            <strong>{{ record.space.name }}</strong
            ><small>{{ record.space.code }}</small>
          </td>
          <td>
            <span class="type-tag" :class="record.kind.toLowerCase()">{{
              labels[record.kind]
            }}</span>
          </td>
          <td class="tabular">
            {{ formatHour(record.start) }}—{{ formatHour(record.end) }}
          </td>
          <td>{{ record.note }}</td>
          <td>
            <span class="muted">{{ isDemo ? "本地演示" : "主后端" }}</span>
          </td>
        </tr>
        <tr v-if="!records.length">
          <td colspan="5" class="table-empty">
            {{ isDemo ? "所选日期与资源没有模拟占用。可添加一条，返回资源详情查看变化。" : "所选日期与资源没有占用安排。" }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <section class="admin-rules">
    <div>
      <AppIcon name="classroom" :size="23" />
      <h3>教学优先</h3>
      <p>教务模拟占用与系统预约共同决定教室可约时间。</p>
    </div>
    <div>
      <AppIcon name="sport" :size="23" />
      <h3>场馆统筹</h3>
      <p>体育场馆外部占用、教学和维护时段都应排除。</p>
    </div>
    <div>
      <AppIcon name="shield" :size="23" />
      <h3>保留历史</h3>
      <p>释放一条预约不清除课表、维护或其他人的预约。</p>
    </div>
  </section>
  <ResourceDisableDialog ref="disableDialog" @disabled="onDisabled" />
  <dialog ref="formDialog" class="modal" aria-labelledby="schedule-form-title">
    <form @submit.prevent="submit">
      <div class="modal-heading">
        <div>
          <span class="eyebrow">SCHEDULE PREVIEW</span>
          <h2 id="schedule-form-title">新增模拟占用</h2>
        </div>
        <button
          type="button"
          class="icon-button"
          aria-label="关闭表单"
          @click="formDialog?.close()"
        >
          <AppIcon name="close" />
        </button>
      </div>
      <p class="muted form-description">
        仅更新本次页面内的演示数据，刷新后重置。
      </p>
      <label class="form-field"
        >预约资源<select v-model="formResource" class="input">
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
          >占用类型<select v-model="formKind" class="input">
            <option value="TEACHING">教学占用</option>
            <option value="EXTERNAL">外部占用</option>
            <option value="MAINTENANCE">停用维护</option>
          </select></label
        >
      </div>
      <div class="form-columns">
        <label class="form-field"
          >开始时间<select v-model.number="formStart" class="input">
            <option v-for="hour in 14" :key="hour" :value="hour + 7">
              {{ formatHour(hour + 7) }}
            </option>
          </select></label
        ><label class="form-field"
          >结束时间<select v-model.number="formEnd" class="input">
            <option v-for="hour in 14" :key="hour" :value="hour + 8">
              {{ formatHour(hour + 8) }}
            </option>
          </select></label
        >
      </div>
      <label class="form-field"
        >备注<input
          v-model="note"
          class="input"
          maxlength="100"
          placeholder="例如：模拟课表、社团训练或设备检修"
      /></label>
      <p v-if="error" class="error-message" role="alert">{{ error }}</p>
      <div class="modal-actions">
        <button
          class="button secondary"
          type="button"
          @click="formDialog?.close()"
        >
          取消</button
        ><button
          class="button primary"
          type="submit"
          :disabled="submitting || !state.ready || !state.scheduleWritable"
        >
          添加演示占用
        </button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.disable-result { padding: 14px 18px; line-height: 1.7; }
</style>
