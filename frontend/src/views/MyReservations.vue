<script setup lang="ts">
import { computed, ref } from "vue";
import { RouterLink } from "vue-router";
import { stateLabels, formatHour, rules } from "../utils/presentation";
import { useCampus } from "../composables/useCampus";
import AppIcon from "../components/common/AppIcon.vue";
import SpaceScene from "../components/visual/SpaceScene.vue";

const { state, activeBookings } = useCampus();
const spaces = computed(() => state.resources);
const tab = ref("ACTIVE");
const records = computed(() =>
  state.bookings
    .filter(
      (b) =>
        tab.value === "ALL" ||
        b.state === "RESERVED" ||
        b.state === "CHECKED_IN",
    )
    .map((b) => ({
      ...b,
      space: spaces.value.find((s) => s.id === b.resourceId)!,
    })),
);
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
      <span class="count-pill">{{ activeBookings.length }} 条进行中</span>
    </h2>
    <RouterLink class="button secondary" to="/resources"
      >去发现空间 <AppIcon name="arrow" :size="16"
    /></RouterLink>
  </div>
  <div class="simple-tabs">
    <button :class="{ active: tab === 'ACTIVE' }" @click="tab = 'ACTIVE'">
      待使用 / 使用中</button
    ><button :class="{ active: tab === 'ALL' }" @click="tab = 'ALL'">
      全部记录
    </button>
  </div>
  <div class="booking-list">
    <article v-for="record in records" :key="record.id" class="booking-card">
      <div class="booking-scene">
        <SpaceScene :kind="record.space.kind" :variant="record.space.variant" />
      </div>
      <div class="booking-content">
        <div class="booking-top">
          <span class="eyebrow">本地演示预约</span
          ><span class="status-label" :class="record.state.toLowerCase()">{{
            stateLabels[record.state]
          }}</span>
        </div>
        <h3>{{ record.space.name }}</h3>
        <p class="resource-location">
          <AppIcon name="location" :size="14" />{{ record.space.location }}
        </p>
        <p class="booking-time">
          <AppIcon name="calendar" :size="16" />{{ record.date
          }}<span
            >{{ formatHour(record.start) }}—{{ formatHour(record.end) }}</span
          >
        </p>
        <div class="booking-rule">
          <AppIcon name="info" :size="15" /><span>{{
            rules[record.space.kind][record.state === "CHECKED_IN" ? 1 : 0]
          }}</span>
        </div>
      </div>
    </article>
  </div>
  <div v-if="!records.length" class="empty-state">
    <AppIcon name="calendar" :size="32" />
    <h3>空闲的时间，也值得好好安排</h3>
    <p>去发现空间，添加一条演示预约。</p>
    <RouterLink class="button primary" to="/resources">浏览资源</RouterLink>
  </div>
  <div class="bottom-tip">
    <AppIcon name="info" :size="17" />
    <p>
      预置记录用于展示不同状态；新增预约仅保存在本次页面。<span
        >扫码、刷卡和自动释放尚未接入此演示。</span
      >
    </p>
  </div>
</template>
