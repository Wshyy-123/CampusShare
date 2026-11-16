<script setup lang="ts">
import { ref } from "vue";
import QRCode from "qrcode";
import { placeCodeUrl } from "../utils/placeCode";
const dialog = ref<HTMLDialogElement | null>(null);
const title = ref("");
const location = ref("");
const url = ref("");
const image = ref("");
const error = ref("");
const loopback = ref(false);
let generation = 0;
async function open(id: string, name: string, where: string) {
  const token = ++generation;
  title.value = name;
  location.value = where;
  image.value = "";
  error.value = "";
  dialog.value?.showModal();
  try {
    url.value = placeCodeUrl(
      id,
      import.meta.env.VITE_PUBLIC_BASE_URL || window.location.origin,
    );
    loopback.value = ["localhost", "127.0.0.1", "[::1]"].includes(
      new URL(url.value).hostname,
    );
    const png = await QRCode.toDataURL(url.value, {
      width: 280,
      margin: 3,
      errorCorrectionLevel: "M",
    });
    if (token === generation) image.value = png;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "二维码生成失败";
  }
}
defineExpose({ open });
</script>
<template>
  <dialog ref="dialog" class="modal" aria-labelledby="place-code-title">
    <div class="modal-heading">
      <h2 id="place-code-title">{{ title }} · 地点码</h2>
      <button
        class="icon-button"
        aria-label="关闭地点码"
        @click="dialog?.close()"
      >
        ×
      </button>
    </div>
    <p class="muted">{{ location }}</p>
    <div class="place-code">
      <img
        v-if="image"
        :src="image"
        :alt="`${title}签到地点二维码`"
        width="280"
        height="280"
      />
      <p v-else>正在生成地点码…</p>
    </div>
    <p>手机扫描此地点码，登录预约账户后自动核验签到。重复扫码不会自动签退。</p>
    <p v-if="loopback" class="muted">
      当前地址仅本机可访问。手机扫码请使用同一局域网或已部署的网站地址生成地点码。
    </p>
    <label class="form-field"
      >地点链接<input class="input" :value="url" readonly aria-label="地点链接"
    /></label>
    <p v-if="error" class="error-message" role="alert">{{ error }}</p>
    <div class="modal-actions">
      <a
        v-if="image"
        class="button secondary"
        :href="image"
        download="campusshare-place.png"
        >保存二维码</a
      ><button class="button primary" @click="dialog?.close()">完成</button>
    </div>
  </dialog>
</template>
<style scoped>
.place-code {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}
.place-code img {
  max-width: 100%;
  height: auto;
  border-radius: 12px;
  background: white;
}
</style>
