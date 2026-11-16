<script setup lang="ts">
import { computed, ref } from "vue";
import AppIcon from "./common/AppIcon.vue";
import { useCampus } from "../composables/useCampus";
import type { DisableResourceResult } from "../services/types";

const { state, isDemo, disableResource } = useCampus();
const emit = defineEmits<{ disabled: [result: DisableResourceResult, date: string] }>();
const dialog = ref<HTMLDialogElement | null>(null);
const resourceId = ref("");
const start = ref("");
const end = ref("");
const reason = ref("");
const error = ref("");
const submitting = ref(false);
const availableResources = computed(() => state.resources);

function open(date: string, selectedResource = "") {
  if (isDemo || !state.ready) return;
  resourceId.value = selectedResource || state.resources[0]?.id || "";
  start.value = `${date}T09:00`;
  end.value = `${date}T10:00`;
  reason.value = "";
  error.value = "";
  dialog.value?.showModal();
}
defineExpose({ open });

async function submit() {
  if (submitting.value) return;
  error.value = "";
  const startTime = `${start.value}:00+08:00`;
  const endTime = `${end.value}:00+08:00`;
  if (!availableResources.value.some((r) => r.id === resourceId.value)
      || !Number.isFinite(Date.parse(startTime)) || !Number.isFinite(Date.parse(endTime))
      || Date.parse(startTime) >= Date.parse(endTime) || !reason.value.trim()) {
    error.value = "请选择资源，填写停用原因，并确保结束时间晚于开始时间。";
    return;
  }
  submitting.value = true;
  try {
    const result = await disableResource(resourceId.value, startTime, endTime, reason.value);
    if (result) {
      emit("disabled", result, start.value.slice(0, 10));
      dialog.value?.close();
    } else {
      error.value = state.error || "停用未成功，请检查后重试。";
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <dialog ref="dialog" class="modal" aria-labelledby="disable-title"
    @cancel="submitting && $event.preventDefault()">
    <form @submit.prevent="submit">
      <div class="modal-heading">
        <div><span class="eyebrow">RESOURCE AVAILABILITY</span><h2 id="disable-title">停用场地</h2></div>
        <button type="button" class="icon-button" aria-label="关闭停用表单" :disabled="submitting" @click="dialog?.close()">
          <AppIcon name="close" />
        </button>
      </div>
      <p class="muted form-description">提交后，系统将取消停用范围内尚未开始使用的预约并保留记录。若存在正在使用的预约，整次停用会被拒绝。</p>
      <fieldset :disabled="submitting" class="disable-fields">
        <label class="form-field">预约资源
          <select v-model="resourceId" class="input" required>
            <option v-for="space in availableResources" :key="space.id" :value="space.id">{{ space.name }} · {{ space.location }}</option>
          </select>
        </label>
        <div class="form-columns">
          <label class="form-field">开始时间（北京时间）<input v-model="start" type="datetime-local" class="input" required /></label>
          <label class="form-field">结束时间（北京时间）<input v-model="end" type="datetime-local" class="input" required /></label>
        </div>
        <label class="form-field">停用原因<textarea v-model="reason" class="input" rows="3" maxlength="500" required placeholder="例如：地板检修，暂停使用" /></label>
      </fieldset>
      <p v-if="error" class="error-message" role="alert">{{ error }}</p>
      <div class="modal-actions">
        <button type="button" class="button secondary" :disabled="submitting" @click="dialog?.close()">取消</button>
        <button type="submit" class="button primary" :disabled="submitting || !state.ready || isDemo">
          {{ submitting ? "正在处理…" : "确认停用并处理预约" }}
        </button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.disable-fields { border: 0; padding: 0; margin: 0; min-width: 0; }
.disable-fields textarea { resize: vertical; }
.disable-fields .form-columns { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.disable-fields .form-field { min-width: 0; }
.disable-fields .input { min-width: 0; max-width: 100%; }
@media (max-width: 600px) {
  .disable-fields .form-columns { grid-template-columns: minmax(0, 1fr); }
}
</style>
