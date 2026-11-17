import { onBeforeUnmount, onMounted, ref } from "vue";

/** Keep booking controls current while a page stays open. The server rechecks every write. */
export function useCurrentTime() {
  const now = ref(Date.now());
  let timer: ReturnType<typeof setInterval> | undefined;
  onMounted(() => {
    now.value = Date.now();
    timer = setInterval(() => {
      now.value = Date.now();
    }, 1000);
  });
  onBeforeUnmount(() => {
    if (timer !== undefined) clearInterval(timer);
  });
  return now;
}
