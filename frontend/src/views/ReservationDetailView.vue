<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { errorMessage } from "../api/http";
import { reservationsApi } from "../api/reservations";
import type { Reservation } from "../types/api";
import StatusBadge from "../components/StatusBadge.vue";

const SPORTS_TYPES = ["BADMINTON", "TABLE_TENNIS", "TENNIS"];

const route = useRoute();
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
  if (!current || !isSports.value) return false;
  return (
    current.status === "RESERVED" &&
    Date.parse(current.startTime) > Date.now()
  );
});
const canCheckIn = computed(
  () =>
    isSports.value &&
    reservation.value !== null &&
    reservation.value.status === "RESERVED",
);
const canCheckOut = computed(
  () =>
    isSports.value &&
    reservation.value !== null &&
    reservation.value.status === "CHECKED_IN",
);

async function load() {
  loading.value = true;
  error.value = "";
  try {
    reservation.value = await reservationsApi.detail(String(route.params.id));
  } catch (exception) {
    error.value = errorMessage(exception);
  } finally {
    loading.value = false;
  }
}

async function refreshSilently() {
  try {
    reservation.value = await reservationsApi.detail(String(route.params.id));
  } catch {
    // 刷新失败时保留当前提示，不覆盖错误信息
  }
}

async function runAction(action: "cancel" | "check-in" | "check-out") {
  const current = reservation.value;
  if (!current) return;
  actionLoading.value = true;
  actionMessage.value = "";
  actionError.value = "";
  try {
    const updated =
      action === "cancel"
        ? await reservationsApi.cancel(current.reservationId)
        : action === "check-in"
          ? await reservationsApi.checkIn(current.reservationId)
          : await reservationsApi.checkOut(current.reservationId);
    reservation.value = updated;
    actionMessage.value =
      action === "cancel"
        ? "已取消，场地已释放"
        : action === "check-in"
          ? "签到成功，已记录到馆时间"
          : "签退成功，场地已释放";
  } catch (exception) {
    actionError.value = errorMessage(exception);
    await refreshSilently();
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

onMounted(load);
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

      <div v-if="isSports" class="card panel">
        <h2>体育场地操作</h2>
        <p class="small">
          使用前可取消；开始后 15 分钟内签到；使用结束签退。操作结果以服务端返回为准。
        </p>
        <div style="display: flex; gap: 12px; flex-wrap: wrap; margin: 12px 0">
          <button
            v-if="canCancel"
            class="btn"
            :disabled="actionLoading"
            @click="runAction('cancel')"
          >
            使用前取消
          </button>
          <button
            v-if="canCheckIn"
            class="btn btn-primary"
            :disabled="actionLoading"
            @click="runAction('check-in')"
          >
            扫码签到
          </button>
          <button
            v-if="canCheckOut"
            class="btn btn-primary"
            :disabled="actionLoading"
            @click="runAction('check-out')"
          >
            扫码签退
          </button>
        </div>
        <p v-if="actionMessage" class="alert alert-success">
          {{ actionMessage }}
        </p>
        <p v-if="actionError" class="alert alert-warning">{{ actionError }}</p>
      </div>

      <div class="alert alert-warning">
        签到和签退的扫码入口、其他资源类型的自动释放仍在对应工作项中接入；本页展示的是服务端真实状态。
      </div>
    </div>
  </section>
</template>
