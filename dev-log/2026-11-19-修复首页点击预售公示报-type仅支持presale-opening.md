# 2026-11-19 修复首页点击「预售公示」提示「type 仅支持 presale / opening」

## 现象

首页 Hero 区点击「预售公示」/「最新开盘」按钮，弹出后端错误：`type 仅支持 presale / opening`。

## 定位

用 Playwright 观察实际请求，发现点击后发出的是：

```
GET /api/public/loupan-spotlight?type=&page=1&size=12
```

`type` 是空字符串，命中后端 `LoupanSpotlightController#list` 的入参校验，返回该提示。

### 根因

`components/LoupanListSection.vue` 中 `spotlightType` 是指向父组件 `spotlight` prop 的 computed：

```js
const spotlightType = computed({
  get: () => props.spotlight,
  set: (v) => emit('update:spotlight', v)
})
```

而 `toggleSpotlight()` 先写 `spotlightType.value = type`（emit 给父组件），**紧接着**调用 `fetchSpotlight()`，后者读 `spotlightType.value` 作为请求参数：

```js
spotlightType.value = type
pg.current = 1
fetchSpotlight()   // 读 computed → 仍是旧值 ''
```

父组件收到 emit 后更新的是它自己的 ref，子组件 props 要等下一次渲染才同步，所以同一 tick 内读到旧值 `''`，请求 `type=` 为空。

同类问题还有两处：

- `clearSpotlight()`：`spotlightType.value = ''` 后调 `fetchData()`，`fetchData` 里 `if (spotlightType.value) return fetchSpotlight()` 会读到旧值 `'presale'`，导致「返回全部楼盘」时仍按板块查询
- `doSearch()`：重置板块后调 `fetchData()`，同样可能仍走 spotlight 分支

## 修复（仅 `web/src/components/LoupanListSection.vue`）

1. 拆分数据加载：
   - `fetchLoupanList()`：普通列表（含首页 `loupan-home` 分组模式）
   - `fetchSpotlight(type = spotlightType.value)`：板块列表，**type 由调用方显式传入**
2. `fetchData()` 保留自动分流（初始加载、分页使用，此时 computed 已稳定）
3. `toggleSpotlight(type)` → `fetchSpotlight(type)`
4. `clearSpotlight()` → `fetchLoupanList()`
5. `doSearch()` → `fetchLoupanList()`

并在 `fetchSpotlight` 上补注释说明「computed 刚 emit 时同 tick 读到旧值」的坑，避免后续再踩。

## 验证

Playwright 点击按钮并抓包（后端未启动，仅校验请求参数）：

| 操作 | 请求 |
| --- | --- |
| 点击「预售公示」 | `/api/public/loupan-spotlight?type=presale&page=1&size=12`（修复前为 `type=`） |
| 点击「最新开盘」 | `/api/public/loupan-spotlight?type=opening&page=1&size=12` |
| 再次点击已选中的「最新开盘」 | `/api/public/loupan-home?page=1&size=12` |
| 点击「返回全部楼盘」 | `/api/public/loupan-home?page=1&size=12`（修复前会继续请求 spotlight） |

- `npx vite build` 通过（9.33s）

## 备注

- 本机 MySQL（127.0.0.1:3306）未启动，后端接口返回 500，本次仅验证请求参数是否正确，未做端到端数据验证
- 验证时启动的 web dev server（4000 端口）未关闭，如需清理：`kill $(lsof -nP -iTCP:4000 -sTCP:LISTEN -t)`
