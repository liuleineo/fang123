/**
 * 轻量访问统计埋点（自建，无第三方依赖）。
 *
 * 设计要点：
 * - 只在生产构建（或显式打开 VITE_VISIT_TRACK=true）时上报，本地开发默认不上报，避免污染线上数据；
 * - SPA 下 nginx access log 拿不到页面维度，所以由前端在路由切换后主动上报 page_view；
 * - 事件先入内存队列，合并 3 秒或积攒 10 条再发一次，页面卸载时用 keepalive 请求兜底，尽量不丢数据；
 * - deviceId 存 localStorage 用于匿名去重，登录后后端按 token 关联 user_id，无需前端处理。
 */

const REPORT_URL = '/api/public/visit/report'
const DEVICE_KEY = 'visit_device_id'
const SESSION_KEY = 'visit_session'
const CHANNEL_KEY = 'visit_channel'

/** 30 分钟无事件即视为新会话 */
const SESSION_TIMEOUT = 30 * 60 * 1000
const FLUSH_INTERVAL = 3000
const FLUSH_BATCH = 10
const MAX_QUEUE = 50

const enabled = import.meta.env.PROD || String(import.meta.env.VITE_VISIT_TRACK) === 'true'

let queue = []
let timer = null
let currentRoute = null
let currentRouteAt = 0
/** document.referrer 只在进入站点时有效，会话内跳转不重复上报 */
let referrerReported = false
let listenersBound = false

function uuid() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID().replace(/-/g, '')
  }
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 10)
}

function deviceId() {
  try {
    let id = localStorage.getItem(DEVICE_KEY)
    if (!id) {
      id = uuid()
      localStorage.setItem(DEVICE_KEY, id)
    }
    return id
  } catch {
    return ''
  }
}

function sessionId() {
  const now = Date.now()
  try {
    const cached = JSON.parse(sessionStorage.getItem(SESSION_KEY) || 'null')
    if (cached && cached.id && now - cached.lastAt < SESSION_TIMEOUT) {
      cached.lastAt = now
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(cached))
      return cached.id
    }
  } catch {}
  const id = uuid()
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({ id, lastAt: now }))
  } catch {}
  return id
}

/** 会话内跳转记为 internal，首次进入按 utm / referrer 判定渠道 */
function resolveChannel() {
  try {
    if (sessionStorage.getItem(CHANNEL_KEY)) {
      return 'internal'
    }
    let channel = 'direct'
    const utm = new URL(window.location.href).searchParams.get('utm_source')
    const referrer = document.referrer || ''
    if (utm) {
      channel = utm.slice(0, 64)
    } else if (referrer) {
      const host = new URL(referrer).host
      if (!host) {
        channel = 'direct'
      } else if (host === window.location.host) {
        channel = 'internal'
      } else if (/weixin|wechat|qq\.com/i.test(host)) {
        channel = 'wechat'
      } else if (/baidu|google|bing|sogou|so\.com|sm\.cn/i.test(host)) {
        channel = 'search'
      } else {
        channel = host.slice(0, 64)
      }
    }
    sessionStorage.setItem(CHANNEL_KEY, channel)
    return channel
  } catch {
    return 'direct'
  }
}

/** 把动态 id 归一化，避免 /loupans/123、/loupans/456 撑爆维度 */
function normalizeRoute(path) {
  return String(path || '/').replace(/\/\d+/g, '/:id').slice(0, 255)
}

function enqueue(event) {
  if (!enabled || !event) return
  queue.push(event)
  if (queue.length >= FLUSH_BATCH) {
    flush()
    return
  }
  if (!timer) {
    timer = setTimeout(() => {
      timer = null
      flush()
    }, FLUSH_INTERVAL)
  }
}

function flush() {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
  if (!queue.length) return
  const events = queue
  queue = []

  const headers = { 'Content-Type': 'application/json' }
  let token = null
  try {
    token = localStorage.getItem('token')
  } catch {}
  if (token) {
    // 带 token 时后端会把事件关联到登录用户；sendBeacon 无法自定义请求头，所以统一用 fetch
    headers.Authorization = `Bearer ${token}`
  }

  try {
    fetch(REPORT_URL, {
      method: 'POST',
      headers,
      body: JSON.stringify({ events }),
      // keepalive 让请求在页面卸载后仍能送达（体积限制 64KB，埋点数据远小于此）
      keepalive: true
    }).catch(() => {})
  } catch {}
}

/** 结算当前页停留时长并上报一次 page_view */
function settleCurrentPage() {
  if (!currentRoute) return 0
  const duration = Date.now() - currentRouteAt
  // 超过 30 分钟视为用户切走后又回来，不算作真实停留
  return duration > SESSION_TIMEOUT ? 0 : duration
}

function trackPageView(route) {
  if (!enabled || !route) return

  const duration = settleCurrentPage()
  const path = normalizeRoute(route.path)
  const referrer = referrerReported ? '' : (document.referrer || '').slice(0, 500)
  referrerReported = true

  enqueue({
    type: 'page_view',
    route: path,
    title: (route.meta && route.meta.title) || document.title || '',
    deviceId: deviceId(),
    sessionId: sessionId(),
    platform: 'web',
    referrer,
    channel: resolveChannel(),
    duration,
    ts: Date.now()
  })

  currentRoute = path
  currentRouteAt = Date.now()
}

function bindUnloadFlush() {
  if (listenersBound) return
  listenersBound = true

  // pagehide / visibilitychange 比 beforeunload 更可靠（移动端 Safari 不触发 beforeunload）
  const settleAndFlush = () => {
    const duration = settleCurrentPage()
    if (currentRoute) {
      enqueue({
        type: 'page_view',
        route: currentRoute,
        title: document.title || '',
        deviceId: deviceId(),
        sessionId: sessionId(),
        platform: 'web',
        referrer: '',
        channel: 'internal',
        duration,
        ts: Date.now()
      })
      currentRouteAt = Date.now()
    }
    flush()
  }

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      settleAndFlush()
    }
  })
  window.addEventListener('pagehide', settleAndFlush)
}

/**
 * 注册路由级埋点，在应用入口调用一次。
 * 注意：必须在 app.mount 之前调用，让初始导航也走 afterEach。
 * @param {import('vue-router').Router} router
 */
export function setupVisitTracking(router) {
  if (!enabled) return
  bindUnloadFlush()
  router.afterEach((to) => {
    trackPageView(to)
  })
}

export default { setupVisitTracking }
