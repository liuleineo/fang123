# 媒体素材列表排序改为按 ID 倒序

## 修改
`backend/src/main/java/com/fang123/controller/LoupanMediaController.java`

将排序从 `orderByDesc(sort).orderByDesc(createTime)` 改为 `orderByDesc(id)`。

## 影响
`/admin/medias` 页面列表默认按 ID 倒序排列（最新的在前）。
