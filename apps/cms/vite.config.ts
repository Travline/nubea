import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss()
  ],
  server: {
    proxy: {
      '/api': { target: process.env.BACKEND_URL || 'http://localhost:8080', changeOrigin: true },
      '/logout': { target: process.env.BACKEND_URL || 'http://localhost:8080', changeOrigin: true },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
})
