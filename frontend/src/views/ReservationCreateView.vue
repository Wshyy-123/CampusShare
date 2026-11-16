<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from "vue";
import { useRoute } from "vue-router";
import { errorMessage, HttpError } from "../api/http";
import { reservationsApi } from "../api/reservations";
import { resourcesApi } from "../api/resources";
import type { Availability, Reservation } from "../types/api";
import StatusBadge from "../components/StatusBadge.vue";
import { dateAfter, localDate } from "../utils/presentation";
import { useCampus } from "../composables/useCampus";
const campus = useCampus();
let loadSequence = 0;
onBeforeUnmount(() => {
  loadSequence++;
});

const route = useRoute();
const resourceId = String(route.params.id);
const availability = ref<Availability | null>(null);
const loading = ref(true);
const loadError = ref("");
const date = ref(dateAfter(1));
const startTime = ref("09:00");
const endTime = ref("10:00");
const submitting = ref(false);
const submitError = ref("");
const created = ref<Reservation | null>(null);
const today = localDate();

let lastRequestKey = "";
let clientRequestId = "";

const resource = computed(() => availability.value?.resource ?? null);
const selectedStart = computed(
  () => `${date.value}T${startTime.value}:00+08:00`,
);
const selectedEnd = computed(() => `${date.value}T${endTime.value}:00+08:00`);

const localIssue = computed(() => {
  if (!availability.value) return "";
  if (!date.value || !startTime.value || !endTime.value)
    return "请选择日期和完整的起止时间";
  if (!availability.value.resource.enabled) return "资源已停用";
  if (
    availability.value.resource.resourceType === "DISCUSSION_ROOM" &&
    date.value <= today
  )
    return "讨论室需提前一自然日预约";
  if (Date.parse(selectedStart.value) < Date.now())
    return "不能预约已经开始的时段";
  if (startTime.value >= endTime.value) return "结束时间必须晚于开始时间";
  const block = availability.value.blocks.find((item) =>
    overlaps(
      selectedStart.value,
      selectedEnd.value,
      item.startTime,
      item.endTime,
    ),
  );
  if (block) {
    return `该时段存在${block.blockType === "TEACHING" ? "教学占用" : "停用安排"}：${block.reason}`;
  }
  const conflict = availability.value.reservations.find((item) =>
    overlaps(
      selectedStart.value,
      selectedEnd.value,
      item.startTime,
      item.endTime,
    ),
  );
  if (conflict) return "该时段已有预约，请选择其他时间";
  if (
    !availability.value.availablePeriods?.some(
      (slot) =>
        Date.parse(slot.startTime) <= Date.parse(selectedStart.value) &&
        Date.parse(slot.endTime) >= Date.parse(selectedEnd.value),
    )
  )
    return "所选时段当前不可约，请换一个时间";
  return "";
});

async function loadAvailability() {
  const sequence = ++loadSequence;
  availability.value = null;
  loading.value = true;
  loadError.value = "";
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date.value)) {
    loadError.value = "请选择有效日期";
    loading.value = false;
    return;
  }
  try {
    const value = await resourcesApi.availability(resourceId, date.value);
    if (sequence === loadSequence) availability.value = value;
  } catch (exception) {
    if (sequence === loadSequence) loadError.value = errorMessage(exception);
  } finally {
    if (sequence === loadSequence) loading.value = false;
  }
}

function onDateChange() {
  created.value = null;
  lastRequestKey = "";
  void loadAvailability();
}

function ensureRequestId(): string {
  const key = `${resourceId}|${date.value}|${startTime.value}|${endTime.value}`;
  if (key !== lastRequestKey) {
    lastRequestKey = key;
    clientRequestId = crypto.randomUUID();
  }
  return clientRequestId;
}

async function submit() {
  if (!availability.value || loading.value) {
    submitError.value = "请先加载可用时段";
    return;
  }
  if (localIssue.value) {
    submitError.value = localIssue.value;
    return;
  }
  submitting.value = true;
  submitError.value = "";
  try {
    created.value = await reservationsApi.create({
      resourceId,
      startTime: selectedStart.value,
      endTime: selectedEnd.value,
      clientRequestId: ensureRequestId(),
    });
    lastRequestKey = "";
    clientRequestId = "";
    await campus.initialize(true);
    await loadAvailability();
    window.scrollTo({ top: 0, behavior: "smooth" });
  } catch (exception) {
    submitError.value = errorMessage(exception);
    if (
      exception instanceof HttpError &&
      exception.code === "RESERVATION_CONFLICT"
    ) {
      await loadAvailability();
    }
  } finally {
    submitting.value = false;
  }
}

function overlaps(
  aStart: string,
  aEnd: string,
  bStart: string,
  bEnd: string,
): boolean {
  return (
    Date.parse(aStart) < Date.parse(bEnd) &&
    Date.parse(aEnd) > Date.parse(bStart)
  );
}

function timeLabel(iso: string): string {
  return iso.slice(11, 16);
}

function typeLabel(type: string): string {
  const labels: Record<string, string> = {
    LIBRARY_SEAT: "图书馆座位",
    BADMINTON: "羽毛球场",
    TABLE_TENNIS: "乒乓球台",
    TENNIS: "网球场",
    DISCUSSION_ROOM: "讨论室",
    CLASSROOM: "教室",
  };
  return labels[type] ?? type;
}

function blockLabel(type: string): string {
  if (type === "TEACHING") return "教学占用";
  if (type === "EXTERNAL" || type === "SPORTS" || type === "SPORTS_OCCUPANCY")
    return "外部占用";
  if (type === "DISABLED") return "资源停用";
  return type;
}

onMounted(loadAvailability);
</script>

<template>
  <section>
    <div class="page-head">
      <div>
        <p class="small">
          <RouterLink to="/resources" class="back-link"
            >← 返回资源列表</RouterLink
          >
        </p>
        <h1>预约资源</h1>
        <p v-if="resource">{{ resource.name }} · {{ resource.location }}</p>
      </div>
      <RouterLink class="btn btn-ghost" to="/bookings">我的预约</RouterLink>
    </div>

    <div v-if="created" class="alert alert-success stack">
      <div class="success-line">
        <strong>{{
          created.status === "RESERVED" ? "预约已确认" : "已找回预约记录"
        }}</strong>
        <StatusBadge
          :status="created.status"
          :resource-type="created.resourceType"
        />
      </div>
      <p>
        {{ created.resourceName }} · {{ created.startTime.slice(0, 10) }}
        {{ timeLabel(created.startTime) }}–{{ timeLabel(created.endTime) }}
      </p>
      <p class="small">
        预约号 {{ created.reservationId }}。座位请在确认后 30
        分钟内到馆；教室和体育场地请在开始后 15 分钟内签到。
      </p>
      <div class="inline-actions">
        <RouterLink
          class="btn btn-primary"
          :to="`/bookings/${created.reservationId}`"
          >查看预约详情</RouterLink
        >
        <RouterLink class="btn btn-ghost" to="/bookings">我的预约</RouterLink>
      </div>
    </div>

    <div class="panel date-controlbar">
      <label class="field"
        >查看日期<input
          v-model="date"
          class="input"
          type="date"
          :min="today"
          @change="onDateChange" /></label
      ><button
        class="button secondary"
        :disabled="loading"
        @click="loadAvailability"
      >
        重新加载
      </button>
    </div>
    <div v-if="loadError" class="alert alert-error">{{ loadError }}</div>
    <div v-else-if="loading" class="card empty">正在加载可约信息…</div>

    <div v-else-if="resource" class="two-column">
      <div class="stack">
        <form class="card panel stack" @submit.prevent="submit">
          <div>
            <h2>选择日期与时段</h2>
            <p class="small">
              所有时间按 Asia/Shanghai 解释；冲突和开放状态以服务端检查为准。
            </p>
          </div>

          <div class="form-grid">
            <label class="field">
              <span>开始时间</span>
              <input
                v-model="startTime"
                class="input"
                type="time"
                step="1800"
              />
            </label>
            <label class="field">
              <span>结束时间</span>
              <input v-model="endTime" class="input" type="time" step="1800" />
            </label>
          </div>

          <div v-if="localIssue" class="alert alert-warning">
            {{ localIssue }}
          </div>

          <button
            class="btn btn-primary"
            type="submit"
            :disabled="submitting || !!localIssue || !resource?.enabled"
          >
            {{ submitting ? "提交中…" : "提交预约" }}
          </button>

          <div v-if="submitError" class="alert alert-error">
            {{ submitError }}
          </div>
          <p class="small">
            提交按钮的禁用只改善体验；服务端会在同一事务内重新检查开放、教学占用和预约冲突。
          </p>
        </form>

        <div class="card panel">
          <h2>当天安排</h2>
          <div v-if="availability" class="stack">
            <div>
              <p class="section-label">开放时段</p>
              <div
                v-if="availability.openings.length"
                class="availability-list"
              >
                <div
                  v-for="slot in availability.openings"
                  :key="slot.startTime"
                  class="availability-row"
                >
                  <span>开放</span>
                  <strong
                    >{{ timeLabel(slot.startTime) }} –
                    {{ timeLabel(slot.endTime) }}</strong
                  >
                </div>
              </div>
              <p v-else class="small">当天暂无开放安排。</p>
            </div>

            <div v-if="availability.blocks.length">
              <p class="section-label">不可约安排</p>
              <div class="availability-list">
                <div
                  v-for="block in availability.blocks"
                  :key="`${block.startTime}-${block.blockType}`"
                  class="availability-row block-row"
                >
                  <span>{{ blockLabel(block.blockType) }}</span>
                  <span
                    >{{ timeLabel(block.startTime) }} –
                    {{ timeLabel(block.endTime) }} · {{ block.reason }}</span
                  >
                </div>
              </div>
            </div>

            <div v-if="availability.reservations.length">
              <p class="section-label">已有预约</p>
              <div class="availability-list">
                <div
                  v-for="slot in availability.reservations"
                  :key="slot.reservationId"
                  class="availability-row"
                >
                  <span>已占用</span>
                  <span
                    >{{ timeLabel(slot.startTime) }} –
                    {{ timeLabel(slot.endTime) }} · {{ slot.status }}</span
                  >
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <aside class="stack">
        <div class="card panel resource-summary">
          <span class="pill">{{ typeLabel(resource.resourceType) }}</span>
          <h2>{{ resource.name }}</h2>
          <p class="small">
            {{ resource.location }} · {{ resource.unitLabel }}
          </p>
          <p v-if="resource.enabled" class="pill pill-success">开放中</p>
          <p v-else class="pill pill-danger">已停用</p>
        </div>
        <div class="card panel">
          <h2>预约规则</h2>
          <ul class="rule-list">
            <li>同一资源的重叠时段只能存在一个有效预约。</li>
            <li>教学占用、停用安排和开放时段外均不可预约。</li>
            <li>
              讨论室须最晚在使用日期前一自然日提交，不套用15分钟签到期限。
            </li>
            <li>
              座位预约成功后 30 分钟内确认到馆；教室和体育场地开始后 15
              分钟内签到。
            </li>
            <li>
              目标流程：教室/体育正常结束由后台释放，座位刷卡离场立即释放；这些到离场接口及任务尚未接入本页。
            </li>
          </ul>
        </div>
      </aside>
    </div>

    <div v-else class="card empty">资源不存在或暂不可用。</div>
  </section>
</template>
