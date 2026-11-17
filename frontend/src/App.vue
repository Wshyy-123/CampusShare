<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink, RouterView, useRoute } from "vue-router";
import AppIcon from "./components/common/AppIcon.vue";
import { useCampus, notify } from "./composables/useCampus";
import { authState, logout, roleLabel } from "./stores/auth";
import { useRouter } from "vue-router";

const { state, activeBookings, isDemo, initialize } = useCampus();
onMounted(() => {
  if (isDemo || authState.user) void initialize();
});
const route = useRoute();
const scopeDialog = ref<HTMLDialogElement | null>(null);
import { navigationPages } from "./config/pages";
const campus = useCampus();
const router = useRouter();
const signingOut = ref(false);
const nav = computed(() =>
  navigationPages.filter(
    (page) =>
      isDemo ||
      !("roles" in page) ||
      (authState.user && page.roles?.includes(authState.user.role)),
  ),
);
async function signOut() {
  signingOut.value = true;
  try {
    await logout();
    campus.reset();
    await router.replace("/login");
  } catch (error) {
    notify(error instanceof Error ? error.message : "退出失败，请重试");
  } finally {
    signingOut.value = false;
  }
}
</script>

<template>
  <a class="skip-link" href="#main-content">跳转到内容</a>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/resources" aria-label="校享首页"
        ><span class="brand-mark"><span /><span /><span /><span /></span
        ><span>校享<span class="brand-en">CAMPUSSHARE</span></span></RouterLink
      >
      <div class="workspace-label">校园工作台 <span>WORKSPACE</span></div>
      <nav class="main-nav" aria-label="主导航">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to"
          ><AppIcon :name="item.icon" /><span>{{ item.label }}</span
          ><span v-if="isDemo && item.to === '/bookings'" class="nav-count">{{
            activeBookings.length
          }}</span></RouterLink
        >
      </nav>
      <button class="scope-link" @click="scopeDialog?.showModal()">
        <AppIcon name="info" :size="17" /> 使用说明
        <AppIcon name="chevron" :size="15" />
      </button>
      <div class="sidebar-footer">
        <span class="small-brand">C.</span
        ><span>校园公共场地预约平台<small>第四组 · 课程项目</small></span>
      </div>
    </aside>
    <div class="main-shell">
      <header class="topbar">
        <div class="breadcrumb">
          校园空间 <AppIcon name="chevron" :size="13" /><span>{{
            route.meta.title
          }}</span>
        </div>
        <div
          v-if="!isDemo && authState.user"
          class="sync-status"
          :class="{ 'sync-error': state.error }"
        >
          <span />{{
            state.error ? "同步异常" : state.ready ? "已同步" : "正在同步"
          }}
        </div>
        <div class="profile">
          <span class="profile-name"
            >{{
              isDemo ? "演示访客" : (authState.user?.displayName ?? "未登录")
            }}
            <small>{{
              isDemo
                ? "学生视角"
                : authState.user
                  ? roleLabel(authState.user.role)
                  : "欢迎使用"
            }}</small></span
          ><span class="avatar">{{
            isDemo ? "访" : (authState.user?.displayName?.slice(0, 1) ?? "客")
          }}</span
          ><button
            v-if="!isDemo && authState.user"
            class="button secondary session-button"
            :disabled="signingOut"
            @click="signOut"
          >
            {{ signingOut ? "退出中…" : "退出" }}</button
          ><RouterLink
            v-else-if="!isDemo && route.path !== '/login'"
            class="button secondary session-button"
            to="/login"
            >登录</RouterLink
          >
        </div>
      </header>
      <div v-if="isDemo" class="demo-banner">
        <span class="demo-dot" /><span
          ><strong>{{ isDemo ? "前端交互演示" : "后端接口模式" }}</strong
          ><span class="banner-extra">{{
            isDemo
              ? " · 当前为模拟数据，未连接后端"
              : " · 登录与预约以服务端结果为准"
          }}</span></span
        ><button @click="scopeDialog?.showModal()">
          查看说明 <AppIcon name="arrow" :size="14" />
        </button>
      </div>
      <main id="main-content" class="main-content">
        <div v-if="state.error" class="inline-note" role="alert">
          {{ state.error }}
        </div>
        <RouterView :key="route.path" />
      </main>
      <footer class="page-footer">
        <span
          >校享 CampusShare
          <span class="footer-divider">/</span> 让校园空间被好好使用</span
        >
        <span class="photo-credit"
          >场景图片仅作示意 ·
          <a
            href="https://unsplash.com"
            target="_blank"
            rel="noopener noreferrer"
            >Unsplash</a
          >
          /
          <a
            href="https://www.pexels.com"
            target="_blank"
            rel="noopener noreferrer"
            >Pexels</a
          ></span
        >
      </footer>
    </div>
    <div v-if="state.notice" class="toast" role="status">
      <AppIcon name="info" :size="18" />{{ state.notice }}
    </div>
    <dialog
      ref="scopeDialog"
      class="modal scope-modal"
      aria-labelledby="scope-title"
      @click="
        (event) => {
          if (event.target === scopeDialog) scopeDialog?.close();
        }
      "
    >
      <div class="modal-heading">
        <div>
          <span class="eyebrow">CAMPUS GUIDE</span>
          <h2 id="scope-title">预约与使用说明</h2>
        </div>
        <button
          class="icon-button"
          aria-label="关闭说明"
          @click="scopeDialog?.close()"
        >
          <AppIcon name="close" />
        </button>
      </div>
      <p class="muted">
        统一预约校园里的座位、教室与运动场地。页面中的场地与账号为课程模拟数据。
      </p>
      <div class="scope-item">
        <AppIcon name="book" />
        <div>
          <strong>图书馆座位</strong>
          <p>
            预约成功起30分钟内模拟刷卡入场；刷卡离场后立即结束使用并释放座位，不支持主动取消。
          </p>
        </div>
      </div>
      <div class="scope-item">
        <AppIcon name="classroom" />
        <div>
          <strong>教室与体育场地</strong>
          <p>
            使用前可以取消。开始后15分钟内，使用预约账户扫描对应地点码签到；已签到的预约到期自动完成。
          </p>
        </div>
      </div>
      <div class="scope-item">
        <AppIcon name="calendar" />
        <div>
          <strong>讨论室</strong>
          <p>
            所有场地仅支持当天尚未开始的时段。讨论室按整间预约，开始前可取消。教学、外部占用与停用安排不能预约。
          </p>
        </div>
      </div>
      <p class="inline-note">
        {{
          isDemo
            ? "当前是演示模式，操作仅保存在当前页面；真实签到和管理操作需要连接后端。"
            : "当前已连接项目后端。手机扫码需要手机可访问的网站地址；图书馆刷卡使用模拟入口。"
        }}
      </p>
      <button class="button primary full-width" @click="scopeDialog?.close()">
        开始浏览
      </button>
    </dialog>
  </div>
</template>
