<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter, RouterLink } from "vue-router";
import { errorMessage } from "../api/http";
import { login, authState } from "../stores/auth";
import { useCampus } from "../composables/useCampus";
import { safeReturnPath } from "../utils/navigation";
import AppIcon from "../components/common/AppIcon.vue";
const router = useRouter();
const route = useRoute();
const campus = useCampus();
const form = reactive({ account: "", password: "" });
const submitting = ref(false);
const error = ref("");
const accounts = [
  { id: "student01", label: "学生一" },
  { id: "student02", label: "学生二" },
  { id: "teacher01", label: "教师" },
  { id: "admin01", label: "管理员" },
];
async function submit() {
  if (!form.account.trim() || !form.password) {
    error.value = "请输入账号和密码";
    return;
  }
  submitting.value = true;
  error.value = "";
  try {
    await login(form.account.trim(), form.password);
    campus.reset();
    await router.replace(safeReturnPath(route.query.redirect));
  } catch (exception) {
    error.value = errorMessage(exception);
  } finally {
    submitting.value = false;
  }
}
</script>
<template>
  <section class="login-layout">
    <div class="page-intro login-intro">
      <span class="eyebrow">YOUR CAMPUS, ON YOUR TERMS</span>
      <h1>你的校园，<br /><span>有处可去。</span></h1>
      <p>登录校享，安排下一次自习、讨论与运动。</p>
      <div class="login-scenes">
        <img
          src="/images/spaces/library.webp"
          alt="图书馆场景示意"
          width="1000"
          height="640"
        /><span>FOCUS. MEET. MOVE.</span>
      </div>
      <div class="login-features">
        <span><AppIcon name="book" :size="16" /> 图书馆</span
        ><span><AppIcon name="classroom" :size="16" /> 教室</span
        ><span><AppIcon name="sport" :size="16" /> 体育场所</span>
      </div>
    </div>
    <div v-if="campus.isDemo" class="panel login-panel">
      <h2>当前为演示模式</h2>
      <p class="muted">
        浏览示例页面无需登录。真实登录与预约请在接口模式运行并启动主后端。
      </p>
      <RouterLink class="button primary" to="/resources"
        >返回发现空间</RouterLink
      >
    </div>
    <form v-else class="panel login-panel" @submit.prevent="submit">
      <span class="eyebrow">WELCOME BACK</span>
      <h2>登录校享</h2>
      <p class="muted">欢迎回来，继续你的校园计划。</p>
      <label class="form-field"
        >账号<input
          v-model="form.account"
          class="input"
          autocomplete="username"
          required /></label
      ><label class="form-field"
        >密码<input
          v-model="form.password"
          class="input"
          type="password"
          autocomplete="current-password"
          required
      /></label>
      <p v-if="error || authState.error" class="error-message" role="alert">
        {{ error || authState.error }}
      </p>
      <button
        class="button primary full-width"
        type="submit"
        :disabled="submitting"
      >
        {{ submitting ? "登录中…" : "登录" }}<AppIcon name="arrow" :size="17" />
      </button>
      <div class="demo-accounts">
        <p class="muted">课程演示账号 · 点击填入</p>
        <div>
          <button
            v-for="account in accounts"
            :key="account.id"
            type="button"
            class="button secondary"
            @click="
              form.account = account.id;
              form.password = 'Campus@123';
              error = '';
            "
          >
            {{ account.label }}
          </button>
        </div>
        <p class="muted">演示账号密码：Campus@123；填写后点击登录。</p>
      </div>
    </form>
  </section>
</template>
