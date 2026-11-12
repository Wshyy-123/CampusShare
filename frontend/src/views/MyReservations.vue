<script setup lang="ts">
import { computed, ref, watch, onMounted } from "vue";
import { RouterLink } from "vue-router";
import {
  formatHour,
  formatDateTime,
  rules,
} from "../utils/presentation";
import { useCampus } from "../composables/useCampus";
import AppIcon from "../components/common/AppIcon.vue";
import SpaceScene from "../components/visual/SpaceScene.vue";
import StatusBadge from "../components/StatusBadge.vue";
const { state, isDemo, loadBookings } = useCampus();
const filter = ref("ALL");
const page = ref(1);
const loading = ref(false);
const loadError = ref("");
const filters = [
  { value: "ALL", label: "全部记录" },
  { value: "RESERVED", label: "待签到" },
  { value: "CHECKED_IN", label: "使用中" },
  { value: "COMPLETED", label: "已完成" },
  { value: "CANCELLED", label: "已取消" },
];
const records = computed(() =>
  state.bookings
    .filter((b) => filter.value === "ALL" || b.state === filter.value)
    .map((b) => ({
      ...b,
      space: state.resources.find((s) => s.id === b.resourceId),
    })),
);
let sequence = 0;
async function refresh() {
  if (isDemo) return;
  const version = ++sequence;
  loading.value = true;
  loadError.value = "";
  const ok = await loadBookings(
    filter.value === "ALL" ? undefined : filter.value,
    page.value,
    10,
  );
  if (version !== sequence) return;
  if (!ok) loadError.value = "预约列表加载失败，请重试。";
  loading.value = false;
}
function selectFilter(value: string) {
  filter.value = value;
  page.value = 1;
}
watch([filter, page], refresh);
onMounted(refresh);
</script>
<template>
  <section class="page-intro">
    <span class="eyebrow">YOUR PLANS, IN ONE PLACE</span>
    <h1>把时间留给<span class="gradient-text">期待的事。</span></h1>
    <p>查看你的预约安排，按时到场，安心使用。</p>
  </section>
  <div class="section-heading">
    <h2>
      我的预约
      <span class="count-pill"
        >{{ isDemo ? records.length : state.bookingTotal }} 条记录</span
      >
    </h2>
    <div class="inline-actions">
      <button
        v-if="!isDemo"
        class="button secondary"
        :disabled="loading"
        @click="refresh"
      >
        刷新列表</button
      ><RouterLink class="button secondary" to="/resources"
        >去发现空间<AppIcon name="arrow" :size="16"
      /></RouterLink>
    </div>
  </div>
  <div class="simple-tabs" aria-label="预约状态筛选">
    <button
      v-for="item in filters"
      :key="item.value"
      :class="{ active: filter === item.value }"
      :aria-pressed="filter === item.value"
      @click="selectFilter(item.value)"
    >
      {{ item.label }}
    </button>
  </div>
  <div v-if="loading" class="panel empty-state" role="status">
    正在读取本人预约…
  </div>
  <div v-else-if="loadError" class="error-message" role="alert">
    {{ loadError
    }}<button class="button secondary" @click="refresh">重新加载</button>
  </div>
  <div v-else class="booking-list">
    <article v-for="record in records" :key="record.id" class="booking-card">
      <div v-if="record.space" class="booking-scene">
        <SpaceScene :kind="record.space.kind" :variant="record.space.variant" />
      </div>
      <div class="booking-content">
        <div class="booking-top">
          <span class="eyebrow">{{
            isDemo ? "本地演示预约" : `本人预约 #${record.id}`
          }}</span
          ><StatusBadge :status="record.state" :cancel-reason="record.cancelReason ?? null" :resource-type="record.space?.resourceType ?? record.space?.kind ?? ''" />
        </div>
        <h3>{{ record.space?.name ?? `资源 #${record.resourceId}` }}</h3>
        <p v-if="record.space" class="resource-location">
          <AppIcon name="location" :size="14" />{{ record.space.location }}
        </p>
        <p class="booking-time">
          <AppIcon name="calendar" :size="16" /><template
            v-if="record.startTime && record.endTime"
            >{{ formatDateTime(record.startTime) }} —
            {{ formatDateTime(record.endTime) }}</template
          ><template v-else
            >{{ record.date
            }}<span
              >{{ formatHour(record.start) }}—{{ formatHour(record.end) }}</span
            ></template
          >
        </p>
        <div v-if="record.space && (record.state === 'RESERVED' || record.state === 'CHECKED_IN')" class="booking-rule">
          <AppIcon name="info" :size="15" /><span>{{
            rules[record.space.kind][record.state === "CHECKED_IN" ? 1 : 0]
          }}</span>
        </div>
        <p v-if="record.arrivalDeadline && record.state === 'RESERVED'" class="muted">
          服务端到馆期限：{{ formatDateTime(record.arrivalDeadline) }}
        </p>
        <RouterLink
          v-if="!isDemo"
          class="button secondary booking-detail-link"
          :to="`/bookings/${record.id}`"
          >查看预约详情<AppIcon name="arrow" :size="15"
        /></RouterLink>
      </div>
    </article>
  </div>
  <div v-if="!loading && !loadError && !records.length" class="empty-state">
    <AppIcon name="calendar" :size="32" />
    <h3>还没有此状态的预约</h3>
    <p>
      {{
        isDemo
          ? "去发现空间，添加一条演示预约。"
          : "换个状态查看，或去发现空间创建预约。"
      }}
    </p>
    <RouterLink class="button primary" to="/resources">浏览资源</RouterLink>
  </div>
  <div
    v-if="!isDemo && !loading && !loadError && state.bookingTotal > 10"
    class="pagination"
  >
    <button class="button secondary" :disabled="page <= 1" @click="page--">
      上一页</button
    ><span
      >第 {{ state.bookingPage }} /
      {{ Math.ceil(state.bookingTotal / 10) }} 页</span
    ><button
      class="button secondary"
      :disabled="page * 10 >= state.bookingTotal"
      @click="page++"
    >
      下一页
    </button>
  </div>
  <div class="bottom-tip">
    <AppIcon name="info" :size="17" />
    <p>
      {{
        isDemo
          ? "预置记录用于展示不同状态；刷新页面后重置。"
          : "此处仅展示后端返回的本人记录；状态以服务端处理结果为准。"
      }}<span v-if="!isDemo"> 座位未到超时由后台自动处理，关闭页面仍生效；可刷新查看最新结果。</span>
    </p>
  </div>
</template>
