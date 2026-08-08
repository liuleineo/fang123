# 真实成交 - 新增"AI新建成交"（文本解析）

## 需求
在真实成交页面"新建成交"按钮旁增加"AI新建成交"，用户粘贴成交播报文本，AI 自动识别成交字段填入表单。

## 示例输入
```
【成交播报】
楼盘地址：潮映华岸府2-1-602
房源面积：302
挂牌价格：2180万
成交价格：1950万带双车位
维护门店：贝壳钱二观潮店
成交日期：8.6
```

## 后端
- `dto/AiParseRealDealResult.java` - 新增 DTO（fields 含 address/communityName/roomNo/houseArea/listPrice/dealPrice/remark/maintainStore/dealDate/district/plate/loupanId）
- `service/AiParseService.java` - 新增 `parseRealDealFromText(String text)` 方法，复用 `callTokenHub` 大模型调用，用 REAL_DEAL_PARSE_PROMPT 提示词解析成交文本
- `controller/RealDealInfoController.java` - 新增 `POST /api/admin/real-deals/ai-parse` 接口（接收 JSON `{text: "..."}`）

## 前端
`admin/src/views/RealDealManage.vue`
- "新建成交"按钮旁新增"AI新建成交"按钮（Sparkles 图标，warning 主题）
- AI 对话框：文本域粘贴成交播报 → 开始识别 → 显示可编辑的识别结果 → "填入表单"
- 识别字段可逐项修改，确认后填入新建表单（dealDate/district/plate/communityName/roomNo/houseArea/dealPrice/remark/loupanId）

## 说明
- 复用已有 TokenHub 大模型（非图片 OCR，直接文本解析）
- 成交日期自动补全年份（8.6 → 2026-08-06）
- 成交价格中的"带双车位"等备注自动分离到 remark 字段

## 精简识别字段（v2）
- 只识别表单需要的字段：communityName/roomNo/houseArea/dealPrice/remark/dealDate/district/plate/loupanId/yfyj
- 移除多余字段：address(完整地址)/listPrice(挂牌价)/maintainStore(维护门店)
- 后端 DTO `AiParseRealDealResult` 同步精简
- AI 提示词明确要求只提取上述字段
- 前端 AI 对话框只展示表单需要的字段编辑框
