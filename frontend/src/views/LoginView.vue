<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../api/http'
import { login } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const form = reactive({ account: 'student01', password: 'Campus@123' })
const submitting = ref(false)
const error = ref('')

const demoAccounts = [
  { account: 'student01', name: '张三', role: '学生' },
  { account: 'student02', name: '李四', role: '学生' },
  { account: 'teacher01', name: '王老师', role: '教师' },
]

function useDemo(account: string) {
  form.account = account
  form.password = 'Campus@123'
  error.value = ''
}

async function submit() {
  if (!form.account.trim() || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await login(form.account.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/resources'
    await router.replace(redirect)
  } catch (exception) {
    error.value = errorMessage(exception)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="login-wrap">
    <div class="card login-card">
      <div class="login-heading">
        <span class="brand-mark brand-mark-large">校</span>
        <div>
          <h1>登录校享</h1>
          <p class="small">预约校园公共场地，查看自己的使用安排。</p>
        </div>
      </div>

      <div class="alert alert-warning">
        这是 XIA-5/XIA-6 联调用的临时登录入口；XIA-1 的完整登录与权限流程完成后应替换本页。
      </div>

      <form class="stack" @submit.prevent="submit">
        <label class="field">
          <span>账号</span>
          <input v-model="form.account" class="input" autocomplete="username" placeholder="例如 student01" />
        </label>
        <label class="field">
          <span>密码</span>
          <input
            v-model="form.password"
            class="input"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </label>

        <div v-if="error" class="alert alert-error">{{ error }}</div>

        <button class="btn btn-primary btn-block" type="submit" :disabled="submitting">
          {{ submitting ? '登录中…' : '登录' }}
        </button>
      </form>

      <div class="demo-block">
        <p class="small">演示账号（密码均为 Campus@123）</p>
        <div class="chip-row">
          <button
            v-for="item in demoAccounts"
            :key="item.account"
            type="button"
            class="chip"
            @click="useDemo(item.account)"
          >
            {{ item.name }} · {{ item.role }} · {{ item.account }}
          </button>
        </div>
      </div>
    </div>

    <aside class="login-aside">
      <h2>这一轮能做什么</h2>
      <ul>
        <li>按资源类型和位置查找开放中的场地或座位。</li>
        <li>选择日期与时段提交预约，服务端检查开放、教学占用和冲突。</li>
        <li>在“我的预约”中查看本人预约及处理后的状态。</li>
      </ul>
      <p class="small">学生和教师都可以使用；资源管理员入口属于其他故事。</p>
    </aside>
  </section>
</template>
