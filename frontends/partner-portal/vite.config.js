import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

const sharedDir = fileURLToPath(new URL('../shared/design-system', import.meta.url))

export default defineConfig({
  base: process.env.VITE_BASE_PATH ? process.env.VITE_BASE_PATH + '/' : '/',
  plugins: [react()],
  resolve: {
    alias: {
      '@design-system': sharedDir,
    },
    dedupe: ['react', 'react-dom', 'react-router-dom'],
  },
  server: {
    port: 5173,
    fs: { allow: ['.', sharedDir] },
  },
})
