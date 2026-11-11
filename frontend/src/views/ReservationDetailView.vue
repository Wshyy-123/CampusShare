<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { errorMessage } from "../api/http";
import { reservationsApi } from "../api/reservations";
import type { Reservation } from "../types/api";
import StatusBadge from "../components/StatusBadge.vue";

const route = useRoute();
const reservation = ref<Reservation | null>(null);
const loading = ref(true);
const error = ref("");

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

      <div class="alert alert-warning">
        取消、签到和签退属于 XIA-7/XIA-8；本页只展示 XIA-6 的状态查询结果。
      </div>
    </div>
  </section>
</template>
