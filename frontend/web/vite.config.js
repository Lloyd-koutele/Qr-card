import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En developpement, l'API Spring Boot tourne sur le port 8080
const api = { target: 'http://localhost:8080', changeOrigin: false, xfwd: true }

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': api, '/oauth2': api, '/login/oauth2': api },
  },
})
