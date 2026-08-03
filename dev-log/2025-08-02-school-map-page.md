# Web 端新增"学校地图"页面

## 后端
`backend/src/main/java/com/fang123/controller/SchoolController.java`
- 新增公开接口 `GET /api/public/schools`，返回全部学校（可按 keyword/schoolType/eduAdminDepartment 筛选），按类型+校区码排序

## 前端
### 新增页面
`web/src/views/SchoolMap.vue`
- 高德地图（v1.4.15），默认杭州中心 [120.15, 30.28]，zoom 11
- 左侧学校列表侧边栏（点击聚焦到地图）
- 彩色圆点标记（按类型着色）：
  - 小学：绿色
  - 初中：蓝色
  - 九年一贯制：紫色
  - 其他：橙色
- 点击标记弹出 InfoWindow（名称、类型、地址、对口初中、电话）
- 支持搜索关键词 / 学校类型 / 行政区筛选
- 地图自动 fitView 适配全部标记

### 路由
`web/src/router/index.js` 新增 `/school-map` → `SchoolMap.vue`

### 导航栏
`web/src/layouts/WebLayout.vue` 新增"学校地图"导航项

## 标记样式演进
1. 数字圆点 → 彩色图标+名称标签 → 图钉定位图标（统一蓝色 #1890ff）
2. **缩放显示名称**：默认只显示图钉；zoom≥16 时显示学校名称标签（监听 zoomchange，跨阈值重新渲染）
3. 图钉颜色已统一为蓝色（不按类型着色，侧边栏列表保留类型色点）

## 坐标纠偏（已修正）
- ~~初版错误地加入 WGS-84→GCJ-02 转换~~
- **实测验证**：竞舟小学数据坐标 (120.103255, 30.284750) 与高德真实坐标 (120.103223, 30.284755) 几乎一致（误差约 3 米）
- **结论**：学校数据坐标本身就是 GCJ-02（高德坐标系），直接展示准确
- **已移除** `wgs84ToGcj02()` 转换，标记/弹窗/聚焦/围栏统一使用原始坐标

## 学区围栏展示
- 点击学校时解析 `map_fence` 字段（`lng,lat;lng,lat` 格式）绘制多边形
- 过滤重复点/无效点，至少 3 个有效点才绘制
- 围栏蓝色半透明填充（stroke #1890ff, fillOpacity 0.12）
- 点击新学校时清除旧围栏；组件卸载时清除

## 说明
- 学校数据需有经纬度才会显示标记（longitude/latitude 为空的不显示）
- 高德地图 Key 与 MapSearch/TupaiMap 一致：`ec9016bfbd481d766643253c1bbe5bc3`
- 名称显示阈值：SHOW_NAME_ZOOM=16
