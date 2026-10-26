# Nebula 前端

Nebula 博客的 Vue 3、TypeScript 与 Vite 前端。本目录是实际开发目录，后端中的 `nebula-front` 仅用于 Git subtree 镜像，不在镜像中开发。

## 维护记录与环境

- [项目维护约定](AGENTS.md)
- [本地开发环境](../../IDEA_project/nebula-blog/LOCAL_ENVIRONMENT.md)：前后端路径、工具、服务连接与本地配置。
- [更新日志](../../IDEA_project/nebula-blog/更新日志.md)：功能补全、界面优化与维护整理，首页介绍和交互规则也记录在这里。
- [问题清单](../../IDEA_project/nebula-blog/ISSUES.md)：待处理、待确认和待验证事项。
- [修复记录](../../IDEA_project/nebula-blog/FIXES.md)：已经完成的问题修复。
- [Git subtree 指南](../../IDEA_project/nebula-blog/git-subtree-guide.md)：按用户要求提交时使用，不代表自动获得提交或同步授权。

本目录不另建问题、修复或更新记录；界面调整的历史和验证结果统一查看后端记录。

## 本地运行

使用项目 `package.json` 中 `engines.node` 指定范围的 Node.js。本机工具和服务配置见上方环境文档。

首次准备依赖时，在本目录执行：

```sh
npm ci
```

启动前端：

```sh
npm run dev
```

默认访问 `http://localhost:5173`；开发代理把 `/api` 和 `/upload` 转给 `http://localhost:8080`。实际业务接口需要后端及其依赖服务可用，服务未启动不等同于代码故障。客户端登录状态与权限由路由守卫和服务端共同校验，不通过修改本地缓存跳过验证。

任务结束默认关闭项目前后端，不随之关闭 MySQL、Redis 或 Docker，也不操作 Windows 服务。

## 页面入口

| 页面 | 路径 |
| --- | --- |
| 客户端首页 | `/` |
| 文章列表与详情 | `/article`、`/article/:id` |
| 标签与分类 | `/tag`、`/category` |
| 个人主页 | `/user/:userId` |
| 全站搜索 | `/search` |
| 写作与编辑 | `/write`、`/write/:id` |
| 媒体管理 | `/media` |
| 后台管理 | `/admin` |

以上是路由入口，不代表匿名或任意账号均可访问；具体登录、角色和业务权限见 `src/router` 与对应服务端规则。

暂缓板块的可见性集中在 `src/constants/client-features.ts`。社区目前收起入口，访问旧地址 `/community` 返回首页；恢复前先确认对应页面已准备好。个人页已有关注、粉丝功能保持独立可用。

## 代码位置

| 内容 | 目录或文件 |
| --- | --- |
| 客户端布局与导航 | `src/layout/ClientLayout.vue`、`src/components/client/home/ClientHeader.vue` |
| 首页页面组合 | `src/views/home/index.vue` |
| 首页介绍板块与装饰图形 | `src/components/client/home` |
| 首页板块脚本 | `src/components/client/home/logic` |
| 客户端业务页面 | `src/views/home`；脚本优先放在各业务同级的 `logic` 目录 |
| 后台页面与公共样式 | `src/views/admin`、`src/views/admin/styles` |
| 接口、状态与路由 | `src/api`、`src/stores`、`src/router` |
| 回归测试 | `tests` |

首页介绍板块与真实业务列表分开维护；不要为展示示例文案而新增真实分类、标签、文章或虚构统计。自行维护的代码、样式、模板、配置和测试说明注释使用中文。

## 验证与构建

```sh
npm run test:security
npm run type-check
npm run build-only
```

`test:security` 执行 `tests/*.test.mjs` 中的现有回归；`type-check` 检查 TypeScript 和 Vue 类型；`build-only` 生成生产资源。也可使用 `npm run build` 同时进行类型检查与构建。只预览已有构建时使用 `npm run preview`。

检查本轮文件可使用 `npx --no-install eslint <文件路径>`。`npm run lint` 带自动修复，`npm run format` 会批量格式化源码，使用前确认范围，避免改动他人的未提交成果。

真实浏览器截图和本地验证日志保存在后端 `nebula-start/target/`，不提交 Git；演示状态的截图应明确标注，不作为真实登录、接口或部署验收的依据。
