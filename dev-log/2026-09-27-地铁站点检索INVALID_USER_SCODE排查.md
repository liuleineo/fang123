# 2026-09-27 地铁站检索始终提示「安全密钥（securityJsCode）未正确配置」排查

## 现象

「地铁」按钮点击后固定弹出：`地铁站点检索失败：高德安全密钥（securityJsCode）未正确配置`（即高德返回 `INVALID_USER_SCODE`），与是否填值无关。

## 排查过程

1. **确认 key 类型**：用 `curl` 直连高德 Web 服务 REST 接口
   ```
   GET https://restapi.amap.com/v3/place/around?key=ec9016...&location=120.1536,30.2874&keywords=地铁站
   GET https://restapi.amap.com/v3/geocode/regeo?key=ec9016...&location=120.1536,30.2874
   ```
   两条均返回 `{"info":"USERKEY_PLAT_NOMATCH","infocode":"10009"}` → 该 key 是**Web端(JS API)** 类型，**不能**走服务端 REST 代理方案，只能继续用 JS API + 安全密钥。
2. **确认后端是否另存 key**：检索 `backend/src` 无任何高德配置（仅 schema.sql 中一句地图围栏注释），项目内不存在第二个可用 key。
3. **测试「该 key 未开启安全密钥」的可能性**：把 `web/index.html` 的 `securityJsCode` 从「误填的 key 本身」改为空串 `''`，浏览器实测点击「地铁」→ 仍返回同一 `INVALID_USER_SCODE`。
   → 反证该 key 在控制台**已开启安全密钥校验**，必须提供与之配对的 jscode，留空必然失败。

## 结论

代码逻辑正常（请求已发出、错误分支与提示均按预期工作），故障点在配置：

- `web/index.html` 第 13 行 `securityJsCode` 之前误填为 key 本身（`ec9016bfbd481d766643253c1bbe5bc3`）。
- 需替换为高德控制台中该 key 的**安全密钥 jscode**。

## 本次改动

`web/index.html`：把 `securityJsCode` 恢复为显式占位符 `'YOUR_SECURITY_JSCODE'`（避免误填值看起来"已配置"），并把上述排查结论写进注释（说明留空 / 误填 key 两种情况均实测失败、以及控制台获取路径）。注释文案由「公交/地铁站点检索」更新为「地铁站检索」。

## 用户需执行（唯一解法）

1. 打开 https://console.amap.com/ → 应用管理 → 我的应用 → 选中该 key
2. 找到「安全密钥」一栏，点击复制 jscode（32 位十六进制字符串，与 key 不同）
3. 粘贴替换 `web/index.html` 第 13 行的 `YOUR_SECURITY_JSCODE`
4. 刷新页面，点击「地铁」即可正常显示站点

## 备选方案（若拿不到 jscode，需另开任务）

- **方案 A**：在高德控制台新建一个「Web服务」类型的 key，地铁站检索改由后端代理 `restapi.amap.com/v3/place/around`（后端调用不需要 securityJsCode），前端不再直连服务接口。
- **方案 B**：内置杭州地铁线路/站点静态数据（geojson），前端纯本地绘制，完全不依赖高德服务接口（需可靠数据来源，避免坐标失真）。
