# 2025-06-21 - 包名重命名：neoapp → fang123

## 改动概述
将 backend 模块的 Java 包名从 `com.neoapp` 全面改为 `com.fang123`。

## 修改内容

### 1. 目录重命名
- `src/main/java/com/neoapp/` → `src/main/java/com/fang123/`

### 2. Java 源文件（批量替换）
- 所有 `package com.neoapp.*` → `package com.fang123.*`
- 所有 `import com.neoapp.*` → `import com.fang123.*`
- `NeoAppApplication.java` → `Fang123Application.java`（类名同步修改）

### 3. pom.xml
- `groupId`: `com.neoapp` → `com.fang123`
- `artifactId`: `neoapp-backend` → `fang123-backend`
- `name`: `neoapp-backend` → `fang123-backend`
- `description`: `NeoApp` → `Fang123`

### 4. application.yml
- `spring.application.name`: `neoapp-backend` → `fang123-backend`
- `jwt.secret`: `NeoAppSecretKey...` → `Fang123SecretKey...`
- 注释中 prod 数据库 URL: `neoapp` → `fang123`

### 5. application-dev.yml
- MySQL URL: `localhost:3306/neoapp` → `localhost:3306/fang123`

### 6. application-prod.yml
- MySQL URL: `localhost:3306/neoapp` → `localhost:3306/fang123`
- JWT Secret 同步修改

### 7. schema.sql
- 注释: `NeoApp` → `Fang123`

### 8. HealthController.java
- 服务名称字符串 `"neoapp-backend"` → `"fang123-backend"`

### 9. JwtUtil.java
- `@Value` 默认值: `NeoAppSecretKey...` → `Fang123SecretKey...`

## ⚠️ 注意事项
- 数据库名称已从 `neoapp` 改为 `fang123`，需要创建新的数据库：
  ```sql
  CREATE DATABASE IF NOT EXISTS fang123 DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- `target/` 目录下的旧编译产物会在下次 `mvn clean compile` 时自动清除并重新生成
- IDE 可能需要刷新/重新导入项目
