# 2025-06-21 - 修改前后端端口

## 改动
- 后端端口: `8080` → `8090`
- 前端端口: `3001` → `4000`

## 修改文件
| 文件 | 改动 |
|------|------|
| `backend/src/main/resources/application.yml` | `server.port: 8080` → `8090` |
| `web/vite.config.js` | `port: 3001` → `4000`，proxy target `8080` → `8090` |
| `backend/src/main/resources/application-dev.yml` | wechat redirect-uri `3001` → `4000`，notify-url `8080` → `8090` |

## 服务状态
- 后端: `http://localhost:8090` ✅
- 前端: `http://localhost:4000` ✅
