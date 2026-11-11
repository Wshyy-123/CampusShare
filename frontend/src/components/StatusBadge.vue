<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  status: string;
  cancelReason?: string | null;
  resourceType?: string;
}>();

const label = computed(() => {
  if (props.status === "RESERVED") {
    if (props.resourceType === "DISCUSSION_ROOM") return "已预约";
    if (props.resourceType === "LIBRARY_SEAT") return "已预约 · 待到馆";
    if (props.resourceType) return "已预约 · 待签到";
    return "已预约";
  }
  if (props.status === "CHECKED_IN") return "已确认到馆";
  if (props.status === "COMPLETED") return "已完成";
  if (props.status === "CANCELLED") {
    if (props.cancelReason === "USER_CANCEL") return "已取消 · 本人取消";
    if (props.cancelReason === "NO_SHOW") return "已取消 · 未到超时";
    if (props.cancelReason === "RESOURCE_DISABLED") return "已取消 · 资源停用";
    return "已取消";
  }
  return props.status;
});

const badgeClass = computed(() => `badge status-${props.status.toLowerCase()}`);
</script>

<template>
  <span :class="badgeClass">{{ label }}</span>
</template>
