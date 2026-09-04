import { createApp } from 'vue'
import { createPinia } from 'pinia'
import TDesign from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'
import './styles/global.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

// 全局渲染错误处理：页面顶部显示错误条，避免白屏无法定位
app.config.errorHandler = (err) => {
  console.error('[全局渲染错误]', err)
  const msg = (err && err.message) ? err.message : String(err)
  try {
    let bar = document.getElementById('vue-error-bar')
    if (!bar) {
      bar = document.createElement('div')
      bar.id = 'vue-error-bar'
      bar.style.cssText = 'position:fixed;top:0;left:0;right:0;z-index:99999;background:#fde2e2;color:#d54941;font-size:12px;padding:8px 12px;border-bottom:1px solid #f5c6c6;white-space:pre-wrap;max-height:40vh;overflow:auto;'
      document.body.appendChild(bar)
    }
    bar.textContent = '页面出错：' + msg
  } catch {}
}

app.use(pinia)
app.use(router)
app.use(TDesign)

app.mount('#app')
