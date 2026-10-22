# 文章正文与预览 XSS 修复

完成日期：2026-10-21。对应 [Security 检查](SECURITY_CHAIN_REVIEW.md) 第 3 项。

## 生效行为

文章正文和后台编辑预览现共用 [renderMarkdown](D:/Webstorm_project/nebula_blog/src/utils/markdown.ts)，统一执行：

1. Markdown-it 关闭原始 HTML，将手写 HTML 显示为文本。
2. 保留 Markdown 标题、强调、列表、引用、表格、图片、链接和代码块。
3. 代码高亮输出之后，再由 DOMPurify 按标签和属性白名单清洗最终 HTML。
4. 白名单不包含脚本、iframe、SVG、MathML、表单、事件属性、内联样式、id/name 等。链接沿用 Markdown-it 的协议检查，并经过最终清洗。
5. 代码围栏的属性由 Markdown-it 编码；未知语言或高亮异常时显示转义代码。
6. 浏览器不支持清洗时，退回到转义的纯文本，避免返回未处理 HTML。

表格对齐转换成限定值的 align 属性，不开放任意内联 CSS。代码高亮使用本地打包的暗色主题；正文、引用和预览文字使用项目的星云主题颜色。

既有 Markdown 源文本不改写。原来依赖手写 HTML 的文章片段会显示标签文本，可改用 Markdown 的图片、表格等语法。代码示例中的 HTML 仍可正常阅读。

## 日志清理

删除 LoginForm 对整个登录响应的 console.log，避免继续把 Token 输出到控制台；同时删除文章评论列表的响应调试输出。

本轮没有更换 Token 存储方式，也没有轮换 JWT 密钥或撤销既有凭证，这些属于审查清单的后续项。

## 依赖与测试

- 新增运行依赖 DOMPurify 3.4.17，package-lock.json 锁定版本。
- 新增仅用于测试的 jsdom 30.1.3；生产浏览器代码不依赖 jsdom。
- 新增 [26 项安全回归](D:/Webstorm_project/nebula_blog/tests/markdown-security.test.mjs)，全部通过。
- 涉及文件 ESLint、Prettier、完整前端类型检查和 Vite 生产构建通过。构建仍有既有依赖注释与大包提示。

专项测试包含 HTML 事件、脚本、SVG/MathML/iframe、DOM clobbering、危险协议与编码变体、围栏语言/图片 alt 属性注入；同时验证正常排版、表格对齐、图片链接和代码高亮。通过模拟高亮器返回危险 HTML，单独验证最终清洗层有效；还覆盖高亮异常与不支持清洗的回退路径。

测试使用 jsdom 解析和检查真实输出 DOM，不启用脚本执行或外部资源加载。当前在本机 Node 22.22.2 验证；运行新增测试需满足 jsdom 30.1.3 的 Node 要求（^22.22.2、^24.15.0 或 >=26），生产前端的构建要求保持原配置。

复现命令，在 `D:\Webstorm_project\nebula_blog` 运行：

```powershell
npm run test:security
npm run type-check
npm run build-only
```

## 浏览器验收

- 在后台“新增文章”的未保存表单中输入普通 Markdown、代码、原始事件属性和危险链接测试样例。
- 预览保留标题、表格与高亮；原始 HTML 只显示文本。实际 DOM 中可执行标签、事件属性、危险链接均为 0，测试标记执行次数为 0。
- 取消该表单，没有创建测试文章、发布恶意内容或修改既有正文。
- 用已授权的 admin 账号重新登录，正常进入超级管理员后台；本次登录产生的控制台业务日志为 0，没有 Token 输出。
- 打开真实文章 `/article/14`，正文正常渲染，22 个代码块使用统一输出；未出现可执行标签或事件属性。

浏览器截图及结果：

- `C:\Users\包\.codex\visualizations\2026\10\18\01a14f31-c748-7751-b7e0-5d38574654a2\markdown-xss-safe-preview.jpg`
- 同目录 `markdown-xss-browser-check.json`（仅保存验证计数，不含 Token 或正文）。

本轮仅修改前端渲染、样式、日志与测试配置；未修改后端代码或数据库结构。正常登录和阅读验收可能产生常规登录日志、浏览计数。

页面验收完成后客户端中断。重启后代码、依赖、构建记录和截图均保留；已补齐验收计数文件，并恢复服务：后端 8080（PID 41156），前端 5173（PID 16844）。当前运行日志在 `nebula-start/target/markdown-security-review/` 的 `backend-8080*.log`、`frontend-5173*.log`。上述浏览器验收结果记录的是客户端重启前已完成的检查。

## 方案依据

- [Markdown-it 官方安全说明](https://github.com/markdown-it/markdown-it/blob/master/docs/safety.md)：优先关闭原始 HTML，并注意扩展生成的输出。
- [DOMPurify 官方使用与配置说明](https://github.com/cure53/DOMPurify)：采用最终输出清洗与白名单配置，清洗后不再交给其他 HTML 转换逻辑。
