# 本地开发环境

配置基线核对：2026-11-08。这里只保留连接、目录和使用约定，不记录密码、问题清单或逐次修复历史。

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
| Redis | Docker容器 `nebula-redis`，镜像 `redis:7`；本机 `127.0.0.1:16380`，容器内6379，DB 0，启用认证 |
| Elasticsearch | Docker容器 `nebula-es`，镜像 `elasticsearch:8.19.17`；`http://127.0.0.1:9200`，启用认证 |
| 前端 | `http://localhost:5173` |
| 后端 | `http://localhost:8080` |

以上是连接配置，不代表服务此刻正在运行。任务结束默认关闭项目前后端；不操作Windows服务，不随之关闭MySQL、Redis、Elasticsearch或Docker。用户要求额外关停容器时只停相关容器，保留Docker Desktop后台，除非明确要求退出软件。

## Docker配置与数据

- 配置目录：`D:/DockerData`。
- Compose配置源：`D:/DockerData/nebula-docker/docker-compose.yml`。
- Docker Desktop目录：`D:/DockerData/DockerDesktopWSL`。
- Redis命名卷：`nebula-redis-data`，容器内挂载到 `/data`；开启持久化，`appendonly=yes`、`appendfsync=everysec`，重启策略为 `always`。本机端口仅绑定 `127.0.0.1:16380`；容器从只读挂载的 `.secrets/redis.conf` 读取配置。
- Redis快照参数：`save="3600 1 300 100 60 10000"`。
- Elasticsearch命名卷为 `nebula-es-data`，容器内挂载到 `/usr/share/elasticsearch/data`；单节点、1GiB堆、2GiB容器内存上限及2核预算，只绑定本机9200。应用使用专用索引账号；本机HTTP启用认证，远程连接需另行配置HTTPS。
- 命名卷由Docker管理，不等同于把Compose所在目录直接作为数据库数据目录。

按需检查配置和容器状态：

```powershell
docker compose -f D:\DockerData\nebula-docker\docker-compose.yml config --quiet
docker compose -f D:\DockerData\nebula-docker\docker-compose.yml ps
```

## 本地凭据

- 应用配置读取 `nebula-start/.secrets/NEBULA_DB_USERNAME`、`NEBULA_DB_PASSWORD`、`NEBULA_JWT_SECRET`、`NEBULA_REDIS_PASSWORD`、`NEBULA_REDIS_PORT` 和 `NEBULA_ES_PASSWORD`，也支持对应环境变量。目录不提交Git。
- Redis本机配置由 `scripts/Initialize-LocalRedisSecurity.ps1` 同步认证文件并应用到Redis容器，复用既有密码和数据卷；可用 `-Port 16380` 设置本机端口，不传时沿用本地端口文件，缺省6379。`NEBULA_REDIS_HOST` 缺省 `127.0.0.1`。Docker配置目录中的 `.secrets` 也不提交Git，脚本只操作Redis。
- ES本机配置与账号由 `scripts/Initialize-LocalSearch.ps1` 初始化，Docker配置只写入 `D:/DockerData/nebula-docker/docker-compose.yml`，应用密码写入本地配置树；管理员密码位于Docker配置目录的 `.secrets/elastic_password`，不交给应用账号。脚本复用既有凭据，Windows挂载的密码在容器内复制为600权限后交给官方入口读取。`-ConfigureOnly` 只准备配置和凭据，不启动容器。
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

## 认证前请求与JSON体积配置

- 配置前缀为 `nebula.security.request`，修改后重启应用生效。可信来源识别沿用 `nebula.security.trusted-proxies`，默认不信任转发头。
- `window-seconds` 默认60，`ip-requests` 默认1200；每个来源共用预算，换路由、账号或Token不重置。`max-concurrency` 默认32，`ip-concurrency` 默认12，覆盖JWT核验、正文解析及同步请求处理；超限返回429并携带重试提示。
- 这层预算在每个应用实例的内存中独立维护，不依赖数据库和Redis。`max-sources` 默认4096，只清理已过期且不占名额的来源；表满时对新来源返回503，不通过淘汰有效计数放行。多实例仍需部署入口的整体保护，认证后的账号业务预算继续由Redis共享。
- `json-bytes` 默认2097152（2MiB），`login-bytes` 默认8192（8KiB）；普通JSON按声明长度和实际读取字节限长，未声明长度同样受限，超限返回413。Multipart不在此缓存，继续使用独立上传限制。
- `json-read-timeout-ms` 默认10000，分段读取JSON前后检查累计时间；`server.tomcat.connection-timeout` 默认5秒，约束单次无数据读取等待，也适用于Multipart。累计期限不是严格的整条HTTP耗时上限，阻塞读取可能延续到容器空闲期限；超时返回408，超限或拒绝后的未读正文直接关闭连接，不继续吞读。容器错误转发保留原状态并返回公开JSON，不把正文超时改成401；直接访问错误页仍需认证。
- 登录账号最多128个字符、登录密码最多1024个字符；这些只是输入上限，不套用新密码最短长度，已有短密码仍进入原认证流程。新密码继续使用6至64字符及UTF-8不超过72字节的规则。

## 数据库等待与读取预算

- `spring.datasource.hikari.connection-timeout` 默认1000毫秒，`validation-timeout` 默认500毫秒；MySQL驱动的连接、网络读取空闲等待分别为2000、5000毫秒。池满、连接失败或明确查询超时统一返回503、`Retry-After`与禁止缓存提示，不把SQL语法错误或约束冲突当作繁忙。
- `nebula.security.request.authentication-timeout-seconds` 默认3，覆盖JWT的用户、会话及权限核验和登录凭据核验；`read-timeout-seconds` 默认5，普通GET/HEAD的认证及后续MyBatis查询共享读取预算。嵌套预算只能缩短期限，现有更短的事务超时继续生效。
- 查询使用JDBC秒粒度超时，连接获取和网络等待另有上限，不将这些配置描述为整条HTTP必定在3秒或5秒内结束。写业务不继承认证读取期限，原有事务与回滚规则保留；后台任务继续使用自己的超时设置。
- MyBatis-Flex继续管理数据库操作，异常翻译器明确使用MySQL错误码，避免首次报错时额外借连接读取数据库类型。

## 高成本请求与媒体额度配置

- 配置位于 `nebula.resources` 和 `nebula.media.limits`，通过应用配置或对应环境配置覆盖，重启后生效。头像和文章图片共用账号媒体额度。
- `nebula.resources.window-seconds` 默认60；下表的请求量均按这个窗口计算。`account`、`ip` 分别是账号和可信来源IP的请求量；`concurrency`、`account-concurrency`、`ip-concurrency` 是各入口在每个应用实例上的总并发、账号并发、IP并发。

| 配置前缀 | account / ip | concurrency / account-concurrency / ip-concurrency |
| --- | --- | --- |
| `nebula.resources.upload` | 12 / 40 | 4 / 1 / 2 |
| `nebula.resources.search` | 30 / 120 | 8 / 1 / 3 |
| `nebula.resources.read` | 240 / 600 | 16 / 8 / 16 |
| `nebula.resources.write` | 60 / 180 | 8 / 2 / 4 |
| `nebula.resources.export` | 4 / 12 | 2 / 1 / 2 |

- `read` 覆盖普通已登录GET/HEAD，包括文章、评论、个人主页、账号资料、当前会话、后台分页、媒体、编辑备份、文章版本、书架和系列，以及匿名公开图片和媒体删除；匿名图片请求只使用IP预算。`write` 覆盖其余已登录的POST、PUT、PATCH、DELETE业务操作，评论、草稿、点赞、关注与管理操作共享账号和IP预算。搜索、上传和文章导出保留独立额度；匿名认证请求在认证前保护之外继续使用原认证限额，未登录的私有接口仍由认证链拒绝。编码路径与普通路径使用同一规则。
- `nebula.resources.max-concurrency` 默认16，进一步限制上述入口合计的并发；`decode-concurrency` 默认2，供上传验证和缩略图解码共用；`search-timeout-seconds` 默认3，限定搜索事务及其SQL的执行时间。
- 请求频率由Redis共享计数，`nebula.resources.key-prefix` 默认 `nebula:resources:v1:`；不同部署环境应使用不同前缀。来源地址沿用 `nebula.security.trusted-proxies` 配置，不直接信任客户端自行提交的转发头。并发名额和缩略图缓存按应用实例计算，多实例部署时应合计规划。
- `nebula.media.limits.account-bytes` 默认524288000（500MiB），`account-files` 默认2000；`daily-bytes` 默认209715200（200MiB），`daily-files` 默认200。每日按上海日期统计，当天删除过的图片仍计入当日上传量；累计额度只统计当前未删除的资源。
- `nebula.media.limits.min-free-disk-bytes` 默认536870912（512MiB），上传后须保留这部分磁盘余量；`thumbnail-cache-bytes` 默认16777216（16MiB），`thumbnail-cache-entries` 默认128。配置时按本机磁盘和内存调整，不需要增加数据库表或启动新的服务。

## 关注与粉丝数据库

- 关注关系表：`user_follow`；建表脚本：`database/migrations/20261023_user_follow.sql`。
- 使用迁移账号通过MyBatis-Flex执行建表脚本；应用账号只需读写权限，不授予建表权限。新环境需先建表，再使用个人主页的关注与粉丝功能；独立社区入口目前隐藏，旧地址返回首页。
- 页面入口与使用规则见 [更新日志](更新日志.md)。

## 定时发布配置

- 新环境需先通过MyBatis-Flex使用迁移账号执行 `database/migrations/20261023_article_publication.sql`，为文章新增计划时间、安排者、暂停原因及查询索引；已有字段时不要重复执行 `ALTER TABLE`。
- 后端默认启用定时发布轮询。`nebula.publication.enabled=false` 可禁用；`nebula.publication.poll-delay-ms` 和 `nebula.publication.initial-delay-ms` 分别配置轮询间隔与启动后首次延迟，默认均为15000毫秒。
- 定时任务需要后端持续运行，时间统一使用 `Asia/Shanghai`；前端时间选择解释为 `+08:00`，不依赖浏览器所在时区。具体使用规则见 [更新日志](更新日志.md)。

## 全站搜索配置

- 后端在 `nebula-api` 使用官方 `co.elastic.clients:elasticsearch-java`；根POM通过 `elasticsearch-client.version` 统一Java客户端与REST传输版本，当前为8.19.17。连接池随应用关闭，单次调用和多步查询使用有界、可取消的请求预算。
- 默认 `NEBULA_SEARCH_BACKEND=elasticsearch`；`NEBULA_ES_ENDPOINT` 默认 `http://127.0.0.1:9200`，`NEBULA_ES_USERNAME` 默认 `nebula_search`。应用账号只能管理 `nebula-search` 及其前缀索引，不能管理集群安全配置；连接地址不能携带凭据。
- 初次部署先运行 `scripts/Initialize-LocalSearch.ps1`，再使用Java 17和项目运行类路径执行：`java -Dfile.encoding=UTF-8 -cp "<项目运行类路径>" scripts/ApplySearchMigration.java nebula_blog`。迁移通过MyBatis-Flex读取本地管理凭据，安装 `20261107_search_index.sql` 的任务表及事务触发器，并应用 `20261108_article_content_capacity.sql`；正文扩容保留现有字符集、排序规则和可空约束，重复执行核对触发器定义。
- MySQL仍是本机业务库。应用账号保持项目库读写权限，不授予DDL或TRIGGER权限；启动不会自动迁移表结构。后端首次运行后异步建立索引，重建未完成时搜索返回可重试的503。
- `nebula.search.alias` 默认 `nebula-search`，不同环境使用独立的 `nebula-search-` 前缀名称；物理索引由程序管理。`batch-size` 默认200且单批正文预算4MiB，单文档最多8MiB；超限任务保留错误并等待处理，不截断正文。
- `nebula.search.poll-delay-ms` 默认1000、`initial-delay-ms` 默认2000；同步有独立调度线程。`request-timeout-ms` 默认1800，范围100到3000；用户搜索还受已有3秒整体预算限制。`max-window` 默认10000，范围1000到10000；`synchronization-enabled=false` 可临时暂停同步。
- 超级管理员入口：`GET /search/index/status` 查看构建状态、待处理和失败任务数量及错误分类；`POST /search/index/rebuild` 申请全量重建。`ready` 表示构建及别名切换完成，网络故障另由错误分类和搜索503响应体现。重建采用独立新索引，完整后切换别名，保留上一份索引；业务表不删除。
- 应急时可显式设置 `NEBULA_SEARCH_BACKEND=mysql` 并重启后端，继续使用原有受限查询；不会因ES断线自动放大MySQL扫描负载。切回ES后继续处理已持久保存的任务。搜索入口和交互规则见 [更新日志](更新日志.md)。
- 搜索集成回归使用独立库 `nebula_es_20261107`、索引前缀 `nebula-search-it` 和Redis第14分库。新环境可用相同Java运行类路径依次执行 `scripts/PrepareSearchTestDatabase.java nebula_es_20261107`、`scripts/ApplySearchMigration.java nebula_es_20261107`；准备入口只复制表结构及角色权限定义，已有表时拒绝覆盖，不复制业务账号和正文。运行测试前需启动本机MySQL及正式认证ES、Redis，测试只清理自己的数据。

## 写作与阅读功能数据库

- 新环境需使用迁移账号通过MyBatis-Flex依次执行[编辑备份迁移](database/migrations/20261027_article_workspace.sql)、[阅读库迁移](database/migrations/20261027_reading_library.sql)、[版本历史迁移](database/migrations/20261027_article_revision.sql)、[系列迁移](database/migrations/20261027_article_series.sql)。
- 以上脚本分别添加文章编辑版本与 `article_workspace`，以及 `article_reading`、`article_revision`、`article_series`、`article_series_item` 表；已有 `edit_version` 字段时不要重复执行对应的 `ALTER TABLE`。应用账号仍只使用项目库读写权限。
- 书架入口为 `/library`；系列发现和详情为 `/series`、`/series/:id`，个人系列管理为 `/series?mine=1`。功能使用规则见 [更新日志](更新日志.md)。

## 原始验收材料

本地执行日志、截图和临时验收材料位于 `nebula-start/target/`，该目录不提交Git。问题说明和修复总结只维护上方两份统一记录，不在验收目录另写问题文档。

## 查询索引迁移

- 迁移文件：`database/migrations/20261107_query_indexes.sql`；入口：`scripts/ApplyQueryIndexes.java`，数据库访问使用MyBatis-Flex。
- 在后端根目录、使用Java 17及项目运行类路径执行：`java -cp "<项目运行类路径>" scripts/ApplyQueryIndexes.java nebula_blog`。本机入口从既有`.admin-secrets/ROOT_PASSWORD`读取迁移凭据，不将凭据放入命令行。
- 入口仅接受`nebula_blog`或`nebula_deep_`开头的数字测试库名；同名同列索引跳过，同名不同定义停止；应用启动不会自动执行结构迁移。迁移仅添加索引，不清理业务数据。
