# 本地开发环境

配置基线核对：2026-10-24；文档整理：2026-10-26。这里只保留连接、目录和使用约定，不记录密码、问题清单或逐次修复历史。

问题统一见 [ISSUES.md](ISSUES.md)，已完成的修复见 [FIXES.md](FIXES.md)，功能补全、界面优化和维护整理见 [更新日志.md](更新日志.md)。

## 项目与工具

| 项目 | 路径或说明 |
| --- | --- |
| 后端 | `D:/IDEA_project/nebula-blog`，实际启动模块为 `nebula-start` |
| 前端 | `D:/Webstorm_project/nebula_blog` |
| 前端镜像 | 后端内的 `nebula-front`，仅用于Git subtree同步，不在其中开发 |
| Java | `D:/jdk17` |
| Maven | `D:/develop/apache-maven-3.9.5/bin/mvn.cmd`，本地依赖在 `D:/MavenRepository` |
| Node.js | `D:/nodejs` |

前后端保持两个独立目录。用户要求提交时，按 [Git subtree指南](git-subtree-guide.md) 先合入前端子树，再提交后端；未要求时不自动提交、推送或同步。

前端启动、验证命令、客户端页面入口和代码目录见 [前端使用说明](../../Webstorm_project/nebula_blog/README.md)。首页由 `src/views/home/index.vue` 组合，介绍组件及其脚本位于实际前端的 `src/components/client/home` 和同级 `logic` 目录；界面调整历史统一见 [更新日志](更新日志.md)。

## 服务连接

| 服务 | 连接与配置 |
| --- | --- |
| MySQL | 本机Windows服务 `MySQL81`，不在Docker中；`localhost:3306`，数据库 `nebula_blog` |
| 应用数据库账号 | `nebula_blog_app@localhost`，仅用于项目库的数据读写；数据库操作使用MyBatis-Flex |
| Redis | Docker容器 `nebula-redis`，配置镜像 `redis:7`；`localhost:6379`，DB 0 |
| Elasticsearch | 配置镜像 `elasticsearch:8.17.3`，容器名 `nebula-es`，端口 `9200`；用户暂缓使用，不自动启动 |
| 前端 | `http://localhost:5173` |
| 后端 | `http://localhost:8080` |

以上是连接配置，不代表服务此刻正在运行。任务结束默认关闭项目前后端；不操作Windows服务，不随之关闭MySQL、Redis或Docker。

## Docker配置与数据

- 配置目录：`D:/DockerData`。
- Compose配置源：`D:/DockerData/nebula-docker/docker-compose.yml`。
- Docker Desktop目录：`D:/DockerData/DockerDesktopWSL`。
- Redis命名卷：`nebula-redis-data`，容器内挂载到 `/data`；开启持久化，`appendonly=yes`、`appendfsync=everysec`，重启策略为 `always`。端口仅绑定 `127.0.0.1:6379`，已启用密码认证；容器从只读挂载的 `.secrets/redis.conf` 读取配置。
- Redis快照参数：`save="3600 1 300 100 60 10000"`。
- Elasticsearch命名卷配置为 `elasticsearch-data`，容器内挂载到 `/usr/share/elasticsearch/data`；单节点、堆内存512MB，本地配置关闭其内置验证。
- 命名卷由Docker管理，不等同于把Compose所在目录直接作为数据库数据目录。

按需检查配置和容器状态：

```powershell
docker compose -f D:\DockerData\nebula-docker\docker-compose.yml config --quiet
docker compose -f D:\DockerData\nebula-docker\docker-compose.yml ps
```

## 本地凭据

- 应用配置读取 `nebula-start/.secrets/NEBULA_DB_USERNAME`、`NEBULA_DB_PASSWORD`、`NEBULA_JWT_SECRET` 和 `NEBULA_REDIS_PASSWORD`，也支持对应环境变量。目录不提交Git。
- Redis本机配置由 `scripts/Initialize-LocalRedisSecurity.ps1` 同步认证文件并应用到Redis容器，复用既有密码和数据卷；Docker配置目录中的 `.secrets` 也不提交Git。脚本只操作Redis，不启动Elasticsearch。
- 数据库管理凭据位于 `nebula-start/.admin-secrets/ROOT_PASSWORD`，不属于应用配置；迁移表结构时才使用管理权限。
- JWT密钥初始化或明确轮换使用 `scripts/Initialize-JwtSecret.ps1`。根目录和 `nebula-start` 目录启动都能读取本地凭据。
- 邮箱凭据使用 `MAIL_USERNAME`、`MAIL_PASSWORD` 等环境变量，邮件协议调试默认关闭。
- 注册确认页面由 `NEBULA_REGISTRATION_CONFIRMATION_URL` 配置，本地默认 `http://localhost:5173/register`；使用 `prod` 配置时必须显式提供对外页面地址，不能包含凭据、查询参数或片段。
- 新签发Token默认两小时有效，`NEBULA_JWT_EXPIRE` 可覆盖毫秒数；已有Token沿用原有效期。接口文档默认关闭，按需设置 `NEBULA_API_DOCS_ENABLED=true` 后仍需超级管理员携带Token访问。
- 前端生产构建使用本地图标和内容安全策略；开发模式保留热更新，正式部署仍需正确配置前端入口、API代理及HTTPS。
- 不把密码、Token、邮箱验证码或密钥值写入文档、截图、日志及提交内容。

## 图片与媒体配置

- 默认上传根目录为后端项目的 `upload`；`file.upload-root` 可通过 `NEBULA_UPLOAD_ROOT` 设置为指定目录。从 `nebula-start` 启动时仍定位到项目根目录。
- 资源字段保存相对路径：`upload/{avatar|article}/{账号NID}/{yyyyMMdd}/{UUID.ext}`，不保存盘符、域名或访问凭证；日期使用上海时区。
- 支持JPG、PNG、GIF、WebP，单张不超过10MB，最多2500万像素，单边不超过12000px。
- 媒体元数据表为 `media_asset`；建表脚本为 `database/migrations/20261023_media_asset.sql`，需要通过MyBatis-Flex执行。
- 文章写作入口为 `/write` 和 `/write/:id`，媒体管理入口为 `/media`。

## 敏感密码验证配置

- `nebula.security.sensitive-password.attempts` 默认5，`window-seconds` 默认900；改密、注销及管理员重置中的当前密码验证共用操作者账号预算，与登录计数隔离。`ip-requests` 默认30，`ip-window-seconds` 默认60，来源识别沿用可信代理配置。
- `nebula.security.sensitive-password.key-prefix` 默认 `nebula:sensitive-password:v1:`；多实例共享Redis和前缀，不同部署环境应隔离前缀。修改这些配置后重启应用生效。

## 高成本请求与媒体额度配置

- 配置位于 `nebula.resources` 和 `nebula.media.limits`，通过应用配置或对应环境配置覆盖，重启后生效。头像和文章图片共用账号媒体额度。
- `nebula.resources.window-seconds` 默认60；下表的请求量均按这个窗口计算。`account`、`ip` 分别是账号和可信来源IP的请求量；`concurrency`、`account-concurrency`、`ip-concurrency` 是各入口在每个应用实例上的总并发、账号并发、IP并发。

| 配置前缀 | account / ip | concurrency / account-concurrency / ip-concurrency |
| --- | --- | --- |
| `nebula.resources.upload` | 12 / 40 | 4 / 1 / 2 |
| `nebula.resources.search` | 30 / 120 | 8 / 1 / 3 |
| `nebula.resources.read` | 240 / 600 | 16 / 4 / 8 |
| `nebula.resources.write` | 60 / 180 | 8 / 2 / 4 |

- `read` 覆盖媒体列表、图片读取、公开图片及媒体删除；匿名图片请求只使用IP预算。`write` 覆盖其余已登录的POST、PUT、PATCH、DELETE业务操作，评论、草稿、点赞、关注与管理操作共享账号和IP预算。上传保留独立额度，匿名认证请求仍使用原认证限额；编码路径与普通路径使用同一规则。
- `nebula.resources.max-concurrency` 默认16，进一步限制上述入口合计的并发；`decode-concurrency` 默认2，供上传验证和缩略图解码共用；`search-timeout-seconds` 默认3，限定搜索事务及其SQL的执行时间。
- 请求频率由Redis共享计数，`nebula.resources.key-prefix` 默认 `nebula:resources:v1:`；不同部署环境应使用不同前缀。来源地址沿用 `nebula.security.trusted-proxies` 配置，不直接信任客户端自行提交的转发头。并发名额和缩略图缓存按应用实例计算，多实例部署时应合计规划。
- `nebula.media.limits.account-bytes` 默认524288000（500MiB），`account-files` 默认2000；`daily-bytes` 默认209715200（200MiB），`daily-files` 默认200。每日按上海日期统计，当天删除过的图片仍计入当日上传量；累计额度只统计当前未删除的资源。
- `nebula.media.limits.min-free-disk-bytes` 默认536870912（512MiB），上传后须保留这部分磁盘余量；`thumbnail-cache-bytes` 默认16777216（16MiB），`thumbnail-cache-entries` 默认128。配置时按本机磁盘和内存调整，不需要增加数据库表或启动新的服务。

## 社区数据库

- 关注关系表：`user_follow`；建表脚本：`database/migrations/20261023_user_follow.sql`。
- 使用迁移账号通过MyBatis-Flex执行建表脚本；应用账号只需读写权限，不授予建表权限。新环境需先建表，再使用社区入口。
- 页面入口与使用规则见 [更新日志](更新日志.md)。

## 定时发布配置

- 新环境需先通过MyBatis-Flex使用迁移账号执行 `database/migrations/20261023_article_publication.sql`，为文章新增计划时间、安排者、暂停原因及查询索引；已有字段时不要重复执行 `ALTER TABLE`。
- 后端默认启用定时发布轮询。`nebula.publication.enabled=false` 可禁用；`nebula.publication.poll-delay-ms` 和 `nebula.publication.initial-delay-ms` 分别配置轮询间隔与启动后首次延迟，默认均为15000毫秒。
- 定时任务需要后端持续运行，时间统一使用 `Asia/Shanghai`；前端时间选择解释为 `+08:00`，不依赖浏览器所在时区。具体使用规则见 [更新日志](更新日志.md)。
- 全站搜索直接查询本机MySQL，不需要Elasticsearch，也不需要另外创建搜索服务。

## 原始验收材料

本地执行日志、截图和临时验收材料位于 `nebula-start/target/`，该目录不提交Git。问题说明和修复总结只维护上方两份统一记录，不在验收目录另写问题文档。
