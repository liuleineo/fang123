/*
 * 杭房一二三 PWA Service Worker
 *
 * 目标：
 * 1. 让站点可被「添加到主屏幕 / 安装」，离线时至少能打开应用外壳；
 * 2. 加速静态资源（带 hash 的 js/css、图片、字体）的二次打开；
 * 3. 绝不缓存接口数据，避免出现「客户资料改了但页面还是旧的」。
 */

const CACHE_VERSION = 'v2'
const STATIC_CACHE = `fang123-static-${CACHE_VERSION}`
const PAGE_CACHE = `fang123-page-${CACHE_VERSION}`
const INDEX_KEY = '/index.html'

/** 安装时预缓存：应用外壳与图标（体积小，失败不影响安装） */
const PRECACHE_URLS = [
  '/manifest.json',
  '/icon-192.png',
  '/icon-512.png',
  '/apple-touch-icon.png'
]

/** 需要做 stale-while-revalidate 的静态资源 */
const STATIC_DESTINATIONS = ['script', 'style', 'image', 'font']

self.addEventListener('install', (event) => {
  event.waitUntil(
    Promise.all([
      caches
        .open(STATIC_CACHE)
        .then((cache) => cache.addAll(PRECACHE_URLS))
        .catch((err) => console.warn('[SW] 预缓存图标失败（忽略）', err)),
      // 预取首页外壳：装完就具备离线打开的能力，不必等第一次导航被拦截
      fetch('/index.html', { cache: 'reload' })
        .then((res) => {
          if (res && res.ok) {
            return caches.open(PAGE_CACHE).then((cache) => cache.put(INDEX_KEY, res))
          }
        })
        .catch((err) => console.warn('[SW] 预取首页外壳失败（忽略）', err))
    ]).then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((k) => k !== STATIC_CACHE && k !== PAGE_CACHE)
            .map((k) => caches.delete(k))
        )
      )
      .then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', (event) => {
  const req = event.request
  if (req.method !== 'GET') return

  const url = new URL(req.url)
  // 跨域资源（COS 图片、高德地图等）交给浏览器自己处理
  if (url.origin !== self.location.origin) return
  // 接口一律走网络，不做缓存
  if (url.pathname.startsWith('/api/')) return

  if (req.mode === 'navigate') {
    event.respondWith(networkFirst(req))
    return
  }

  if (STATIC_DESTINATIONS.includes(req.destination)) {
    event.respondWith(staleWhileRevalidate(req))
  }
})

/**
 * 页面导航：网络优先，成功就刷新缓存；断网时回退到上一次成功缓存的首页外壳。
 */
async function networkFirst(req) {
  const cache = await caches.open(PAGE_CACHE)
  try {
    const res = await fetch(req)
    if (res && res.ok && res.type === 'basic') {
      cache.put(INDEX_KEY, res.clone())
    }
    return res
  } catch (err) {
    const cached = await cache.match(INDEX_KEY)
    if (cached) return cached
    return new Response(
      '<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">' +
        '<title>网络不可用</title><body style="font-family:-apple-system,sans-serif;padding:48px 24px;text-align:center;color:#333">' +
        '<h2 style="font-size:18px">当前网络不可用</h2>' +
        '<p style="color:#888;font-size:14px">请检查网络连接后重试</p>' +
        '<button onclick="location.reload()" style="margin-top:16px;padding:8px 20px;border:0;border-radius:6px;background:#0052D9;color:#fff;font-size:14px">重新加载</button>' +
        '</body>',
      { headers: { 'Content-Type': 'text/html; charset=utf-8' } }
    )
  }
}

/**
 * 静态资源：先用缓存快速响应，同时后台拉新更新缓存。
 * 带 hash 的文件内容不会变，所以这里不存在脏数据风险。
 */
async function staleWhileRevalidate(req) {
  const cache = await caches.open(STATIC_CACHE)
  const cached = await cache.match(req)
  const network = fetch(req)
    .then((res) => {
      if (res && res.ok && res.type === 'basic') cache.put(req, res.clone())
      return res
    })
    .catch(() => null)
  return cached || (await network) || Response.error()
}
