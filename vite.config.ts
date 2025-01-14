/// <reference types="vitest" />
import {defineConfig} from 'vite'
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [scalaJSPlugin()],
  build: {
    outDir: 'webapp/build',
    rollupOptions: {
      input: {
        index: 'index.html',
        guide: 'guide.html',
      },
      output: {
        // entryFileNames: 'bundle.js',
        // chunkFileNames: '[name].js',
        // assetFileNames: (assetInfo) => {
        //   if (assetInfo.name && assetInfo.name.endsWith('.css')) {
        //     return 'bundle.css'
        //   }
        //   return 'bundle-[name].[ext]'
        // }
      }
    }
  },
  test: {
    environment: 'jsdom'
  }
})
