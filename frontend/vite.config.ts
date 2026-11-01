import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 把带有 /api 的请求转发给你的后端
        changeOrigin: true,
      }
    }
  }
})