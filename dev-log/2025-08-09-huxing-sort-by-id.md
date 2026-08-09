# 户型管理列表改为按 ID 倒序

## 修改
`backend/src/main/java/com/fang123/controller/LoupanHuxingController.java`

将 `/api/admin/huxings` 接口排序从 `orderByDesc(sort).orderByDesc(createTime)` 改为 `orderByDesc(id)`。

## 影响
`/admin/huxings` 页面列表默认按 ID 倒序排列（最新创建的户型在前）。
