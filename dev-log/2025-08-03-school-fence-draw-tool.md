# admin 学校编辑增加围栏绘制工具

## 需求
在 admin 学校编辑表单的"地图围栏坐标"处增加围栏绘制工具（地图画多边形生成坐标）。

## 修改文件
`admin/src/views/SchoolManage.vue`

### 模板（v2：改为全屏绘制）
- 表单项默认**不展示地图**，只显示：
  - "绘制/查看围栏"按钮 + 当前围栏点数 + 清除按钮
- 点击"绘制/查看围栏" → 弹出**全屏 dialog**（90% 宽，70vh 高）
  - 内含高德地图 + 开始绘制/撤销/清除按钮 + 保存/取消
- 图标用 PenLine、Trash2

### 功能
- **开始绘制**：用 AMap.MouseTool 在地图上画多边形，完成后自动把坐标（`lng,lat;lng,lat`）写入 `form.mapFence`
- **撤销**：清除绘制的围栏并清空 mapFence
- **清除**：关闭绘制工具并清空
- **已有围栏显示**：编辑时若已有 mapFence，地图上显示蓝色半透明多边形并 fitView
- 新建时地图定位到默认杭州；编辑时定位到学校经纬度

### 高德地图
- Key：`ec9016bfbd481d766643253c1bbe5bc3`
- 引入 AMap.MouseTool 插件
- 组件卸载时销毁地图

## 相关修复
web 端学校地图 `showInfo` 围栏字段 bug：`s.map_fence` → `s.mapFence`（驼峰命名）
