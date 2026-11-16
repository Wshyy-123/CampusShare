<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from "vue";
import { useRoute } from "vue-router";
import { errorMessage } from "../api/http";
import { reservationsApi } from "../api/reservations";
import type { Reservation } from "../types/api";
import { useCampus } from "../composables/useCampus";
import LibraryCardPanel from "../components/LibraryCardPanel.vue";
import { authState } from "../stores/auth";
import PlaceCodeDialog from "../components/PlaceCodeDialog.vue";
import StatusBadge from "../components/StatusBadge.vue";

const SPORTS_TYPES = ["BADMINTON", "TABLE_TENNIS", "TENNIS"];

const route = useRoute();
const campus = useCampus();
const placeCode = ref<InstanceType<typeof PlaceCodeDialog> | null>(null);
const reservation = ref<Reservation | null>(null);
const loading = ref(true);
const error = ref("");
const actionLoading = ref(false);
const actionMessage = ref("");
const actionError = ref("");

const isSports = computed(
  () =>
    reservation.value !== null &&
    SPORTS_TYPES.includes(reservation.value.resourceType),
);
const canCancel = computed(() => {
  const current = reservation.value;
  if (!current || (!isSports.value && current.resourceType !== "CLASSROOM"))
    return false;
  return (
    current.status === "RESERVED" && Date.parse(current.startTime) > Date.now()
  );
});
let generation = 0;
async function load() {
  const version = ++generation;
  reservation.value = null;
  actionMessage.value = "";
  actionError.value = "";
  loading.value = true;
  error.value = "";
  try {
    const result = await reservationsApi.detail(String(route.params.id));
    if (version === generation) reservation.value = result;
  } catch (exception) {
    if (version === generation) error.value = errorMessage(exception);
  } finally {
    if (version === generation) loading.value = false;
  }
}

async function cancel() {
  const current = reservation.value;
  if (!current || actionLoading.value) return;
  const version = generation;
  actionLoading.value = true;
  actionMessage.value = "";
  actionError.value = "";
  try {
    const result = await campus.cancelReservation(current.reservationId);
    if (version !== generation) return;
    if (result) {
      reservation.value = result;
      actionMessage.value = "已取消，场地已释放";
    } else actionError.value = campus.state.error || "取消未成功";
  } finally {
    actionLoading.value = false;
  }
}

function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  return `${iso.slice(0, 10)} ${iso.slice(11, 16)}`;
}

function cancelReasonLabel(reason: string | null): string {
  if (reason === "USER_CANCEL") return "本人取消";
  if (reason === "NO_SHOW") return "未到超时";
  if (reason === "RESOURCE_DISABLED") return "资源停用";
  return reason ?? "—";
}

watch(
  () => [route.params.id, authState.user?.id],
  () => {
    void load();
  },
  { immediate: true },
);
onBeforeUnmount(() => {
  generation++;
});
</script>

<template>
  <section>
    <div class="page-head">
      <div>
        <p class="small">
          <RouterLink to="/bookings" class="back-link"
            >← 返回我的预约</RouterLink
          >
        </p>
        <h1>预约详情</h1>
        <p v-if="reservation">{{ reservation.resourceName }}</p>
      </div>
    </div>

    <div v-if="loading" class="card empty">正在加载预约详情…</div>
    <div v-else-if="error" class="card empty">
      <strong>无法查看这条预约</strong>
      <p class="small">{{ error }}</p>
      <RouterLink class="btn btn-primary" to="/bookings"
        >返回我的预约</RouterLink
      >
    </div>

    <div v-else-if="reservation" class="stack">
      <div class="card panel detail-head">
        <div>
          <span class="pill">{{ reservation.resourceType }}</span>
          <h2>{{ reservation.resourceName }}</h2>
          <p class="small">
            {{ reservation.location }} · {{ reservation.unitLabel }}
          </p>
        </div>
        <StatusBadge
          :status="reservation.status"
          :cancel-reason="reservation.cancelReason"
          :resource-type="reservation.resourceType"
        />
      </div>

      <div class="card panel">
        <h2>预约信息</h2>
        <dl class="detail-grid">
          <div class="detail-item">
            <dt>预约号</dt>
            <dd class="mono">#{{ reservation.reservationId }}</dd>
          </div>
          <div class="detail-item">
            <dt>资源编号</dt>
            <dd class="mono">#{{ reservation.resourceId }}</dd>
          </div>
          <div class="detail-item">
            <dt>使用时段</dt>
            <dd>
              {{ formatDateTime(reservation.startTime) }} –
              {{ formatDateTime(reservation.endTime) }}
            </dd>
          </div>
          <div class="detail-item">
            <dt>当前状态</dt>
            <dd>{{ reservation.status }}</dd>
          </div>
          <div class="detail-item">
            <dt>预约成功时间</dt>
            <dd>{{ formatDateTime(reservation.acceptedAt) }}</dd>
          </div>
          <div class="detail-item">
            <dt>最晚确认时间</dt>
            <dd>{{ formatDateTime(reservation.arrivalDeadline) }}</dd>
          </div>
          <div class="detail-item">
            <dt>确认到馆时间</dt>
            <dd>{{ formatDateTime(reservation.checkInAt) }}</dd>
          </div>
          <div class="detail-item">
            <dt>签退时间</dt>
            <dd>{{ formatDateTime(reservation.checkOutAt) }}</dd>
          </div>
          <div v-if="reservation.status === 'CANCELLED'" class="detail-item">
            <dt>取消原因</dt>
            <dd>{{ cancelReasonLabel(reservation.cancelReason) }}</dd>
          </div>
          <div class="detail-item">
            <dt>创建时间</dt>
            <dd>{{ formatDateTime(reservation.createdAt) }}</dd>
          </div>
        </dl>
      </div>

      <LibraryCardPanel
        v-if="reservation.resourceType === 'LIBRARY_SEAT'"
        :reservation="reservation"
        @updated="reservation = $event"
      />
      <div
        v-if="isSports || reservation.resourceType === 'CLASSROOM'"
        class="card panel"
      >
        <h2>{{ isSports ? "体育场地操作" : "教室操作" }}</h2>
        <p class="small">
          使用前可取消；开始后 15
          分钟内签到；已签到后到预约结束自动完成。操作结果以服务端返回为准。
        </p>
        <div style="display: flex; gap: 12px; flex-wrap: wrap; margin: 12px 0">
          <button
            v-if="canCancel"
            class="btn"
            :disabled="actionLoading"
            @click="cancel"
          >
            使用前取消
          </button>
          <button
            class="button secondary"
            @click="
              placeCode?.open(
                reservation.resourceId,
                reservation.resourceName,
                reservation.location,
              )
            "
          >
            查看地点二维码
          </button>
          <p class="small">
            到场后使用手机扫描现场地点码签到。体育预约结束后可再次扫描并选择记录签退。
          </p>
        </div>
        <p v-if="actionMessage" class="alert alert-success">
          {{ actionMessage }}
        </p>
        <p v-if="actionError" class="alert alert-warning">{{ actionError }}</p>
      </div>

      <div
        v-if="isSports || reservation.resourceType === 'CLASSROOM'"
        class="alert alert-warning"
      >
        教室和体育到期由后台自动释放；体育签退单独记录，不依赖签退才能释放。
      </div>
    </div>
    <PlaceCodeDialog ref="placeCode" />
  </section>
</template>
