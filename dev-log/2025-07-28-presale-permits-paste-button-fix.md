# 修复预售证管理 - 粘贴图片后"开始识别"按钮无法点击

## 问题
`/admin/presale-permits` 页面中，粘贴图片后"开始识别"按钮始终处于 disabled 状态，无法点击。

## 原因
`<t-tabs>` 组件缺少 `v-model="aiTab"` 绑定，导致 `aiTab` 变量始终为初始值 `'upload'`。即使用户切换到"粘贴图片"标签页，`aiTab` 也不会更新。

按钮 disabled 逻辑：
```vue
:disabled="aiTab==='upload' ? aiFiles.length===0 : aiPasteFiles.length===0"
```

由于 `aiTab` 永远是 `'upload'`，始终检查 `aiFiles.length===0`（上传文件列表为空），所以按钮一直 disabled。

## 修复
在 `<t-tabs>` 上添加 `v-model="aiTab"`：
```vue
<t-tabs v-model="aiTab">
```

## 修改文件
- `admin/src/views/PresalePermitManage.vue`
- `admin/src/views/LoupanTupaiLandManage.vue`
- `admin/src/views/LoupanHuxingManage.vue`
- `admin/src/views/LoupanManage.vue`

## 排查范围
检查了所有 admin 页面中使用 `<t-tabs>` + `aiTab` 模式的页面，共发现 4 个页面有同样问题（缺少 `v-model="aiTab"`），全部已修复。
