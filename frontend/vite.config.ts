import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * The production build is emitted straight into the Spring Boot static resources so a single
 * `java -jar` serves both the API and the dashboard. During development the Vite dev server
 * proxies /api to the backend on port 8080.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
    chunkSizeWarningLimit: 1200
  }
})
