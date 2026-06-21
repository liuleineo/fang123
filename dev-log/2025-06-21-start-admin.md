# 2025-06-21 - 启动 admin 管理后台

## 改动
- 修正 admin `vite.config.js` 代理 target: `8080` → `8090`

## 服务状态
| 服务 | 端口 | URL |
|------|------|-----|
| 后端 | 8090 | http://localhost:8090 |
| Web 前端 | 4000 | http://localhost:4000 |
| Admin 后台 | 3003 | http://localhost:3003/admin/ |

> 注：Admin 默认端口 3000，因端口被占自动 fallback 到 3003
