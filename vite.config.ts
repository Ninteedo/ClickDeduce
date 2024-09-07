/// <reference types="vitest" />
import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [scalaJSPlugin(), react()],
  build: {
    outDir: 'build',
    rollupOptions: {
      input: {
        index: 'webapp/pages/index.html',
        guide: 'webapp/pages/guide.html',
      }
    }
  },
  test: {
    environment: 'jsdom'
  }
})
