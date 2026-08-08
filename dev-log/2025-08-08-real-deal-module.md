# 新增真实成交模块（admin 端增删改查）

## 需求
实现 `real_deal_info` 表（真实成交）的 admin 端增删改查。

## 数据库
`real_deal_info` 表已在 schema.sql 中定义（含 id/deal_date/district/plate/community_name/room_no/house_area/deal_price/remark/yfyj/loupan_id/deleted 等字段）。

## 后端新增
- `entity/RealDealInfo.java` - 实体（主键 AUTO，含 deleted 软删除标志，createTime/updateTime 自动填充）
- `mapper/RealDealInfoMapper.java`
- `service/RealDealInfoService.java`
- `service/impl/RealDealInfoServiceImpl.java`
- `controller/RealDealInfoController.java` - CRUD 接口

### 接口列表
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/real-deals` | 分页+搜索（keyword/district/plate/loupanId），过滤 deleted=0 |
| GET | `/api/admin/real-deals/{id}` | 详情 |
| POST | `/api/admin/real-deals` | 新增（校验成交日期/小区名称） |
| PUT | `/api/admin/real-deals/{id}` | 更新 |
| DELETE | `/api/admin/real-deals/{id}` | 软删除（deleted=1） |

## admin 端新增
- `views/RealDealManage.vue` - 页面（列表+搜索+抽屉表单）
  - 字段：成交日期/行政区/板块/小区/房号/面积/成交价/一手价/楼盘ID/备注
  - 成交价红色高亮，一手价蓝色
- `router/index.js` - 新增 `/real-deals` 路由
- `layouts/AdminLayout.vue` - 菜单新增"真实成交"，图标 Coins

## 说明
- 删除采用软删除（deleted=1），列表查询自动过滤已删除记录
- 面积/价格用 BigDecimal，前端 t-input-number 录入
- 支持按楼盘ID、行政区、关键词筛选
