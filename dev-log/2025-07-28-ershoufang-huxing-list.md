# 二手房页面改为每行一个楼盘 + 户型展示

## 后端新增
`backend/src/main/java/com/fang123/controller/LoupanPublicController.java`
- 新增 `GET /api/public/loupans/huxings/batch?loupanIds=1,2,3` 接口
- 批量获取多个楼盘的户型列表，按面积从大到小排序

## 前端改造
`web/src/views/ErShouFang.vue`

### 布局变化
- 从网格卡片 → 每行一个楼盘的列表布局
- 左侧：封面图（固定宽度 72）
- 右侧：楼盘详情（名称、价格、位置、标签等）

### 户型展示
- 底部横滑区域展示该楼盘的户型卡片
- 每个户型卡片：户型图 + 名称 + 面积 + 室厅
- 按建筑面积从大到小排列
- 最多展示 6 个户型
- 户型卡片点击跳转楼盘详情页

### 技术细节
- 加载楼盘列表后，批量请求 `batchHuxings` 接口
- 每个楼盘最多取前 6 个户型
- pageSize 从 12 改为 10（单行信息更多，一屏更合理）
