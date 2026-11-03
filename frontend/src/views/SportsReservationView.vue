<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiError, getDebugUserId, setDebugUserId as persistDebugUserId } from '../api/http'
import {
  cancelReservation,
  checkInReservation,
  checkOutReservation,
  getReservation,
} from '../api/reservations'
import type { ApiEnvelope, ReservationDetail } from '../types/reservation'

const reservation = ref<ReservationDetail | null>(null)
const reservationIdInput = ref('')
const loading = ref(false)
const hint = ref('')
const message = ref('')
const messageKind = ref<'success' | 'error' | 'info'>('info')
const requestId = ref('')
const debugUserId = ref(getDebugUserId())

const STATUS_LABELS: Record<string, string> = {
  RESERVED: '已预约',
  CHECKED_IN: '已签到',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const REASON_LABELS: Record<string, string> = {
  USER_CANCEL: '用户取消',
  NO_SHOW: '超时未到',
  RESOURCE_DISABLED: '资源停用',
}

const showCancel = computed(() => {
  const current = reservation.value
  return current !== null && current.status === 'RESERVED' && Date.parse(current.startTime) > Date.now()
})
const showCheckIn = computed(() => reservation.value?.status === 'RESERVED')
const showCheckOut = computed(() => reservation.value?.status === 'CHECKED_IN')

onMounted(() => {
  const fromUrl = new URLSearchParams(window.location.search).get('reservationId')
  if (fromUrl) {
    reservationIdInput.value = fromUrl
    void loadReservation(fromUrl)
  }
})

async function openReservation(rawId: string) {
  const normalized = rawId.trim()
  if (!/^[1-9]\d*$/.test(normalized)) {
    hint.value = '请输入正确的预约号（正整数）'
    return
  }
  hint.value = ''
  await loadReservation(normalized)
}

async function loadReservation(reservationId: string) {
  loading.value = true
  try {
    apply(await getReservation(reservationId))
    message.value = ''
    messageKind.value = 'info'
  } catch (error) {
    handleError(error)
  } finally {
    loading.value = false
  }
}

async function refresh() {
  if (reservation.value) {
    await loadReservation(reservation.value.reservationId)
  }
}

async function runAction(action: 'cancel' | 'check-in' | 'check-out') {
  const current = reservation.value
  if (!current) {
    return
  }
  loading.value = true
  try {
    const envelope =
      action === 'cancel'
        ? await cancelReservation(current.reservationId)
        : action === 'check-in'
          ? await checkInReservation(current.reservationId)
          : await checkOutReservation(current.reservationId)
    apply(envelope)
    message.value = envelope.message
    messageKind.value = 'success'
  } catch (error) {
    handleError(error)
    await refreshSilently(current.reservationId)
  } finally {
    loading.value = false
  }
}

async function refreshSilently(reservationId: string) {
  try {
    apply(await getReservation(reservationId))
  } catch {
    // 刷新失败不覆盖当前错误提示
  }
}

function apply(envelope: ApiEnvelope<ReservationDetail>) {
  reservation.value = envelope.data
  requestId.value = envelope.requestId
}

function handleError(error: unknown) {
  messageKind.value = 'error'
  if (error instanceof ApiError) {
    message.value = error.code === 'NETWORK_ERROR' ? error.message : `${error.message}（${error.code}）`
    requestId.value = error.requestId || requestId.value
    return
  }
  message.value = '发生未知错误，请稍后重试'
}

function saveDebugUserId() {
  persistDebugUserId(debugUserId.value)
}

function statusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status
}

function reasonLabel(reason: string): string {
  return REASON_LABELS[reason] ?? reason
}

function formatTime(value: string | null): string {
  return value ? value.slice(5, 16).replace('T', ' ') : '—'
}
</script>

<template>
  <div class="glass-card">
    <header class="head">
      <h1 class="title">体育场地</h1>
      <p class="subtitle">使用前取消 · 扫码签到 · 结束签退</p>
    </header>

    <form class="lookup" @submit.prevent="openReservation(reservationIdInput)">
      <input
        v-model="reservationIdInput"
        class="lookup-input"
        inputmode="numeric"
        placeholder="预约号，例如 4"
        aria-label="预约号"
      />
      <button class="lookup-button" type="submit" :disabled="loading">打开</button>
    </form>
    <p v-if="hint" class="hint">{{ hint }}</p>

    <section v-if="reservation" class="detail">
      <div class="detail-head">
        <span class="badge" :class="`badge-${reservation.status.toLowerCase()}`">
          {{ statusLabel(reservation.status) }}
        </span>
        <span class="reservation-id">#{{ reservation.reservationId }}</span>
      </div>

      <dl class="rows">
        <div class="row">
          <dt>资源</dt>
          <dd>{{ reservation.resourceType ?? '未知类型' }} · {{ reservation.resourceId }} 号场地</dd>
        </div>
        <div class="row">
          <dt>时段</dt>
          <dd>{{ formatTime(reservation.startTime) }} — {{ formatTime(reservation.endTime) }}</dd>
        </div>
        <div class="row">
          <dt>签到时间</dt>
          <dd>{{ formatTime(reservation.checkInAt) }}</dd>
        </div>
        <div class="row">
          <dt>签退时间</dt>
          <dd>{{ formatTime(reservation.checkOutAt) }}</dd>
        </div>
        <div v-if="reservation.cancelReason" class="row">
          <dt>取消原因</dt>
          <dd>{{ reasonLabel(reservation.cancelReason) }}</dd>
        </div>
      </dl>

      <div class="actions">
        <button v-if="showCancel" class="action cancel" :disabled="loading" @click="runAction('cancel')">
          使用前取消
        </button>
        <button v-if="showCheckIn" class="action check-in" :disabled="loading" @click="runAction('check-in')">
          扫码签到
        </button>
        <button v-if="showCheckOut" class="action check-out" :disabled="loading" @click="runAction('check-out')">
          扫码签退
        </button>
        <button class="action ghost" :disabled="loading" @click="refresh">刷新状态</button>
      </div>

      <p v-if="message" class="message" :class="`message-${messageKind}`">{{ message }}</p>
      <p v-if="requestId" class="request-id">requestId: {{ requestId }}</p>
    </section>

    <section v-else class="empty">
      <p>扫描场地二维码，或输入预约号查看详情。</p>
      <p class="empty-note">已取消或已完成的预约也能查看结果。</p>
    </section>

    <footer class="debug">
      <label>
        调试用户ID
        <input v-model="debugUserId" inputmode="numeric" @change="saveDebugUserId" />
      </label>
      <span>仅开发环境生效</span>
    </footer>
  </div>
</template>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(16px);
  padding: 28px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}
.head {
  margin-bottom: 20px;
}
.title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 6px;
  color: #e2e2e2;
}
.subtitle {
  margin: 0;
  font-size: 13px;
  color: #8b8b94;
}
.lookup {
  display: flex;
  gap: 10px;
}
.lookup-input {
  flex: 1;
  min-width: 0;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(0, 0, 0, 0.3);
  color: #f4f4f5;
  font-size: 16px;
}
.lookup-input:focus {
  outline: none;
  border-color: rgba(66, 184, 131, 0.7);
}
.lookup-button {
  padding: 12px 20px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #42b883, #347474);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.lookup-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.hint {
  margin: 10px 0 0;
  font-size: 13px;
  color: #f0b429;
}
.detail {
  margin-top: 22px;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.badge {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid transparent;
}
.badge-reserved {
  color: #7cc4ff;
  background: rgba(56, 139, 253, 0.15);
  border-color: rgba(56, 139, 253, 0.35);
}
.badge-checked_in {
  color: #6ee7a8;
  background: rgba(66, 184, 131, 0.16);
  border-color: rgba(66, 184, 131, 0.4);
}
.badge-completed {
  color: #c4b5fd;
  background: rgba(139, 92, 246, 0.16);
  border-color: rgba(139, 92, 246, 0.35);
}
.badge-cancelled {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(239, 68, 68, 0.35);
}
.reservation-id {
  font-size: 13px;
  color: #6b6b74;
}
.rows {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 14px;
}
.row dt {
  color: #8b8b94;
  flex-shrink: 0;
}
.row dd {
  margin: 0;
  color: #e4e4e7;
  text-align: right;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}
.action {
  flex: 1 1 140px;
  padding: 12px;
  border-radius: 12px;
  border: none;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease, filter 0.15s ease;
}
.action:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.1);
}
.action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cancel {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
}
.check-in {
  background: linear-gradient(135deg, #42b883, #2f7d5b);
}
.check-out {
  background: linear-gradient(135deg, #6366f1, #4338ca);
}
.ghost {
  background: rgba(255, 255, 255, 0.08);
  color: #d4d4d8;
}
.message {
  margin: 16px 0 0;
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
}
.message-success {
  color: #6ee7a8;
  background: rgba(66, 184, 131, 0.12);
}
.message-error {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.12);
}
.message-info {
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.05);
}
.request-id {
  margin: 8px 0 0;
  font-size: 11px;
  color: #55555e;
  word-break: break-all;
}
.empty {
  margin-top: 22px;
  padding: 18px;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.22);
  color: #a1a1aa;
  font-size: 14px;
  line-height: 1.6;
}
.empty-note {
  margin: 6px 0 0;
  font-size: 12px;
  color: #6b6b74;
}
.debug {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: #6b6b74;
}
.debug input {
  width: 64px;
  margin-left: 6px;
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(0, 0, 0, 0.3);
  color: #e4e4e7;
  font-size: 13px;
}
</style>
