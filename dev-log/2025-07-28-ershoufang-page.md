# 新增二手房页面

## 后端修改
`backend/src/main/java/com/fang123/controller/LoupanPublicController.java`
- `/api/public/loupans` 接口新增 `salesStatus` 查询参数（逗号分隔多值，如 `3,4`）
- 支持 `w.in()` 多值筛选

## 前端修改

### 新增页面
`web/src/views/ErShouFang.vue`
- 复用 Home.vue 的卡片布局风格
- 默认筛选 `salesStatus=3,4`（售罄+已交付）
- 橙色/红色主题色，区别于新盘的蓝色
- 支持搜索关键词、行政区、板块、类型、装修筛选
- 分页支持

### 路由
`web/src/router/index.js`
- 新增 `/ershoufang` 路由 → `ErShouFang.vue`

### 导航栏
`web/src/layouts/WebLayout.vue`
- "二手房"链接从 `/#list` 改为 `/ershoufang`
