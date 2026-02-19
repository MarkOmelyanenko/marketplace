import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

const sharedDir = fileURLToPath(new URL('../shared/design-system', import.meta.url))

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@design-system': sharedDir,
    },
    dedupe: ['react', 'react-dom', 'react-router-dom'],
  },
  server: {
    port: 5174,
    fs: { allow: ['.', sharedDir] },
  },
})
