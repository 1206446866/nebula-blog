# 本地环境与 Docker 配置

最近核对：2026-10-21。本文记录本机实际配置，不包含账号密码。

## 项目目录

- 后端：`D:\IDEA_project\nebula-blog`。
- 前端：`D:\Webstorm_project\nebula_blog`。`nebula-front` 是 subtree 镜像，不在其中开发。
- Docker 目录：`D:\DockerData`。
- Docker Compose 配置源：`D:\DockerData\nebula-docker\docker-compose.yml`。
- Docker Desktop WSL 目录：`D:\DockerData\DockerDesktopWSL`，当前包含 `disk` 与 `main`。

服务管理约定（2026-10-21 用户明确）：以后“关闭项目服务”以项目前后端为主，不操作 Windows 服务（包括 MySQL81）；基础设施不随前后端默认启停。

## 服务

| 服务 | 位置/容器 | 连接 | 持久化与状态 |
| --- | --- | --- | --- |
| MySQL 8.1.0 | Windows 本机服务 `MySQL81`，不在 Docker 内 | `localhost:3306`，数据库 `nebula_blog` | 运行中；应用已改用受限账号 `nebula_blog_app`，凭据由本地文件或环境变量提供；未操作 Windows 服务 |
| Redis 7.4.9 | `nebula-redis`，镜像配置为 `redis:7` | `localhost:6379`，应用使用 DB 0 | 本轮检查时已恢复运行；命名卷 `nebula-redis-data` 挂载至 `/data`；重启策略 `always` |
| Elasticsearch | 配置容器名 `nebula-es`，镜像 `elasticsearch:8.17.3` | 配置端口 `localhost:9200` | 当前未创建容器；命名卷配置 `elasticsearch-data` 挂载至 `/usr/share/elasticsearch/data` |
| 前端 | Vite | `http://localhost:5173` | 2026-10-22 后台管理修复验收后保持关闭，5173 未监听 |
| 后端 | Java 17 | `http://localhost:8080` | 2026-10-22 后台管理修复验收后关闭，8080 未监听；临时 18080 也已关闭 |

Redis 实际参数：`appendonly=yes`、`appendfsync=everysec`、`save="3600 1 300 100 60 10000"`。未配置 Redis 密码。

Compose 中 Elasticsearch 为单节点，配置关闭内置安全验证，JVM 堆 `512m`，重启策略 `always`。记录配置不代表已经启动该服务。

命名卷由 Docker Desktop 的 Linux 环境管理，并非直接把 `D:\DockerData\nebula-docker` 绑定为数据库数据目录。

## 本次同步

原 Compose 文件使用了 `elasticsearch-data`，但没有在顶层 `volumes` 中声明，导致整个配置无法通过校验。已在原文件补齐声明，`docker compose config --quiet` 校验通过。

原配置备份：`nebula-start/target/account-review/docker-compose.before.yml`。本次没有启动 Elasticsearch，没有重启、重建 Redis 或改变其数据卷。

检查统一使用这一份配置源：

```powershell
docker compose -f D:\DockerData\nebula-docker\docker-compose.yml config --quiet
docker compose -f D:\DockerData\nebula-docker\docker-compose.yml ps
```

## 授权修复后的运行状态

2026-10-21 本轮恢复服务：后端 PID `32116`，前端 PID `26116`；日志在 `nebula-start/target/authorization-review/`。`admin`（用户 ID 1）已由 ADMIN 提升为 SUPER_ADMIN，详见 [授权修复记录](AUTHORIZATION_FIX.md)。本轮未更改 Docker、Redis、Elasticsearch 配置。

后续草稿可见性修复已将后端更新为 PID `48000`，日志在 `nebula-start/target/content-visibility-review/`。前端仍为 PID `26116`，见 [草稿可见性修复记录](DRAFT_VISIBILITY_FIX.md)。

渲染安全验收后客户端中断，重启后恢复服务：当时后端 PID `41156`，前端 PID `16844`。该轮日志在 `nebula-start/target/markdown-security-review/`，见 [渲染安全修复记录](XSS_RENDERING_FIX.md)。

Token 撤销修复加载时后端 PID `26496`，前端 PID `16844`。该轮日志在 `nebula-start/target/token-revocation-review/backend-8080*.log`。复用现有 Redis DB 0 的 `auth:session:<jti>` 键维护登录会话，不修改 Docker 配置或数据库结构；JWT 密钥暂缓调整。见 [Token 撤销修复记录](TOKEN_REVOCATION_FIX.md)。历史 Token 需重新登录一次。

注册完整性修复加载时后端 PID `52892`，前端 PID `16844`。该轮日志在 `nebula-start/target/registration-review/backend-8080*.log`。复用现有 Redis 注册凭证键及数据库邮箱唯一索引，没有结构迁移或基础设施调整。见 [注册完整性修复记录](REGISTRATION_INTEGRITY_FIX.md)。

文章点赞修复加载时后端 PID `34836`，前端 PID `16844`。后端最新日志在 `nebula-start/target/article-like-review/backend-8080*.log`，前端运行日志仍在 `markdown-security-review/`。没有结构迁移或基础设施调整。见 [文章点赞修复记录](ARTICLE_LIKE_FIX.md)。

## 暂停开发后的运行状态

2026-10-21 用户要求关闭项目服务、暂缓开发。已核实进程身份并停止前端 PID `16844`、后端 PID `34836`，正常停止 Redis 容器（退出码 0）。`5173`、`8080`、`6379`、`9200` 均未监听；Elasticsearch 未创建、未启动。MySQL81 停止请求被 Windows 服务控制权限拒绝，仍监听 `3306`，未强制终止数据库进程。代码、容器数据卷及现有修复记录保留；没有创建自动继续任务。

## 登录完整性修复后的当前状态

2026-10-21 用户恢复开发并要求修复接下来两项 P2。邮箱/手机号认证状态和登录 User-Agent 容错已修复，安全与业务回归 155 项通过；详见 [登录完整性修复记录](LOGIN_INTEGRITY_FIX.md)。当前后端 PID `50904`、前端 PID `52560`，运行及验收记录在 `nebula-start/target/login-integrity-review/`。本轮检查时 Redis 已恢复运行，测试复用现有 MySQL/Redis，没有操作 Windows 服务或修改基础设施配置。下一项仍待处理的是 Security 链 CORS，JWT 密钥继续暂缓。

## CORS 与错误响应修复后的当前状态

2026-10-21 后续两项 P2 已修复并加载：当前后端 PID `35348`、前端 PID `52560`。后端安全/业务回归 185 项、前端 47 项及 16 项实际 HTTP 验收通过。后端运行与测试留档在 `nebula-start/target/http-security-review/`；详见 [HTTP 安全修复记录](HTTP_SECURITY_FIX.md)。未操作 Windows 服务、修改 Docker/数据库结构或 JWT 密钥；明确列出的 P2 已完成，独立审查边界仍保留。

## 安全审查边界修复后的当前状态

2026-10-21 原独立审查边界已动态验证并修复，详见 [边界验证修复记录](SECURITY_BOUNDARIES_FIX.md)。当前后端 PID `11304`、前端 PID `52560`。后端安全/业务回归 233 项、前端 50 项、实际 HTTP 验收 15 项通过；日志在 `nebula-start/target/security-boundaries-review/`。

新增认证限速默认使用现有 Redis 的 `auth:abuse:` 键，验证码预算/邮箱窗口仍在 DB 0，均有有效期。未改 Redis/Docker 配置、数据库结构或 Windows 服务。应用默认不信任转发头；如部署反代，可用 `nebula.security.trusted-proxies` 显式配置可信代理范围。JWT 密钥仍暂缓，没有真实发信或修改既有账号密码。

## JWT 密钥修复后的当前状态

2026-10-21 已完成剩余 JWT P1：签名密钥迁移至 Git 忽略的 `nebula-start/.secrets/NEBULA_JWT_SECRET`，支持环境变量优先覆盖，当前密钥已经轮换。根目录及启动模块目录的启动方式均已实际验证，使用同一密钥重启可保持新会话；原有登录需要重新登录一次。首次生成及明确轮换使用 `scripts/Initialize-JwtSecret.ps1`，文件仅当前 Windows 用户可读写，报告不记录密钥。

当前后端 PID `48068`、前端 PID `52560`，运行日志在 `nebula-start/target/jwt-secret-review/`；临时 18080 验收后端已停止。252 项后端安全/业务回归和 11 项 HTTP 验收通过，详见 [JWT 密钥修复记录](JWT_SECRET_FIX.md)。未操作 Windows 服务、改变 Docker/数据库结构或现有账号密码。另发现的数据库明文密码和 SMTP 调试日志开启问题保留待处理。

## 凭据修复与本轮最终关闭状态

2026-10-21 数据库明文凭据和邮件调试日志两项问题均已修复，详见 [凭据与邮件配置修复记录](CONFIG_CREDENTIALS_FIX.md)。应用连接 `nebula_blog_app@localhost`，仅具备 `nebula_blog` 库的读写权限；8 份配置的凭据统一外部注入，启动根目录和模块目录均可读取本地配置。应用凭据在 `nebula-start/.secrets/NEBULA_DB_USERNAME`、`NEBULA_DB_PASSWORD`。

原 `root@localhost` 密码已轮换，旧密码经实际新连接验证被拒绝；新管理密码在仅当前用户可访问的 `nebula-start/.admin-secrets/ROOT_PASSWORD`，该目录不参与应用配置或 Git 提交。IDEA 中使用旧 root 密码的连接需要更新。邮件仍使用原环境变量凭据，协议调试默认关闭，没有发送真实邮件。

最新 259 项后端回归与 6 项实际 HTTP 验收通过，日志在 `nebula-start/target/config-secrets-review/`。随后已核实身份并关闭前端 PID `52560`、后端 PID `48068` 及临时验收服务，5173、8080、18080 均未监听。MySQL 3306、Redis 6379 保持运行；未操作 Windows 服务或 Docker 配置。

## 后台管理修复后的当前状态

2026-10-22 已修复管理端新增的 8 组 P2，草稿编辑、删除功能按用户说明暂缓，见 [后台管理修复记录](ADMIN_MANAGEMENT_FIX.md)。后端 295 项、前端 50 项、实际 HTTP 19 项验收通过，日志在 `nebula-start/target/admin-management-fix/`。所有数据库操作使用 MyBatis-Flex，未改数据库结构、现有账号密码或角色权限。

验收临时启动的 18080 后端已核实身份并关闭，5173、8080、18080 均未监听；MySQL 3306、Redis 6379 保持运行，没有操作 Windows 服务或修改 Docker 配置。按用户本轮约定，后续任务结束默认关闭项目前后端。

## 草稿管理功能开发后的当前状态

2026-10-22 用户追加要求开发此前暂缓的草稿编辑、删除功能，现已完成，见 [草稿管理功能记录](DRAFT_MANAGEMENT_FIX.md)。最新后端 312 项、前端 50 项回归与构建通过，日志位于 `nebula-start/target/draft-management-fix/`。本轮测试复用现有 MySQL/Redis，没有启动独立前后端监听服务；最终 5173、8080、18080 均未监听，MySQL 3306、Redis 6379 保持运行。
