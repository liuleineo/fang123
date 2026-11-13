<template>
  <div class="xinpan-page pb-6">
    <!-- 标题 + 筛选 -->
    <section class="bg-white border-b border-gray-50">
      <div class="section-container py-5">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 class="text-xl sm:text-2xl font-bold text-[var(--color-text-primary)] flex items-center gap-2">
              <Table2 class="w-6 h-6 text-[var(--color-primary)]" />
              新盘信息
            </h1>
            <p class="text-sm text-[var(--color-text-tertiary)] mt-1">杭州新盘关键信息一览，共 {{ total }} 个{{ salesStatusLabel }}楼盘</p>
          </div>

          <div class="flex flex-wrap items-center gap-2 w-full sm:w-auto">
            <t-select v-model="district" :options="districtOptions" placeholder="全部区域" clearable
                      class="!w-32" @change="onFilterChange" />
            <t-select v-model="salesStatus" :options="salesStatusOptions" placeholder="全部状态" clearable
                      class="!w-32" @change="onFilterChange" />
            <t-input v-model="keyword" placeholder="搜索楼盘 / 品牌" clearable class="!w-48"
                     @enter="onFilterChange" @clear="onFilterChange">
              <template #prefix-icon><Search class="w-4 h-4" /></template>
            </t-input>
            <t-button variant="outline" @click="onFilterChange">查询</t-button>
          </div>
        </div>
      </div>
    </section>

    <section class="section-container pt-5">
      <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
        <div v-if="loading" class="py-16 text-center"><t-loading /></div>

        <div v-else-if="!list.length" class="py-16 text-center">
          <p class="text-sm text-[var(--color-text-tertiary)]">暂无符合条件的楼盘</p>
          <t-button v-if="keyword || district || salesStatus !== '1'" variant="text" theme="primary" class="!mt-2"
                    @click="resetFilter">清空筛选</t-button>
        </div>

        <template v-else>
          <div class="hidden sm:block overflow-x-auto">
            <table class="xinpan-table">
              <colgroup>
                <col class="c-district" />
                <col class="c-name" />
                <col class="c-plate" />
                <col class="c-brand" />
                <col class="c-area" />
                <col class="c-price" />
                <col class="c-total" />
                <col class="c-fee" />
                <col />
                <col />
              </colgroup>
              <thead>
                <tr>
                  <th>区域</th>
                  <th>楼盘名称</th>
                  <th>板块</th>
                  <th>开发商品牌</th>
                  <th>面积段</th>
                  <th>单价</th>
                  <th>总价范围</th>
                  <th>物业费</th>
                  <th>优点</th>
                  <th>缺点</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="lp in list" :key="lp.id">
                  <td class="whitespace-nowrap">{{ lp.district || '-' }}</td>
                  <td>
                    <router-link :to="`/loupan/${lp.encodedId}`" class="name-link block truncate"
                                 :title="lp.projectName">
                      {{ lp.projectName }}
                    </router-link>
                  </td>
                  <td class="truncate" :title="lp.plate || ''">{{ lp.plate || '-' }}</td>
                  <td>
                    <p class="cell-brand" :title="lp.brandList || ''">{{ lp.brandList || '-' }}</p>
                  </td>
                  <td class="whitespace-nowrap">{{ areaText(lp) }}</td>
                  <td class="price">{{ unitPriceText(lp) }}</td>
                  <td class="price">{{ totalPriceText(lp) }}</td>
                  <td class="text-[13px] whitespace-nowrap">{{ feeText(lp) }}</td>
                  <td class="align-top">
                    <p class="cell-text" :title="lp.advantage">{{ lp.advantage || '-' }}</p>
                  </td>
                  <td class="align-top">
                    <p class="cell-text" :title="lp.disadvantage">{{ lp.disadvantage || '-' }}</p>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <p class="hidden sm:block lg:hidden text-center text-xs text-[var(--color-text-tertiary)] py-2">← 左右滑动查看完整表格 →</p>

          <!-- 移动端：表格列太多，改为卡片展示 -->
          <div class="sm:hidden divide-y divide-gray-50">
            <div v-for="lp in list" :key="lp.id" class="p-4">
              <div class="flex items-start justify-between gap-2">
                <router-link :to="`/loupan/${lp.encodedId}`" class="name-link text-[15px]">
                  {{ lp.projectName }}
                </router-link>
                <span class="text-xs text-[var(--color-text-tertiary)] shrink-0 pt-0.5">
                  {{ lp.district }}{{ lp.plate ? ' · ' + lp.plate : '' }}
                </span>
              </div>
              <div class="grid grid-cols-2 gap-x-3 gap-y-1 mt-2 text-[13px] text-[var(--color-text-primary)]">
                <p><span class="text-[var(--color-text-tertiary)]">开发商品牌 </span>{{ lp.brandList || '-' }}</p>
                <p><span class="text-[var(--color-text-tertiary)]">面积段 </span>{{ areaText(lp) }}</p>
                <p><span class="text-[var(--color-text-tertiary)]">单价 </span><span class="price-inline">{{ unitPriceText(lp) }}</span></p>
                <p><span class="text-[var(--color-text-tertiary)]">总价 </span><span class="price-inline">{{ totalPriceText(lp) }}</span></p>
                <p class="col-span-2"><span class="text-[var(--color-text-tertiary)]">物业费 </span>{{ feeText(lp) }}</p>
              </div>
              <p v-if="lp.advantage" class="mt-2 text-[13px] leading-5 text-[var(--color-text-secondary)]">
                <span class="text-emerald-600 font-medium">优点 </span>{{ lp.advantage }}
              </p>
              <p v-if="lp.disadvantage" class="mt-1 text-[13px] leading-5 text-[var(--color-text-secondary)]">
                <span class="text-rose-500 font-medium">缺点 </span>{{ lp.disadvantage }}
              </p>
            </div>
          </div>

          <div class="flex justify-center sm:justify-end px-4 py-4 border-t border-gray-50">
            <t-pagination
              :current="page"
              :total="total"
              :page-size="size"
              :page-size-options="[20, 50, 100, 200]"
              :show-page-size="true"
              @current-change="onPageChange"
              @page-size-change="onSizeChange" />
          </div>
        </template>
      </div>

      <p class="text-xs text-[var(--color-text-tertiary)] mt-3 leading-5">
        数据来源于公开信息整理，仅供参考；实际价格、物业费以售楼处公示及网签合同为准。
      </p>
    </section>

    <!-- 生成海报：右下角悬浮按钮 -->
    <button v-if="list.length && !loading" type="button" class="poster-fab" @click="openPoster">
      <ImageIcon class="w-4 h-4" />
      <span>生成海报</span>
    </button>

    <t-dialog v-model:visible="posterVisible" header="新盘信息海报" width="420px" :footer="false" :close-on-overlay-click="false">
      <div v-if="posterGenerating" class="flex flex-col items-center justify-center gap-3 py-12">
        <t-loading />
        <p class="text-sm text-[var(--color-text-tertiary)]">正在生成海报…</p>
      </div>
      <div v-else-if="posterUrl" class="space-y-3">
        <img :src="posterUrl" class="poster-preview" alt="新盘信息海报" />
        <p class="text-xs text-[var(--color-text-tertiary)] text-center">手机端长按图片可保存到相册</p>
        <div class="flex gap-2">
          <t-button class="flex-1" variant="outline" @click="posterVisible = false">关闭</t-button>
          <t-button class="flex-1" theme="primary" @click="downloadPoster">保存图片</t-button>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Table2, Search, Image as ImageIcon } from 'lucide-vue-next'
import { MessagePlugin } from 'tdesign-vue-next'
import request from '@/utils/request'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const keyword = ref('')
const district = ref('')
const salesStatus = ref('1')  // 默认只看在售楼盘
const districts = ref([])

const districtOptions = computed(() => districts.value.map(d => ({ label: d, value: d })))

// 销售状态：0 待售 / 1 在售 / 2 售罄 / 3 已交付（与 Loupan.salesStatus 一致）
const salesStatusOptions = [
  { label: '在售', value: '1' },
  { label: '待售', value: '0' },
  { label: '售罄', value: '2' },
 
]

/** 副标题用：当前状态下显示「共 N 个在售楼盘」 */
const salesStatusLabel = computed(() =>
  salesStatusOptions.find(o => o.value === salesStatus.value)?.label || ''
)

async function fetchFilters() {
  try {
    const r = await request.get('/public/loupan-filters')
    districts.value = r?.districts || []
  } catch {
    districts.value = []
  }
}

async function fetchList() {
  loading.value = true
  try {
    // orderByDistrict：按杭州行政区官方顺序分组展示
    const params = { page: page.value, size: size.value, orderByDistrict: true }
    if (keyword.value) params.keyword = keyword.value
    if (district.value) params.district = district.value
    if (salesStatus.value !== '') params.salesStatus = salesStatus.value
    const r = await request.get('/public/loupans', { params })
    list.value = r?.records || []
    total.value = r?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  fetchList()
}

function resetFilter() {
  keyword.value = ''
  district.value = ''
  salesStatus.value = '1'
  onFilterChange()
}

function onPageChange(p) {
  page.value = p
  fetchList()
}

function onSizeChange(s) {
  size.value = s
  page.value = 1
  fetchList()
}

// ===== 单元格格式化 =====
function areaText(lp) {
  const a = Number(lp.areaMin || 0)
  const b = Number(lp.areaMax || 0)
  if (!a && !b) return '-'
  if (!b || a === b) return `${a || b}㎡`
  return `${a}-${b}㎡`
}

/** 高层均价为空时，回退到洋房/叠墅/排屋均价（与首页口径一致） */
function unitPriceText(lp) {
  const p = lp.avgUnitPrice || lp.avgUnitPriceYangfang || lp.avgUnitPriceDieshu || lp.avgUnitPricePaiwu
  return p ? `${Number(p).toLocaleString()}元/㎡` : '-'
}

function totalPriceText(lp) {
  const a = Number(lp.minTotalPrice || 0)
  const b = Number(lp.maxTotalPrice || 0)
  if (!a && !b) return '-'
  if (!b || a === b) return `${a || b}万`
  return `${a}-${b}万`
}

function feeText(lp) {
  const h = Number(lp.propertyFeeHigh || 0)
  const v = Number(lp.propertyFeeVilla || 0)
  if (h && v && h !== v) return `${h}-${v}元/㎡·月`
  const one = h || v
  return one ? `${one}元/㎡·月` : '-'
}

// ===== 新盘信息海报：Canvas 手绘当前页表格（不依赖截图库，文字更清晰）=====
/** 海报最多展示条数，避免画布过大（iOS Safari 单画布面积上限约 1670 万像素） */
const POSTER_MAX_ROWS = 30
const posterVisible = ref(false)
const posterGenerating = ref(false)
const posterUrl = ref('')

const POSTER_FONT = '"PingFang SC","Hiragino Sans GB","Microsoft YaHei","Helvetica Neue",Arial,sans-serif'
const posterFont = (size, weight = 'normal') => `${weight} ${size}px ${POSTER_FONT}`
const posterPad2 = n => String(n).padStart(2, '0')
function posterDate() {
  const d = new Date()
  return `${d.getFullYear()}-${posterPad2(d.getMonth() + 1)}-${posterPad2(d.getDate())}`
}
function posterDateTime() {
  const d = new Date()
  return `${posterDate()} ${posterPad2(d.getHours())}:${posterPad2(d.getMinutes())}`
}

/** 单行截断为「…」 */
function posterEllipsis(ctx, text, font, maxWidth) {
  ctx.font = font
  const str = String(text ?? '')
  if (ctx.measureText(str).width <= maxWidth) return str
  let t = str
  while (t.length > 1 && ctx.measureText(t + '…').width > maxWidth) t = t.slice(0, -1)
  return t + '…'
}

/** 按宽度逐字换行，返回全部行 */
function posterWrapAll(ctx, text, font, maxWidth) {
  ctx.font = font
  const str = String(text ?? '').replace(/\s+/g, ' ').trim()
  if (!str) return []
  const lines = []
  let line = ''
  for (const ch of str) {
    if (line && ctx.measureText(line + ch).width > maxWidth) {
      lines.push(line)
      line = ch
    } else {
      line += ch
    }
  }
  if (line) lines.push(line)
  return lines
}

/** 海报列定义：宽度合计 1736，画布宽 1800 */
const POSTER_COLS = [
  { title: '区域', w: 84, align: 'left', size: 13, color: '#4B5563', get: lp => lp.district || '-' },
  { title: '楼盘名称', w: 210, align: 'left', size: 15, weight: 'bold', color: '#0052D9', get: lp => lp.projectName || '-' },
  { title: '板块', w: 100, align: 'left', size: 13, color: '#4B5563', get: lp => lp.plate || '-' },
  { title: '开发商品牌', w: 150, align: 'left', size: 12, color: '#4B5563', get: lp => lp.brandList || '-', wrap: true, maxLines: 2 },
  { title: '面积段', w: 110, align: 'left', size: 13, color: '#4B5563', get: lp => areaText(lp) },
  { title: '单价', w: 130, align: 'right', size: 14, weight: 'bold', color: '#E34D59', get: lp => unitPriceText(lp) },
  { title: '总价范围', w: 120, align: 'right', size: 14, weight: 'bold', color: '#E34D59', get: lp => totalPriceText(lp) },
  { title: '物业费', w: 150, align: 'left', size: 12, color: '#4B5563', get: lp => feeText(lp) },
  { title: '优点', w: 341, align: 'left', size: 12, color: '#5B6472', get: lp => lp.advantage || '-', wrap: true, maxLines: 3 },
  { title: '缺点', w: 341, align: 'left', size: 12, color: '#5B6472', get: lp => lp.disadvantage || '-', wrap: true, maxLines: 3 },
]

/** 生成海报 dataURL */
function buildXinpanPoster() {
  const rows = list.value.slice(0, POSTER_MAX_ROWS)
  if (!rows.length) throw new Error('当前没有可展示的楼盘')

  const PAD = 32
  const TITLE_H = 148
  const HEAD_H = 52
  const FOOTER_H = 62
  const LINE_H = 17
  const CELL_PAD_Y = 12
  const CELL_PAD_X = 10

  const tableW = POSTER_COLS.reduce((a, c) => a + c.w, 0)
  const W = tableW + PAD * 2
  const tableX = PAD
  const tableY = TITLE_H + 26

  // 先测量排版（用离屏 canvas 只做 measureText）
  const measure = document.createElement('canvas').getContext('2d')
  if (!measure) throw new Error('当前浏览器不支持 Canvas')

  const prepared = rows.map(lp => {
    const cells = POSTER_COLS.map(col => {
      const font = posterFont(col.size, col.weight)
      const maxWidth = col.w - CELL_PAD_X * 2
      const raw = String(col.get(lp) ?? '').trim() || '-'
      if (!col.wrap) return [posterEllipsis(measure, raw, font, maxWidth)]
      const all = posterWrapAll(measure, raw, font, maxWidth)
      const max = col.maxLines || 2
      const lines = all.slice(0, max)
      if (all.length > lines.length) {
        let last = lines[lines.length - 1]
        while (last.length > 1 && measure.measureText(last + '…').width > maxWidth) last = last.slice(0, -1)
        lines[lines.length - 1] = last + '…'
      }
      return lines
    })
    const lineCount = Math.max(1, ...cells.map(c => c.length))
    return { cells, h: Math.max(56, lineCount * LINE_H + CELL_PAD_Y * 2) }
  })

  const tableH = HEAD_H + prepared.reduce((a, r) => a + r.h, 0)
  const H = tableY + tableH + FOOTER_H

  // 倍率自适应：按面积逐级降档，避免超过浏览器画布上限得到空白图
  let scale = 2
  const MAX_AREA = 12000000
  while (scale > 1 && W * H * scale * scale > MAX_AREA) scale = Math.max(1, scale - 0.25)

  const canvas = document.createElement('canvas')
  canvas.width = Math.round(W * scale)
  canvas.height = Math.round(H * scale)
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('当前浏览器不支持 Canvas')
  ctx.scale(scale, scale)
  ctx.textBaseline = 'middle'

  // 背景
  ctx.fillStyle = '#FFFFFF'
  ctx.fillRect(0, 0, W, H)

  // 标题区
  const grad = ctx.createLinearGradient(0, 0, W, TITLE_H)
  grad.addColorStop(0, '#0052D9')
  grad.addColorStop(1, '#3C7BFF')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, W, TITLE_H)

  ctx.textAlign = 'left'
  ctx.font = posterFont(36, 'bold')
  ctx.fillStyle = '#FFFFFF'
  ctx.fillText('杭州新盘信息', PAD, 60)

  const subParts = []
  if (salesStatusLabel.value) subParts.push(salesStatusLabel.value)
  subParts.push(district.value || '全杭州')
  if (keyword.value) subParts.push(`关键词：${keyword.value}`)
  ctx.font = posterFont(18)
  ctx.fillStyle = 'rgba(255,255,255,0.94)'
  ctx.fillText(subParts.join(' · '), PAD, 104)

  ctx.textAlign = 'right'
  ctx.font = posterFont(14)
  ctx.fillStyle = 'rgba(255,255,255,0.9)'
  ctx.fillText(posterDate(), W - PAD, 60)
  ctx.font = posterFont(12)
  ctx.fillStyle = 'rgba(255,255,255,0.78)'
  ctx.fillText('杭房一二三 · hangfang123.com', W - PAD, 88)
  ctx.fillText(
    prepared.length < total.value
      ? `共 ${total.value} 个楼盘 · 海报展示前 ${prepared.length} 个`
      : `共 ${prepared.length} 个楼盘`,
    W - PAD, 118
  )

  // 表头
  ctx.fillStyle = '#F5F7FB'
  ctx.fillRect(tableX, tableY, tableW, HEAD_H)
  let hx = tableX
  POSTER_COLS.forEach(col => {
    ctx.font = posterFont(13, '500')
    ctx.fillStyle = '#6B7280'
    ctx.textAlign = col.align === 'right' ? 'right' : 'left'
    ctx.fillText(col.title, col.align === 'right' ? hx + col.w - CELL_PAD_X : hx + CELL_PAD_X, tableY + HEAD_H / 2)
    hx += col.w
  })

  // 数据行
  let rowY = tableY + HEAD_H
  prepared.forEach((r, ri) => {
    if (ri % 2 === 1) {
      ctx.fillStyle = '#FAFBFD'
      ctx.fillRect(tableX, rowY, tableW, r.h)
    }
    let x = tableX
    POSTER_COLS.forEach((col, ci) => {
      const lines = r.cells[ci]
      ctx.font = posterFont(col.size, col.weight)
      ctx.fillStyle = col.color
      ctx.textAlign = col.align === 'right' ? 'right' : 'left'
      const tx = col.align === 'right' ? x + col.w - CELL_PAD_X : x + CELL_PAD_X
      let cy = rowY + (r.h - lines.length * LINE_H) / 2 + LINE_H / 2
      lines.forEach(line => {
        ctx.fillText(line, tx, cy)
        cy += LINE_H
      })
      x += col.w
    })
    rowY += r.h
  })

  // 网格线
  ctx.strokeStyle = '#E9EDF3'
  ctx.lineWidth = 1
  ctx.beginPath()
  let gy = tableY
  ctx.moveTo(tableX, gy + 0.5)
  ctx.lineTo(tableX + tableW, gy + 0.5)
  gy += HEAD_H
  ctx.moveTo(tableX, gy + 0.5)
  ctx.lineTo(tableX + tableW, gy + 0.5)
  prepared.forEach(r => {
    gy += r.h
    ctx.moveTo(tableX, gy + 0.5)
    ctx.lineTo(tableX + tableW, gy + 0.5)
  })
  let vx = tableX
  ctx.moveTo(vx + 0.5, tableY)
  ctx.lineTo(vx + 0.5, tableY + tableH)
  POSTER_COLS.forEach(col => {
    vx += col.w
    ctx.moveTo(vx - 0.5, tableY)
    ctx.lineTo(vx - 0.5, tableY + tableH)
  })
  ctx.stroke()

  // 页脚
  ctx.textAlign = 'left'
  ctx.font = posterFont(11)
  ctx.fillStyle = '#9AA3B2'
  ctx.fillText('数据来源：杭房一二三 · hangfang123.com', PAD, H - 40)
  ctx.fillText('数据来源于公开信息整理，仅供参考；实际价格、物业费以售楼处公示及网签合同为准', PAD, H - 22)
  ctx.textAlign = 'right'
  ctx.fillText(`生成时间 ${posterDateTime()}`, W - PAD, H - 40)

  return canvas.toDataURL('image/png')
}

async function openPoster() {
  if (!list.value.length) {
    MessagePlugin.warning('当前没有可生成海报的数据')
    return
  }
  posterVisible.value = true
  posterGenerating.value = true
  posterUrl.value = ''
  // 先让弹窗渲染出 loading，再同步绘制，避免点击后界面无反馈
  await new Promise(r => setTimeout(r, 30))
  try {
    posterUrl.value = buildXinpanPoster()
  } catch (e) {
    console.error('[新盘信息海报] 生成失败:', e)
    MessagePlugin.error('海报生成失败：' + (e?.message || '请稍后重试'))
    posterVisible.value = false
  } finally {
    posterGenerating.value = false
  }
}

// 关闭弹窗后释放大图（dataURL 可能几 MB），延迟到关闭动画结束
watch(posterVisible, v => {
  if (v) return
  setTimeout(() => { if (!posterVisible.value) posterUrl.value = '' }, 300)
})

function downloadPoster() {
  if (!posterUrl.value) return
  const a = document.createElement('a')
  a.href = posterUrl.value
  a.download = `杭州新盘信息-${posterDate()}.png`
  document.body.appendChild(a)
  a.click()
  a.remove()
}

onMounted(() => {
  fetchFilters()
  fetchList()
})
</script>

<style scoped>
.xinpan-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 14px;
  color: var(--color-text-primary);
}
/* 列宽：8 个固定列 + 优点/缺点均分剩余空间，容器内不产生横向滚动 */
.xinpan-table .c-district { width: 70px; }
.xinpan-table .c-name { width: 176px; }
.xinpan-table .c-plate { width: 92px; }
.xinpan-table .c-brand { width: 118px; }
.xinpan-table .c-area { width: 94px; }
.xinpan-table .c-price { width: 106px; }
.xinpan-table .c-total { width: 110px; }
.xinpan-table .c-fee { width: 134px; }

.xinpan-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 12px 10px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
  background: #f5f7fb;
  border-bottom: 1px solid #e9edf3;
  white-space: nowrap;
}
.xinpan-table tbody td {
  padding: 12px 10px;
  border-bottom: 1px solid #f2f4f7;
  vertical-align: middle;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
}
.xinpan-table th:first-child,
.xinpan-table td:first-child {
  padding-left: 16px;
}
.xinpan-table th:last-child,
.xinpan-table td:last-child {
  padding-right: 16px;
}

/* 1024–1279：收紧列宽与字号，保证 PC 端仍无横向滚动 */
@media (min-width: 1024px) and (max-width: 1279px) {
  .xinpan-table {
    font-size: 13px;
  }
  .xinpan-table .c-district { width: 60px; }
  .xinpan-table .c-name { width: 140px; }
  .xinpan-table .c-plate { width: 86px; }
  .xinpan-table .c-brand { width: 100px; }
  .xinpan-table .c-area { width: 86px; }
  .xinpan-table .c-price { width: 100px; }
  .xinpan-table .c-total { width: 104px; }
  .xinpan-table .c-fee { width: 132px; }
  .xinpan-table thead th,
  .xinpan-table tbody td {
    padding-left: 8px;
    padding-right: 8px;
  }
  .xinpan-table th:first-child,
  .xinpan-table td:first-child {
    padding-left: 12px;
  }
  .xinpan-table th:last-child,
  .xinpan-table td:last-child {
    padding-right: 14px;
  }
}

/* 平板（<1024）：列太多，保留横向滚动 + 提示 */
@media (max-width: 1023px) {
  .xinpan-table {
    table-layout: auto;
    min-width: 980px;
  }
}
.xinpan-table tbody tr:hover td {
  background: #f7faff;
}
.xinpan-table tbody tr:last-child td {
  border-bottom: 0;
}
.name-link {
  color: var(--color-primary);
  font-weight: 600;
  text-decoration: none;
}
.name-link:hover {
  text-decoration: underline;
}
.price,
.price-inline {
  color: #e34d59;
  font-weight: 600;
  white-space: nowrap;
}
.cell-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}
/* 品牌可能多个（如「滨江集团/绿城中国」），最多两行 */
.cell-brand {
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}
/* 新盘信息：右下角悬浮「生成海报」按钮 */
.poster-fab {
  position: fixed;
  right: 16px;
  bottom: calc(24px + env(safe-area-inset-bottom, 0px));
  z-index: 40;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 0;
  outline: none;
  border-radius: 9999px;
  background: linear-gradient(135deg, #0052d9, #3c7bff);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 82, 217, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.poster-fab:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(0, 82, 217, 0.42);
}
.poster-fab:active {
  transform: translateY(0);
}

/* 海报预览：等比缩放，长图可滚动查看 */
.poster-preview {
  display: block;
  width: 100%;
  max-height: 60vh;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #eef0f4;
  background: #f7f8fa;
}
</style>
