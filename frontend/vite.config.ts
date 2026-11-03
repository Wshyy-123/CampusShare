import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端默认跑在 8080；本机端口被占用时可用 VITE_PROXY_TARGET 覆盖，例如
// VITE_PROXY_TARGET=http://127.0.0.1:8081 pnpm run dev
const proxyTarget = process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
      }
    }
  }
})
