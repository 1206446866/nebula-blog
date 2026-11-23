# Nebula Blog

Nebula Blog 是一个前后端分离的博客项目，包含文章阅读、Markdown 写作和后台管理。作者可以把多篇文章编成系列，读者可以把感兴趣的文章放进自己的书架。

## 主要功能

| 功能 | 可以做什么 |
| --- | --- |
| 写作与发布 | 编辑和预览 Markdown，管理文章图片，导入或导出文稿。支持草稿、编辑备份和版本恢复，可立即发布或定时发布。 |
| 浏览与搜索 | 按分类、标签和系列浏览文章，搜索文章、评论、用户、分类和标签。 |
| 阅读与互动 | 收藏文章、标记稍后读、从上次的位置继续阅读，也可以发表评论或点赞。 |
| 后台管理 | 按权限管理用户、角色、文章、评论、分类和标签。 |

## 技术栈

- 前端使用 Vue 3、TypeScript、Vite、Pinia 和 Element Plus。
- 后端使用 Java 17、Spring Boot、Spring Security 和 MyBatis-Flex，通过 Maven 管理多个模块。
- MySQL 保存业务数据，Redis 用于会话、验证码和缓存，Elasticsearch 提供默认的全站搜索。

## 本地运行

准备 JDK 17、Maven、MySQL 和 Redis。前端需要 Node.js 20.x（至少 20.19）或 22.12 及以上。项目默认使用 Elasticsearch，本地配置基于 8.19.17。

1. **准备数据库和服务。** 按 [本地开发环境](LOCAL_ENVIRONMENT.md) 准备 `nebula_blog` 数据库和服务连接。基础表定义位于 [database/nebula_blog](database/nebula_blog)，后续表结构和搜索迁移位于 [database/migrations](database/migrations)。应用启动前需要完成数据库初始化和所需迁移。
2. **启动后端。** 设置 `NEBULA_DB_USERNAME`、`NEBULA_DB_PASSWORD` 和 `NEBULA_JWT_SECRET`，并按实际服务设置 Redis、搜索和邮件配置。用 IDE 将根目录的 `pom.xml` 导入为 Maven 项目，运行 `nebula-start` 模块中的 `com.nebula.start.NebulaStartApplication`。后端默认使用 `http://localhost:8080`。
3. **启动前端。** 在仓库根目录执行以下命令。

   ```sh
   cd nebula-front
   npm ci
   npm run dev
   ```

   打开 `http://localhost:5173`。开发服务器会把 `/api` 和 `/upload` 请求转发给后端。

## 仓库导航

| 位置 | 内容 |
| --- | --- |
| [nebula-front](nebula-front) | Vue 前端。此目录由独立前端仓库通过 Git subtree 同步，可用于本地运行。 |
| [nebula-start](nebula-start) | Spring Boot 启动入口、应用配置和集成测试。 |
| [nebula-api](nebula-api) | 聚合接口、全站搜索与索引同步。 |
| `nebula-article`、`nebula-comment`、`nebula-media` | 写作与阅读、评论和图片管理。 |
| `nebula-auth`、`nebula-user`、`nebula-role` | 注册登录、用户资料、角色与权限。 |
| [database](database) | 基础表定义与数据库迁移。 |

环境变量和服务设置见 [本地开发环境](LOCAL_ENVIRONMENT.md)。前端同步流程见 [Git subtree 指南](git-subtree-guide.md)。功能变更见 [更新日志](更新日志.md)，问题状态和已完成的修复分别见 [问题清单](ISSUES.md) 与 [修复记录](FIXES.md)。
