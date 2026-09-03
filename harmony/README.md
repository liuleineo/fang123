# 杭房一二三 · 鸿蒙原生客户端

参照 `fang123/web`（Vue 3）转化实现的 HarmonyOS 原生 App（Stage 模型 / ArkTS，API 12 / HarmonyOS 5.0）。

> 首期范围：**我的客户 CRM**（登录/注册 → 客户列表 → 添加/编辑/删除 → 跟进记录 → 分享 → AI 沟通文案），不含地图。

## 工程结构

```
harmony/
├── AppScope/                     # 应用级配置（bundleName/图标/名称）
├── build-profile.json5           # 工程级构建配置（SDK 版本）
├── hvigorfile.ts / oh-package.json5
└── entry/                        # 入口模块（HAP）
    ├── build-profile.json5
    └── src/main/
        ├── module.json5          # 模块配置 + INTERNET 权限
        ├── resources/base/       # 字符串/颜色/图标/路由表(main_pages)
        └── ets/
            ├── entryability/EntryAbility.ets
            ├── common/AppContext.ets   # 全局 Context 与 token 持久化
            ├── common/Api.ets          # HTTP 请求封装
            ├── model/Models.ets        # 数据模型
            └── pages/
                ├── Index.ets      # 启动分发（有 token → Customers，否则 Login）
                ├── Login.ets      # 手机号+密码登录
                ├── Register.ets   # 注册
                └── Customers.ets  # 我的客户 CRM（含各底部弹层操作）
```

## 运行步骤（DevEco Studio）

1. 用 **DevEco Studio 5（HarmonyOS NEXT）** 打开本目录
2. 首次打开若提示 SDK/工程升级，按提示 Sync 一次
3. 连接真机或模拟器，Run `entry`

### 接口地址

默认请求线上后端：`https://hangfang123.com`（见 `entry/src/main/ets/common/Api.ets` 的 `BASE_URL`）。

本地联调（后端跑在电脑 8090）：
- 把 `BASE_URL` 改为 `http://<电脑局域网IP>:8090`
- HarmonyOS **默认禁止明文 HTTP**，需在 `module.json5` 中补充网络安全配置或在工程中放行
  （最简单：后端走 HTTPS，或按官方文档为 debug 包配置网络，详见 DevEco 网络权限文档）

## 已实现功能

| 模块 | 说明 | 接口 |
|---|---|---|
| 登录 | 手机号+密码 | `POST /api/auth/login` |
| 注册 | 手机号+密码+昵称 | `POST /api/auth/register` |
| 客户列表 | 分页加载/下拉刷新/关键词搜索/意向筛选/按最后跟进时间排序 | `GET /api/user/customers` |
| 添加/编辑客户 | 底部弹层表单（姓名/手机号/意向/备注/删除） | `POST/PUT/DELETE /api/user/customers` |
| 跟进记录 | 查看+按方式(电话/微信/到访/其他)添加 | `GET/POST /api/user/customers/{id}/follow-ups` |
| 分享客户 | 按昵称/手机号搜索用户并分享 | `GET /api/user/customers/user-search`、`POST .../share` |
| AI 沟通文案 | 生成+一键复制 | `POST /api/user/customers/{id}/ai-suggest` |
| 退出登录 | 清理 token 回登录页 | — |

## 后续可扩展

- 楼盘浏览/详情/图库等门户内容页（首页 / 列表 / 详情 / 图库 / 户型 / 一房一价 / 动态 / 真实成交）
- 地图类页面（鸿蒙 Map Kit 或 Web 内嵌方案）
- 客户头像/跟进图片上传（需鸿蒙 Image 选择器 + 上传接口改造为 multipart）
- 微信登录 / Excel 批量导入 / AI 图片识别录入
