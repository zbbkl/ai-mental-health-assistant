import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    proxy: {
      // 本地后端（backend/src/main/resources/application.yml 中 server.port）
      '/api': {
        target: 'http://localhost:1236',
        changeOrigin: true
      },
      // 上传后的静态文件由后端 /files/** 提供
      '/files': {
        target: 'http://localhost:1236',
        changeOrigin: true
      }
    }
  }
})
