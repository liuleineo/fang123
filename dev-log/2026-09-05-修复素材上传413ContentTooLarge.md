# 2026-09-05 修复素材上传 413 Content Too Large

## 问题

生产环境 `POST https://hangfang123.com/api/admin/medias/upload` 上传素材（图片/视频）时返回 `413 Content Too Large`。

## 根因

- 413 由 **Nginx 的 `client_max_body_size` 限制**导致，请求在到达 Spring Boot 之前即被 Nginx 拒绝
- README 中生产 Nginx 示例仅配置 `client_max_body_size 6M`
- 后端 `application-prod.yml` 已于 09-02 将 `max-file-size` 提高到 100MB，Nginx 与后端上限不一致
- 超过 6MB 的上传直接命中 Nginx 限制 → 413（走不到后端的 400 友好提示）

## 修复

### 服务器 Nginx 配置（需在生产服务器操作）

在 `hangfang123.com` 对应站点配置（宝塔：网站 → 配置文件）的 `server {}` 内增加/修改：

```nginx
client_max_body_size 200m;
proxy_read_timeout 300s;
```

然后重载生效：

```bash
nginx -t && nginx -s reload
# 或宝塔面板「重载配置」
```

### 文档同步

`README.md` Nginx 示例：`location /api/` 内的 `client_max_body_size 6M` + `proxy_read_timeout 120s`
→ 移到 `server` 级：`client_max_body_size 200m` + `proxy_read_timeout 300s`，并注明与后端 100MB 保持一致。

## 验证

- 服务器上执行 `curl -sI -X POST https://hangfang123.com/api/admin/medias/upload` 不再返回 413
- 上传 >6MB 的素材（视频/高清图）应能成功
