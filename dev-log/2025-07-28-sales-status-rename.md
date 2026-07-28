# 售楼状态枚举调整：待开放→待售，去掉停工

## 变更
| 旧值 | 旧名称 | 新值 | 新名称 |
|------|--------|------|--------|
| 0 | 待开放 | 0 | 待售 |
| 1 | 在售 | 1 | 在售 |
| 2 | 售罄 | 2 | 售罄 |
| 3 | 停工 | 3 | 交付 |
| 4 | 已交付 | - | 删除 |

## 修改文件
- `backend/src/main/resources/db/schema.sql` - 注释更新
- `admin/src/views/LoupanManage.vue` - 映射数组 + select 选项
- `web/src/views/LoupanDetail.vue` - 映射数组 + 颜色逻辑
- `web/src/views/Home.vue` - 映射数组 + 颜色逻辑
- `web/src/views/ErShouFang.vue` - 映射数组 + 颜色逻辑 + 默认筛选改为 `2,3`

## 颜色对照
- 0(待售): gray
- 1(在售): green
- 2(售罄): gray/orange
- 3(交付): blue
