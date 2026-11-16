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
      <div class="workspace-label">我的校园 <span>WORKSPACE</span></div>
      <nav class="main-nav" aria-label="主导航">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to"
          ><AppIcon :name="item.icon" /><span>{{ item.label }}</span
          ><span v-if="isDemo && item.to === '/bookings'" class="nav-count">{{
            activeBookings.length
          }}</span></RouterLink
        >
      </nav>
      <div class="sidebar-guide">
        <span class="guide-symbol"><AppIcon name="book" :size="25" /></span>
        <h3>让每一处空间<br />都被好好使用。</h3>
        <p>学习、交流、运动，<br />在这里找到你的下一站。</p>
        <RouterLink to="/resources"
          >探索校园空间 <AppIcon name="arrow" :size="16"
        /></RouterLink>
      </div>
      <button class="scope-link" @click="scopeDialog?.showModal()">
        <AppIcon name="info" :size="17" /> 演示说明
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
          校园工作台 <AppIcon name="chevron" :size="13" /><span>{{
            route.meta.title
          }}</span>
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
                  : "接口模式"
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
      <div class="demo-banner">
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
        <span>CampusShare / 共享校园的每一种可能</span
        ><span>{{
          isDemo ? "前端演示数据 · 刷新后重置" : "接口模式 · 不回退演示数据"
        }}</span>
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
          <span class="eyebrow">ABOUT THIS PREVIEW</span>
          <h2 id="scope-title">这次演示包含什么</h2>
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
        本地演示模式使用内存样例；后端接口模式连接主后端，登录及预约会读写数据库。学校教务、场馆与账号种子数据用于课程模拟，不代表学校实际安排。
      </p>
      <div class="scope-item">
        <AppIcon name="check" />
        <div>
          <strong>可以操作</strong>
          <p>
            分类与搜索、日期切换、资源详情、时段选择、添加演示预约、查看我的预约、维护模拟占用。
          </p>
        </div>
      </div>
      <div class="scope-item">
        <AppIcon name="clock" />
        <div>
          <strong>尚未联调</strong>
          <p>
            扫码签到、刷卡到离场、占用维护写接口及后台自动释放尚未接入本界面。接口模式已接入主后端的登录、资源可用性与本人预约；真实学校教务和校园卡仍为模拟接入范围。
          </p>
        </div>
      </div>
      <p class="inline-note">
        演示模式数据只在当前页面内存；接口模式的预约写入后端数据库。真实管理页按服务端身份显示，当前仅可查看占用，不能维护。
      </p>
      <button class="button primary full-width" @click="scopeDialog?.close()">
        开始浏览
      </button>
    </dialog>
  </div>
</template>
