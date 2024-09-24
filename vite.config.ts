/// <reference types="vitest" />
import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [scalaJSPlugin(), react()],
  root: 'webapp',
  build: {
    outDir: 'build',
    rollupOptions: {
      input: {
        index: 'webapp/index.html',
        guide: 'webapp/guide.html',
      },
      output: {
        entryFileNames: 'bundle.js',
        chunkFileNames: '[name].js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name && assetInfo.name.endsWith('.css')) {
            return 'bundle.css'
          }
          return 'bundle-[name].[ext]'
        }
      }
    }
  },
  test: {
    environment: 'jsdom'
  }
})
