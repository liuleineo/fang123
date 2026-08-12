# 2026-08-12 school-map 学校弹窗增加关闭按钮

## 改动内容

`web/src/views/SchoolMap.vue`（school-map 页面，高德地图学校弹窗）：

1. 新增全局变量 `infoWindow` 及关闭函数 `closeInfoWindow()`，并挂载到 `window`（因弹窗内容为 HTML 内联字符串，需挂到全局供 `onclick` 调用）
2. `showInfo()` 函数开头调用 `closeInfoWindow()` 关闭上一个弹窗，避免多次点击堆积
3. 弹窗内容（InfoWindow content）顶部右上角新增圆形"✕"关闭按钮，白色背景上悬浮显示
4. 弹窗标题 `padding-right` 调整为 20px，避免与关闭按钮重叠
5. `showInfo()` 末尾改为先创建 InfoWindow 实例并保存到 `infoWindow`，再 `.open()`，供关闭函数使用

## 交互效果

点击学校图钉/列表项弹出详情后，弹窗右上角出现"✕"关闭按钮，点击可关闭弹窗。

## 说明

- 已重新编译 web 端（`npm run build`）成功
- 若需线上生效，需将 `web/dist` 部署到线上服务器
