<script setup lang="ts">
import { ref } from 'vue'

const message = ref('等待操作...')
const isLoading = ref(false)

// 1. 刷卡请求 (XIA-10 标准1和3)
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

// 2. 取消预约请求 (XIA-10 标准2)
const cancelReservation = async () => {
  isLoading.value = true
  message.value = '正在尝试取消预约...'
  try {
    // 这里假设要取消的预约 id 是 1，具体根据你的测试数据来
    const response = await fetch('/api/v1/library/cancel?id=1', {
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
      <h1 class="title">图书馆座位终端</h1>
      <div class="status-box">
        <p class="status-text">{{ message }}</p>
      </div>

      <div class="btn-group">
        <button class="action-btn swipe" @click="simulateSwipe" :disabled="isLoading">
          {{ isLoading ? '处理中...' : '🔴 模拟刷卡' }}
        </button>

        <button class="action-btn cancel" @click="cancelReservation" :disabled="isLoading">
          {{ isLoading ? '处理中...' : '❌ 取消预约' }}
        </button>
      </div>
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
  margin-bottom: 24px;
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.status-text {
  font-size: 16px;
  color: #a1a1aa;
}
.btn-group {
  display: flex;
  gap: 12px;
}
.action-btn {
  flex: 1;
  padding: 12px;
  border-radius: 12px;
  border: none;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.swipe {
  background: linear-gradient(135deg, #42b883, #347474);
}
.cancel {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
}
.action-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
}
.action-btn:disabled {
  background: #333;
  cursor: not-allowed;
  transform: none;
}
</style>
