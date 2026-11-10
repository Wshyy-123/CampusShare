<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink, RouterView, useRoute } from "vue-router";
import AppIcon from "./components/common/AppIcon.vue";
import { useCampus } from "./composables/useCampus";

const { state, activeBookings, isDemo, initialize } = useCampus();
onMounted(initialize);
const route = useRoute();
const scopeDialog = ref<HTMLDialogElement | null>(null);
import { navigationPages as nav } from "./config/pages";
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
          ><span v-if="item.to === '/bookings'" class="nav-count">{{
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
            >{{ isDemo ? "演示访客" : "身份未接入" }}
            <small>{{ isDemo ? "学生视角" : "接口模式" }}</small></span
          ><span class="avatar">访</span>
        </div>
      </header>
      <div class="demo-banner">
        <span class="demo-dot" /><span
          ><strong>{{ isDemo ? "前端交互演示" : "真实接口接入模式" }}</strong
          ><span class="banner-extra">{{
            isDemo
              ? " · 当前为模拟数据，未连接后端"
              : " · 禁止用模拟数据代替接口结果"
          }}</span></span
        ><button @click="scopeDialog?.showModal()">
          查看说明 <AppIcon name="arrow" :size="14" />
        </button>
      </div>
      <main id="main-content" class="main-content">
        <div v-if="state.error" class="inline-note" role="alert">
          {{ state.error }}
        </div>
        <RouterView />
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
        这是主前端新增的交互预览，帮助理解产品流程。所有场地、时段和预约均为本地样例，不代表学校实际安排。
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
            真实登录、数据库持久化、扫码签到、刷卡到离场、并发预约及后台自动释放。演示操作不会调用这些接口。
          </p>
        </div>
      </div>
      <p class="inline-note">
        演示预约与占用仅保存在当前页面内存，刷新即重置。这里的管理入口用于展示，不表示已实现管理员权限。
      </p>
      <button class="button primary full-width" @click="scopeDialog?.close()">
        开始浏览
      </button>
    </dialog>
  </div>
</template>
