<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from "vue";
import { useRoute, RouterLink } from "vue-router";
import { placesApi } from "../api/places";
import { errorMessage } from "../api/http";
import { resourceToSpace, reservationToBooking } from "../api/campusApi";
import { useCampus } from "../composables/useCampus";
import { authState } from "../stores/auth";
import type { PlaceAttendance } from "../types/api";
import StatusBadge from "../components/StatusBadge.vue";
const route = useRoute();
const campus = useCampus();
const value = ref<PlaceAttendance | null>(null);
const loading = ref(false);
const writing = ref(false);
const error = ref("");
const notice = ref("");
const serverNow = ref(Date.now());
let offset = 0;
let generation = 0;
let attempted = "";
let timer: ReturnType<typeof setInterval> | undefined;
const booking = computed(() => value.value?.reservation ?? null);
const canSignOut = computed(
  () =>
    !!booking.value &&
    !!value.value &&
    ["BADMINTON", "TABLE_TENNIS", "TENNIS"].includes(
      value.value.place.resourceType,
    ) &&
    ["CHECKED_IN", "COMPLETED"].includes(booking.value.status) &&
    !!booking.value.checkInAt &&
    !booking.value.checkOutAt &&
    serverNow.value >= Date.parse(booking.value.endTime),
);
async function load() {
  if (campus.isDemo) {
    error.value = "地点签到需要连接后端；演示模式不会登记到场。";
    return;
  }
  const version = ++generation;
  const owner = authState.user?.id;
  loading.value = true;
  error.value = "";
  try {
    const result = await placesApi.get(
      String(route.params.id),
      typeof route.query.reservationId === "string"
        ? route.query.reservationId
        : undefined,
    );
    if (version !== generation || owner !== authState.user?.id) return;
    resourceToSpace(result.place);
    if (result.place.resourceId !== String(route.params.id))
      throw new Error("地点信息不匹配");
    if (result.reservation) reservationToBooking(result.reservation);
    if (!Number.isFinite(Date.parse(result.serverTime)))
      throw new Error("无法核对签到时间");
    value.value = result;
    offset = Date.parse(result.serverTime) - Date.now();
    serverNow.value = Date.now() + offset;
    await autoCheckIn();
  } catch (e) {
    if (version === generation) error.value = errorMessage(e);
  } finally {
    if (version === generation) loading.value = false;
  }
}
async function autoCheckIn() {
  if (
    campus.isDemo ||
    writing.value ||
    !value.value ||
    !booking.value ||
    document.visibilityState !== "visible" ||
    window.top !== window.self
  )
    return;
  const current = booking.value;
  if (current.status === "CHECKED_IN") {
    notice.value = "你已签到。重复扫码不会转为签退。";
    return;
  }
  if (current.status !== "RESERVED") return;
  const now = Date.now() + offset;
  if (
    now < Date.parse(current.startTime) ||
    now >=
      Math.min(
        Date.parse(current.endTime),
        Date.parse(current.startTime) + 15 * 60000,
      )
  )
    return;
  const key = `${authState.user?.id}/${current.reservationId}`;
  if (attempted === key) return;
  attempted = key;
  writing.value = true;
  const version = generation;
  try {
    const updated = await campus.checkInAtPlace(current.reservationId, {
      resourceId: value.value.place.resourceId,
      source: "QR",
    });
    if (version !== generation) return;
    if (updated) {
      value.value.reservation = updated;
      notice.value = "签到成功，地点与到场时间已登记。";
    } else error.value = campus.state.error || "签到未成功，请重新核验。";
  } finally {
    writing.value = false;
  }
}
async function signOut() {
  if (!canSignOut.value || !booking.value || !value.value || writing.value)
    return;
  const version = generation;
  writing.value = true;
  error.value = "";
  try {
    const updated = await campus.checkOutAtPlace(booking.value.reservationId, {
      resourceId: value.value.place.resourceId,
      source: "QR",
    });
    if (version !== generation) return;
    if (updated) {
      value.value.reservation = updated;
      notice.value = "已记录签退时间。预约结束后的释放不依赖这次签退。";
    } else error.value = campus.state.error || "签退未成功，请重试。";
  } finally {
    writing.value = false;
  }
}
function retry() {
  attempted = "";
  void load();
}
function visible() {
  if (document.visibilityState === "visible") void autoCheckIn();
}
onMounted(() => {
  void load();
  timer = setInterval(() => {
    serverNow.value = Date.now() + offset;
  }, 1000);
  document.addEventListener("visibilitychange", visible);
});
watch(
  () => route.fullPath,
  () => {
    attempted = "";
    value.value = null;
    notice.value = "";
    void load();
  },
);
onBeforeUnmount(() => {
  generation++;
  if (timer) clearInterval(timer);
  document.removeEventListener("visibilitychange", visible);
});
</script>
<template>
  <section class="page-intro">
    <span class="eyebrow">ARRIVE AT YOUR PLACE</span>
    <h1>地点<span class="gradient-text">签到</span></h1>
    <p>扫描具体教室、球桌或场地的地点码，核验本人的有效预约。</p>
  </section>
  <section class="panel attendance-panel" aria-live="polite">
    <p v-if="loading">正在核对地点和预约…</p>
    <template v-if="value">
      <h2>{{ value.place.name }}</h2>
      <p class="muted">
        {{ value.place.location }} · {{ value.place.unitLabel }}
      </p>
      <p>当前账户：{{ authState.user?.displayName }}</p>
      <template v-if="booking">
        <StatusBadge
          :status="booking.status"
          :cancel-reason="booking.cancelReason"
          :resource-type="booking.resourceType"
        />
        <p>
          {{ booking.startTime.slice(0, 16).replace("T", " ") }} —
          {{ booking.endTime.slice(0, 16).replace("T", " ") }}
        </p>
        <p v-if="booking.checkInAt">签到：{{ booking.checkInAt }}</p>
        <p v-if="booking.checkOutAt">签退：{{ booking.checkOutAt }}</p>
        <p
          v-if="
            booking.status === 'RESERVED' &&
            serverNow >= Date.parse(booking.endTime)
          "
        >
          预约已经结束，不能再签到。
        </p>
        <p
          v-else-if="
            booking.status === 'RESERVED' &&
            serverNow < Date.parse(booking.startTime)
          "
        >
          预约尚未开始。到开始时间后可重新核验。
        </p>
        <p
          v-else-if="
            booking.status === 'RESERVED' &&
            serverNow >= Date.parse(booking.startTime) + 15 * 60000
          "
        >
          签到期限已过，请查看预约处理结果。
        </p>
        <button
          v-if="canSignOut"
          class="button primary"
          :disabled="writing"
          @click="signOut"
        >
          记录签退
        </button>
        <RouterLink
          class="button secondary"
          :to="`/bookings/${booking.reservationId}`"
          >预约详情</RouterLink
        >
      </template>
      <p v-else>
        当前账户没有该地点可签到或待签退的预约，请核对账户、地点和预约时间。
      </p>
    </template>
    <p v-if="notice" class="success-message" role="status">{{ notice }}</p>
    <p v-if="error" class="error-message" role="alert">{{ error }}</p>
    <div class="modal-actions">
      <button
        class="button secondary"
        :disabled="loading || writing"
        @click="retry"
      >
        重新核验</button
      ><RouterLink class="button secondary" to="/bookings">我的预约</RouterLink>
    </div>
  </section>
</template>
<style scoped>
.attendance-panel {
  padding: 24px;
  max-width: 760px;
}
.attendance-panel p {
  line-height: 1.8;
  overflow-wrap: anywhere;
}
.attendance-panel .button {
  margin: 8px 8px 8px 0;
}
.success-message {
  color: var(--success, #73e0b5);
}
</style>
