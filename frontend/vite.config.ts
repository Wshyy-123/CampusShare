import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        // 本机 8080 已被其他进程占用；后端默认使用 8081，可用 CAMPUSSHARE_PORT 覆盖。
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
