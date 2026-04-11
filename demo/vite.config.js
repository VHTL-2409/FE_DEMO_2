import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    tailwindcss(),
    vue()
  ],
  server: {
    host: true,
    port: 5173,
    strictPort: true
  },
  preview: {
    host: true,
    port: 4173,
    strictPort: true
  },
  build: {
    // Target modern browsers for smaller output
    target: 'es2020',
    // Optimize chunk splitting
    rollupOptions: {
      output: {
        // Manual chunks for better caching and code splitting
        manualChunks: (id) => {
          // Vue core
          if (id.includes('node_modules/vue') || id.includes('node_modules/@vue') || id.includes('node_modules/pinia')) {
            return 'vue-vendor'
          }
          // Icons library
          if (id.includes('lucide-vue-next')) {
            return 'icons'
          }
          // WebSocket
          if (id.includes('@stomp/stompjs')) {
            return 'websocket'
          }
        },
        // Chunk file naming
        chunkFileNames: 'assets/chunk-[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]'
      }
    },
    // Enable CSS code splitting
    cssCodeSplit: true,
    // Optimize chunk size warning threshold
    chunkSizeWarningLimit: 600,
    // Enable minification with esbuild (faster than terser)
    minify: 'esbuild',
    // Source maps for production debugging (set to false for smaller output)
    sourcemap: false,
    // Assets inline limit
    assetsInlineLimit: 4096
  },
  // Optimize deps
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'lucide-vue-next']
  }
})