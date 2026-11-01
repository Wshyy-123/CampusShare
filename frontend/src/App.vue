<script setup lang="ts">
import { ref } from 'vue'

const message = ref('等待刷卡...')
const isLoading = ref(false)

const simulateSwipe = async () => {
  isLoading.value = true
  message.value = '正在验证预约信息...'
  try {
    const response = await fetch('/api/v1/library/check-in?userId=1', {
      method: 'POST'
    })
    const text = await response.text()
    message.value = text
  } catch (error) {
    message.value = '网络错误，请检查后端是否启动！'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="page-bg">
    <div class="glass-card">
      <h1 class="title">图书馆刷卡终端</h1>
      <div class="status-box">
        <p class="status-text">{{ message }}</p>
      </div>
      <button class="swipe-btn" @click="simulateSwipe" :disabled="isLoading">
        {{ isLoading ? '识别中...' : '滴！刷卡' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.page-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at top, #1e1e24, #0d0d11);
  color: #fff;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}
.glass-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(16px);
  padding: 40px;
  border-radius: 24px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0,0,0,0.4);
  max-width: 400px;
  width: 90%;
}
.title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 24px;
  color: #e2e2e2;
}
.status-box {
  background: rgba(0, 0, 0, 0.3);
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 32px;
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.status-text {
  font-size: 16px;
  color: #a1a1aa;
}
.swipe-btn {
  width: 100%;
  padding: 16px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #42b883, #347474);
  color: white;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.swipe-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(66, 184, 131, 0.4);
}
.swipe-btn:disabled {
  background: #333;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
</style>