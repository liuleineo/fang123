# 学校地图优化：按需加载数据（提升速度）

## 需求
为提升加载速度、防止一次 API 输出所有数据，默认只请求展示所需字段，点击学校后再请求弹窗展示的数据和围栏数据。

## 实现
### 后端
`controller/SchoolController.java`：
1. 新增 `GET /api/public/schools/light` - 精简字段列表接口（只返回地图标记需要的字段：campusCode/schoolOrgName/campusName/schoolType/tier/longitude/latitude）
2. 新增 `GET /api/public/schools/{campusCode}` - 学校详情接口（返回完整数据，含 communityNames/targetMiddleSchoolName/mapFence 等）

### web 前端
`views/SchoolMap.vue`：
1. `fetchData` 改用 `/public/schools/light` 精简接口（地图标记 + 筛选用），大幅减少数据传输
2. `showInfo(s)` 改为 async：点击标记时请求 `/public/schools/{campusCode}` 详情接口，拿到完整数据后再展示弹窗（小区、对口初中、简介）和绘制围栏
3. 详情请求失败时回退使用列表已有数据

## 说明
- 地图标记只需精简字段，点击时才请求完整详情，显著提升首屏加载速度
- 围栏数据（mapFence）较大，改为点击时才加载，避免一次拉取所有围栏
