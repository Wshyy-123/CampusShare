<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRouter, RouterLink } from "vue-router";
import AppIcon from "../components/common/AppIcon.vue";
import SpaceScene from "../components/visual/SpaceScene.vue";
import {
  kindLabels,
  localDate,
  formatHour,
  rules,
  dateAfter,
} from "../utils/presentation";
import { useCampus } from "../composables/useCampus";
import type { Space, SpaceKind } from "../types/campus";

const router = useRouter();
const { state, isDemo, slots, blocks, reserve, loadAvailability } = useCampus();
const spaces = computed(() => state.resources);
const submitting = ref(false);
const selectedKind = ref<SpaceKind | "ALL">("ALL");
const keyword = ref("");
const date = ref(localDate());
const onlyAvailable = ref(false);
const selectedSpace = ref<Space | null>(null);
const chosenHour = ref<number | null>(null);
const detailDialog = ref<HTMLDialogElement | null>(null);
const grid = ref<HTMLElement | null>(null);
const categories: { id: SpaceKind | "ALL"; label: string; icon: string }[] = [
  { id: "ALL", label: "全部空间", icon: "grid" },
  { id: "LIBRARY_SEAT", label: "图书馆", icon: "book" },
  { id: "CLASSROOM", label: "教室", icon: "classroom" },
  { id: "SPORTS", label: "体育场所", icon: "sport" },
];
const availableCount = computed(
  () =>
    spaces.value.filter((space) =>
      slots(space.id, date.value).some((s) => s.available),
    ).length,
);
const filtered = computed(() =>
  spaces.value.filter(
    (space) =>
      (selectedKind.value === "ALL" ||
        space.kind === selectedKind.value ||
        (selectedKind.value === "LIBRARY_SEAT" &&
          space.kind === "DISCUSSION_ROOM")) &&
      `${space.name} ${space.location} ${space.tags.join(" ")}`
        .toLowerCase()
        .includes(keyword.value.trim().toLowerCase()) &&
      (!onlyAvailable.value ||
        slots(space.id, date.value).some((s) => s.available)),
  ),
);
const selectedSlots = computed(() =>
  selectedSpace.value ? slots(selectedSpace.value.id, date.value) : [],
);
const detailBlocks = computed(() =>
  selectedSpace.value ? blocks(selectedSpace.value.id, date.value) : [],
);
const availableRanges = computed(() => {
  const ranges: { start: number; end: number }[] = [];
  for (const slot of selectedSlots.value.filter((s) => s.available)) {
    const last = ranges.at(-1);
    if (last?.end === slot.start) last.end = slot.end;
    else ranges.push({ start: slot.start, end: slot.end });
  }
  return ranges;
});
watch([date, () => state.resources], async () => {
  chosenHour.value = null;
  await Promise.all(
    state.resources.map((space) => loadAvailability(space.id, date.value)),
  );
});
async function openDetail(space: Space) {
  if (space.kind === "DISCUSSION_ROOM" && date.value <= localDate())
    date.value = dateAfter(1);
  if (!(await loadAvailability(space.id, date.value))) return;
  selectedSpace.value = space;
  chosenHour.value = null;
  detailDialog.value?.showModal();
}
async function makeReservation() {
  if (!selectedSpace.value || chosenHour.value === null) return;
  submitting.value = true;
  try {
    if (await reserve(selectedSpace.value.id, date.value, chosenHour.value)) {
      detailDialog.value?.close();
      await router.push("/bookings");
    }
  } finally {
    submitting.value = false;
  }
}
function freeLabel(space: Space) {
  if (space.disabled) return "维护中";
  const free = slots(space.id, date.value).filter((s) => s.available).length;
  return free ? `${free} 个可约时段` : "当日已约满";
}
</script>

<template>
  <section class="hero-panel">
    <div class="hero-copy">
      <span class="eyebrow"
        ><span class="tiny-line" /> YOUR CAMPUS, YOUR SPACE</span
      >
      <h1>
        好时光，<br />从找到<span class="gradient-text">好空间</span>开始。
      </h1>
      <p>
        一个安静的座位，一间灵感碰撞的教室，<br
          class="desktop-break"
        />一场说走就走的运动。校园空间，尽在校享。
      </p>
      <button
        class="button hero-button"
        @click="grid?.scrollIntoView({ behavior: 'smooth', block: 'start' })"
      >
        寻找我的空间 <AppIcon name="arrow" :size="18" />
      </button>
    </div>
    <div class="hero-art" aria-hidden="true">
      <div class="orbit orbit-one" />
      <div class="orbit orbit-two" />
      <div class="orbit orbit-three" />
      <div class="orbital-core">
        <div class="core-icon"><AppIcon name="grid" :size="46" /></div>
        <span>CAMPUSSHARE</span>
      </div>
      <div class="floating-space floating-library">
        <AppIcon name="book" :size="20" /><span
          >安静，自有一席之地<small>LIBRARY</small></span
        >
      </div>
      <div class="floating-space floating-sport">
        <AppIcon name="sport" :size="20" /><span
          >热爱，随时上场<small>SPORTS</small></span
        >
      </div>
      <span class="orbit-point point-one" /><span
        class="orbit-point point-two"
      /><span class="hero-coordinate">EXPLORE / CONNECT / SHARE</span>
    </div>
    <span class="hero-index">01 — EXPLORE THE CAMPUS</span>
  </section>
  <section class="summary-strip" aria-label="演示资源概况">
    <div>
      <span class="summary-icon violet"><AppIcon name="grid" /></span>
      <div>
        <strong>{{ spaces.length.toString().padStart(2, "0") }}</strong
        ><span>{{ isDemo ? "精选演示资源" : "可查询资源" }}</span>
      </div>
    </div>
    <div>
      <span class="summary-icon mint"><AppIcon name="check" /></span>
      <div>
        <strong>{{ availableCount.toString().padStart(2, "0") }}</strong
        ><span>所选日有空闲</span>
      </div>
    </div>
    <div>
      <span class="summary-icon amber"><AppIcon name="shield" /></span>
      <div>
        <strong>3 <small>类</small></strong
        ><span>学习与运动场景</span>
      </div>
    </div>
    <p>让资源流动起来，<br />让校园生活更从容。</p>
  </section>
  <section ref="grid" class="explore-section">
    <div class="section-heading">
      <div>
        <span class="eyebrow">FIND YOUR NEXT SPOT</span>
        <h2>发现你的下一站<span class="heading-dot">.</span></h2>
      </div>
      <label class="date-filter"
        ><AppIcon name="calendar" :size="17" /><span class="sr-only"
          >查看日期</span
        ><input
          v-model="date"
          type="date"
          :min="localDate()"
          aria-label="查看日期"
      /></label>
    </div>
    <div class="explore-toolbar">
      <div class="category-tabs" aria-label="资源分类">
        <button
          v-for="category in categories"
          :key="category.id"
          :class="{ active: selectedKind === category.id }"
          :aria-pressed="selectedKind === category.id"
          @click="selectedKind = category.id"
        >
          <AppIcon :name="category.icon" :size="16" />{{ category.label
          }}<span>{{
            category.id === "ALL"
              ? spaces.length
              : spaces.filter(
                  (s) =>
                    s.kind === category.id ||
                    (category.id === "LIBRARY_SEAT" &&
                      s.kind === "DISCUSSION_ROOM"),
                ).length
          }}</span>
        </button>
      </div>
      <label class="search-field"
        ><AppIcon name="search" :size="18" /><span class="sr-only"
          >搜索场地</span
        ><input
          v-model="keyword"
          type="search"
          placeholder="搜索空间、位置或设施…"
      /></label>
    </div>
    <div class="result-meta">
      <span
        >共找到 <strong>{{ filtered.length }}</strong> 处空间
        <span class="meta-divider">/</span>
        <span>所选日期 {{ date }}</span></span
      ><label class="available-filter"
        ><input v-model="onlyAvailable" type="checkbox" />只看有空闲</label
      >
    </div>
    <div v-if="filtered.length" class="resource-grid">
      <article
        v-for="space in filtered"
        :key="space.id"
        class="resource-card"
        :class="{ 'disabled-card': space.disabled }"
      >
        <button
          class="scene-button"
          :aria-label="`查看${space.name}详情`"
          @click="openDetail(space)"
        >
          <SpaceScene :kind="space.kind" :variant="space.variant" /><span
            class="scene-category"
            ><AppIcon
              :name="
                space.kind === 'LIBRARY_SEAT' ||
                space.kind === 'DISCUSSION_ROOM'
                  ? 'book'
                  : space.kind === 'CLASSROOM'
                    ? 'classroom'
                    : 'sport'
              "
              :size="13"
            />{{ kindLabels[space.kind] }}</span
          ><span class="scene-code">{{ space.code }}</span>
        </button>
        <div class="resource-body">
          <div class="resource-title">
            <h3>
              <button @click="openDetail(space)">{{ space.name }}</button>
            </h3>
            <span class="status-dot" :class="{ amber: space.disabled }" />
          </div>
          <p class="resource-location">
            <AppIcon name="location" :size="14" />{{ space.location }}
          </p>
          <div class="resource-tags">
            <span v-for="tag in space.tags" :key="tag">{{ tag }}</span>
          </div>
          <div class="resource-bottom">
            <span :class="space.disabled ? 'text-amber' : 'text-mint'"
              ><AppIcon
                :name="space.disabled ? 'settings' : 'clock'"
                :size="14"
              />{{ freeLabel(space) }}</span
            ><button @click="openDetail(space)">
              查看时段<AppIcon name="arrow" :size="16" />
            </button>
          </div>
        </div>
      </article>
    </div>
    <div v-else class="empty-state">
      <AppIcon name="search" :size="32" />
      <h3>还没有找到合适的空间</h3>
      <p>换个关键词，或试试其他分类。</p>
      <button
        class="button secondary"
        @click="
          keyword = '';
          selectedKind = 'ALL';
          onlyAvailable = false;
        "
      >
        清除筛选
      </button>
    </div>
  </section>
  <div class="bottom-tip">
    <AppIcon name="info" :size="16" />
    <p>
      提前查看占用安排，让每一次出发都不扑空。<span>
        {{
          isDemo
            ? "当前资源与占用均为演示样例。"
            : "当前资源、占用与预约来自主后端。"
        }}</span
      >
    </p>
  </div>

  <dialog
    ref="detailDialog"
    class="modal detail-modal"
    aria-labelledby="detail-title"
    @click="
      (event) => {
        if (event.target === detailDialog) detailDialog?.close();
      }
    "
  >
    <template v-if="selectedSpace"
      ><div class="modal-heading">
        <div>
          <span class="eyebrow">{{
            isDemo ? "SPACE DETAILS · 演示数据" : "SPACE DETAILS · 服务端数据"
          }}</span>
          <h2 id="detail-title">{{ selectedSpace.name }}</h2>
          <p class="muted">
            {{ selectedSpace.location }} ·
            {{
              selectedSpace.capacity
                ? `${selectedSpace.capacity} 人`
                : "容量以场地说明为准"
            }}
            {{
              selectedSpace.kind === "LIBRARY_SEAT" ? "独立座位" : "使用空间"
            }}
          </p>
        </div>
        <button
          class="icon-button"
          aria-label="关闭详情"
          @click="detailDialog?.close()"
        >
          <AppIcon name="close" />
        </button>
      </div>
      <div class="detail-date">
        <label for="detail-date">查看日期</label
        ><input
          id="detail-date"
          v-model="date"
          class="input"
          type="date"
          :min="localDate()"
        /><span class="muted"
          >{{
            selectedSpace.close > selectedSpace.open ? "开放" : "当日未开放"
          }}
          {{
            selectedSpace.close > selectedSpace.open
              ? formatHour(selectedSpace.open)
              : ""
          }}
          {{ selectedSpace.close > selectedSpace.open ? "—" : ""
          }}{{
            selectedSpace.close > selectedSpace.open
              ? formatHour(selectedSpace.close)
              : ""
          }}</span
        >
      </div>
      <div class="timeline-heading">
        <h3>当日占用一览</h3>
        <span class="muted">{{ date }}</span>
      </div>
      <div class="timeline-track" aria-label="0时至24时时段占用">
        <div
          v-for="block in detailBlocks"
          :key="`${block.style}-${block.id}`"
          class="time-segment"
          :class="block.style"
          :style="{
            left: `${(block.start / 24) * 100}%`,
            width: `${((block.end - block.start) / 24) * 100}%`,
          }"
          :title="`${block.label} ${formatHour(block.start)}—${formatHour(block.end)}`"
        >
          <span>{{ block.label }}</span>
        </div>
        <div v-if="selectedSpace.disabled" class="maintenance all-day-block">
          全天维护中
        </div>
      </div>
      <div class="timeline-ticks">
        <span v-for="hour in [0, 4, 8, 12, 16, 20, 24]" :key="hour">{{
          formatHour(hour)
        }}</span>
      </div>
      <div class="timeline-legend">
        <span><i class="legend-dot free" />空闲</span
        ><span><i class="legend-dot teaching" />教学</span
        ><span><i class="legend-dot reserved" />系统预约</span
        ><span><i class="legend-dot external" />外部占用</span
        ><span><i class="legend-dot maintenance" />停用维护</span>
      </div>
      <p v-if="availableRanges.length" class="available-ranges">
        可约区间：{{
          availableRanges
            .map((r) => `${formatHour(r.start)}—${formatHour(r.end)}`)
            .join("、")
        }}
      </p>
      <p v-else class="inline-note">该资源当天暂无可约时段。</p>
      <RouterLink
        v-if="!isDemo && !selectedSpace.disabled"
        class="button secondary"
        :to="`/resources/${selectedSpace.id}/reserve`"
        @click="detailDialog?.close()"
        >打开完整预约表单<AppIcon name="arrow" :size="16"
      /></RouterLink>
      <h3 class="slot-heading">
        选择一个快捷时段
        <small>{{
          isDemo
            ? "按 1 小时展示，仅用于交互演示"
            : "快捷预约 1 小时；也可使用完整预约表单"
        }}</small>
      </h3>
      <div class="slot-grid">
        <button
          v-for="slot in selectedSlots"
          :key="slot.start"
          :disabled="!slot.available"
          :aria-pressed="chosenHour === slot.start"
          :class="{ chosen: chosenHour === slot.start }"
          @click="chosenHour = slot.start"
        >
          <strong
            >{{ formatHour(slot.start) }}—{{ formatHour(slot.end) }}</strong
          ><span>{{ slot.reason }}</span>
        </button>
      </div>
      <div class="rule-box">
        <AppIcon name="shield" :size="18" />
        <div>
          <strong>使用规则</strong>
          <p v-for="rule in rules[selectedSpace.kind]" :key="rule">
            {{ rule }}
          </p>
        </div>
      </div>
      <div class="modal-actions">
        <span class="muted">{{
          isDemo
            ? "仅创建本地演示记录，不向后端提交。"
            : "提交后由后端再次校验身份、开放与预约冲突。"
        }}</span
        ><button
          class="button primary"
          :disabled="chosenHour === null || submitting || !state.ready"
          @click="makeReservation"
        >
          {{ submitting ? "提交中…" : isDemo ? "模拟预约" : "提交预约" }}
          <AppIcon name="arrow" :size="16" />
        </button>
      </div>
    </template>
  </dialog>
</template>
