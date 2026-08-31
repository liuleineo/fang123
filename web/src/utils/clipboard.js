/**
 * 兼容各平台的复制文本方法
 * - 优先使用 Clipboard API（现代浏览器，需 HTTPS 安全上下文）
 * - 降级使用 execCommand('copy') + 临时 textarea（兼容鸿蒙/微信 WebView 等不支持 Clipboard API 的环境）
 */
export function copyText(text) {
  return new Promise((resolve, reject) => {
    if (!text) {
      reject(new Error('无内容可复制'))
      return
    }
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(resolve).catch(() => fallback(text, resolve, reject))
      return
    }
    fallback(text, resolve, reject)
  })
}

function fallback(text, resolve, reject) {
  try {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.setAttribute('readonly', '')
    ta.style.position = 'fixed'
    ta.style.top = '-9999px'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    // 兼容 iOS/鸿蒙：选中范围后再复制
    const range = document.createRange()
    range.selectNodeContents(ta)
    const sel = window.getSelection()
    sel.removeAllRanges()
    sel.addRange(range)
    ta.setSelectionRange(0, text.length)
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    if (ok) resolve()
    else reject(new Error('复制失败'))
  } catch (e) {
    reject(e)
  }
}
