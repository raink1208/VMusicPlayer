// https://nuxt.com/docs/api/configuration/nuxt-config
import { fileURLToPath } from 'node:url'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },

  // SSG設定: 静的サイト生成モード
  ssr: false,

  alias: {
    '~': fileURLToPath(new URL('./', import.meta.url)),
  },
  css: [
    '~/assets/css/reset.css'
  ],
  runtimeConfig: {
    public: {
      apiBase: process.env.API_BASE_URL || 'http://localhost:8080'
    }
  },
  typescript: {
    strict: true,
    typeCheck: false
  },
  vite: {
    optimizeDeps: {
      include: ['vuedraggable', 'sortablejs']
    }
  },

  // Nitroプリレンダリング設定
  nitro: {
    prerender: {
      crawlLinks: true,
      routes: ['/']
    }
  }
})
