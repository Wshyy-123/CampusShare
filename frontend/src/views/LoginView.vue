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
    <div class="page-intro">
      <span class="eyebrow">WELCOME TO YOUR CAMPUS</span>
      <h1>登录，找到你的<span class="gradient-text">下一站。</span></h1>
      <p>用自己的账户预约校园空间，查看使用安排。</p>
      <div class="rule-box">
        <AppIcon name="shield" />
        <div>
          <strong>会话身份 · 本人预约</strong>
          <p>预约归属以服务端登录账户为准，页面不传入用户编号。</p>
        </div>
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
      <h2>登录校享</h2>
      <p class="muted">连接主后端，查询与保存本人预约。</p>
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
        <p class="muted">已导入演示SQL时，可填入以下虚构账号：</p>
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
        <p class="muted">仅填入表单，仍需后端核验账号与密码。</p>
      </div>
    </form>
  </section>
</template>
