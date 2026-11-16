<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { SpaceKind } from "../../types/campus";
import IllustratedSpaceScene from "./IllustratedSpaceScene.vue";
const props = defineProps<{
  kind: SpaceKind;
  variant: number;
  resourceType?: string;
}>();
const failed = ref(false);
const photo = computed(() => {
  if (props.resourceType === "TENNIS") return "/images/spaces/tennis.jpg";
  if (props.resourceType === "TABLE_TENNIS")
    return "/images/spaces/ping-pong.jpg";
  if (props.kind === "SPORTS") return "/images/spaces/sports.jpg";
  if (props.kind === "CLASSROOM") return "/images/spaces/classroom.jpg";
  return props.kind === "LIBRARY_SEAT"
    ? "/images/spaces/reading.webp"
    : "/images/spaces/library.webp";
});
watch(photo, () => {
  failed.value = false;
});
</script>
<template>
  <div
    class="space-scene scene-photo"
    :class="kind.toLowerCase()"
    aria-hidden="true"
    :data-resource-type="resourceType"
  >
    <img
      v-if="!failed"
      :src="photo"
      alt=""
      width="1000"
      height="640"
      loading="lazy"
      decoding="async"
      @error="failed = true"
    />
    <IllustratedSpaceScene v-else :kind="kind" :variant="variant" />
  </div>
</template>
