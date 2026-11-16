<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from "vue";
import type { Reservation } from "../types/api";
import { useCampus } from "../composables/useCampus";
import { authState } from "../stores/auth";
const props = defineProps<{ reservation: Reservation }>();
const emit = defineEmits<{ updated: [Reservation] }>();
const campus = useCampus();
const busy = ref(false);
const message = ref("");
const error = ref("");
let generation = 0;
watch([() => props.reservation.reservationId, () => authState.user?.id], () => {
  generation++;
  message.value = "";
  error.value = "";
});
onBeforeUnmount(() => {
  generation++;
});
async function swipe(direction: "enter" | "leave") {
  if (busy.value || campus.isDemo) return;
  const version = generation;
  const current = props.reservation;
  busy.value = true;
  message.value = "";
  error.value = "";
  try {
    const payload = {
      resourceId: current.resourceId,
      source: "CARD_SIMULATION" as const,
    };
    const result =
      direction === "enter"
        ? await campus.enterLibrary(current.reservationId, payload)
        : await campus.leaveLibrary(current.reservationId, payload);
    if (version !== generation) return;
    if (result) {
      emit("updated", result);
      message.value =
        direction === "enter"
          ? "刷卡入场成功，已登记到馆时间。"
          : "刷卡离场成功，本次预约已结束，座位已释放。";
    } else error.value = campus.state.error || "刷卡未成功，请重新核验。";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section class="card panel" aria-live="polite">
    <h2>图书馆模拟刷卡</h2>
    <p class="small">
      {{ reservation.resourceName }} · {{ reservation.unitLabel }}
    </p>
    <p class="small">
      入场需在预约成功起30分钟内完成；座位不可主动取消。入场和离场分别操作，离场立即结束本次预约并释放座位。
    </p>
    <p v-if="campus.isDemo" class="small">
      模拟刷卡需要连接后端，演示数据模式不会登记到馆。
    </p>
    <div class="card-actions">
      <button
        v-if="reservation.status === 'RESERVED'"
        class="button primary"
        :disabled="busy || campus.isDemo"
        @click="swipe('enter')"
      >
        模拟刷卡入场
      </button>
      <button
        v-if="reservation.status === 'CHECKED_IN'"
        class="button primary"
        :disabled="busy || campus.isDemo"
        @click="swipe('leave')"
      >
        模拟刷卡离场并释放
      </button>
      <p v-if="reservation.status === 'CHECKED_IN'" class="small">
        已登记入场，重复入场不会自动离场。
      </p>
      <p v-if="reservation.status === 'COMPLETED'" class="small">
        本次使用已结束。
      </p>
      <p v-if="reservation.status === 'CANCELLED'" class="small">
        预约已取消，刷卡不能恢复。
      </p>
    </div>
    <p v-if="message" class="alert alert-success" role="status">
      {{ message }}
    </p>
    <p v-if="error" class="alert alert-warning" role="alert">{{ error }}</p>
  </section>
</template>
<style scoped>
.card-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}
</style>
