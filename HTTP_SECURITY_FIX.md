# CORS 与错误响应修复（2026-10-21）

已修复 Security 清单最后两项明确列出的 P2：匿名跨域预检被认证拦截，以及方法权限/全局异常的 HTTP 状态不一致、错误详情泄露。

## CORS

`CorsConfig` 提供唯一的 `UrlBasedCorsConfigurationSource`，Security 链通过 `.cors(...)` 接入，在 JWT 和认证检查前处理合法预检。删除原先仅在 MVC 生效的独立配置，避免两套策略分歧。这符合 [Spring Security CORS 官方说明](https://docs.spring.io/spring-security/reference/6.5/servlet/integrations/cors.html)。

- 默认允许源为 `http://localhost:5173` 与 `http://127.0.0.1:5173`，均为完整源匹配；相似域名、`null` 源和其他端口不在默认允许范围。
- 允许方法为 GET、HEAD、POST、PUT、PATCH、DELETE、OPTIONS；允许请求头为 Authorization、Content-Type、Accept，预检缓存 3600 秒。
- 使用 Bearer Token，不启用跨域 Cookie 凭证。没有对所有 OPTIONS 请求添加匿名放行规则：只有合法 CORS 预检在 CORS 层结束，实际请求和普通 OPTIONS 仍经过原有认证与授权。
- 部署源可通过配置 `nebula.cors.allowed-origins` 覆盖，多个完整源以逗号分隔，例如启动参数 `--nebula.cors.allowed-origins=https://blog.example.com`。生产配置应填写实际可信源。
- 拒绝的 CORS 请求保留 Spring 的 HTTP 403 与固定 `Invalid CORS request` 响应；认证和 MVC 错误继续使用项目 JSON 结构。允许源上的 401、403、4xx、5xx 响应也包含允许源头，浏览器可以读取并展示错误。

## HTTP 状态和错误信息

全局异常处理使用真实 HTTP 状态，保留现有 `code/message/data` 结构和业务码。业务码与 HTTP 状态单独定义，不按业务码的数字前缀推断：例如“用户不存在”的业务码仍为 `40101`，HTTP 为 404；评论无权操作的业务码 `5002` 对应 HTTP 403。

| 场景 | HTTP | 对外行为 |
| --- | --- | --- |
| 未登录、失效 Token、登录认证失败 | 401 | 保留原登录失败业务码与受控提示；不签发 Token |
| 方法权限拒绝、资源操作无权 | 403 | 权限拒绝统一为“权限不足”，资源业务错误使用其固定提示 |
| 参数校验、参数类型错误、非法 JSON | 400 | 固定格式提示或预定义校验提示，不返回原始输入、内部类型和方法路径 |
| 不存在/不可见资源 | 404 | 保留业务码，草稿不可见仍与不存在资源相同 |
| 业务状态/唯一约束冲突 | 409 | 业务使用预定义提示；未转换的数据库约束异常使用固定“数据冲突，请检查后重试” |
| 方法、内容类型、上传大小错误 | 405 / 415 / 413 | 固定提示；405 保留 Allow 响应头 |
| 未知运行时、检查型异常、数据库故障 | 500 | “服务器异常，请稍后重试”，完整异常保留在服务端日志 |
| 已有注册/会话基础设施不可用 | 503 | 保留已有安全提示和重试语义 |

查询对象字段转换失败单独识别，不把 Spring 的绑定错误原文发给客户端。授权拒绝不再拼接异常消息，未知异常也不再返回 `e.getMessage()`。确定可展示的业务异常仍使用错误码枚举中的固定消息。

原先用普通 RuntimeException 表达的旧密码错误、确认密码不一致、头像输入错误与权限格式错误，改为明确的业务异常，避免被统一兜底后误显示为服务器故障。旧密码错误、确认不一致分别为业务码 `40020`、`40021` / HTTP 400；改密并发冲突为 `40920` / HTTP 409。没有修改密码校验、事务或 Token 撤销规则。

## 前端兼容

- 请求拦截器显示实际 HTTP 400/403/404/409/413 等响应中的安全业务提示，继续处理旧的 HTTP 200 + 非成功业务码。
- HTTP 500 等未知服务错误显示固定提示，503 保留原来的安全重试提示；网络故障有单独提示。
- 登录 API 显式禁止会话过期跳转：登录失败的 HTTP 401 显示密码/账号错误，不清理当前已有效的其他登录状态。
- 受保护请求的 401 仍只清理发起该请求的同一会话，迟到的旧请求不能清理新 Token。
- 登录表单等待请求、阻止重复点击，捕获请求失败并恢复按钮。

## 验证

后端安全与业务回归 **185 项通过，失败/错误均为 0**，包括新增 30 项完整 CORS → Security → MVC 集成测试。前端安全测试 **47 项通过**，包括新增 13 项实际请求拦截器/API 模块验证；相关文件 ESLint、Prettier、类型检查与构建通过。构建仍有此前的大分包及依赖 PURE 注释提示。

新增后端验证覆盖可信/非法源、方法和请求头；预检先于 JWT 解析；实际请求仍鉴权；真实无权限账号的方法拒绝；业务码与 HTTP 的独立映射；未知/数据库异常脱敏和服务端日志；绑定/JSON/校验错误；405/415/404。数据库操作全部使用 MyBatis-Flex，隔离测试账号与相关写入事务回滚，Redis 测试会话逐个撤销，没有修改既有账号、文章或评论。

回归命令：

```powershell
$env:JAVA_HOME = 'D:\jdk17'
& 'D:\develop\apache-maven-3.9.5\bin\mvn.cmd' -o '-Dmaven.repo.local=D:\MavenRepository' '-Dstyle.color=never' '-Dtest=*Test,!*ApplicationTests' '-Dsurefire.failIfNoSpecifiedTests=false' -pl nebula-start -am test
```

范围沿用上轮：原有独立模块 `*ApplicationTests` 空启动模板因依赖不齐未纳入安全/业务回归；本轮没有重跑或修复这些模板，不宣称默认不筛选的 Maven 全量测试通过。

运行中接口 **16 项验收通过**，包括真实预检 200、非法源 403、认证 401、参数 400、资源 404、方法 405、类型 415、前端代理错误转发，以及登录失败后原会话仍可使用。使用授权 admin 账号只产生一次正常登录日志，随后立即撤销验收会话，重放返回 401。没有发送邮件或修改真实资料；记录不包含密码或 Token。另确认仅用于测试的异常探针端点在运行服务中不存在（404）。

## 当前服务与留档

- 后端已加载修复：8080，PID `35348`；前端继续运行：5173，PID `52560`。
- 后端测试、运行及 HTTP 验收：`nebula-start/target/http-security-review/`，包含 `security-regression.log`、`http-boundary.log`、`http-acceptance.json`、`backend-8080*.log`。
- 前端验证：`D:\Webstorm_project\nebula_blog\http-security-tests.log`、`http-security-lint.log`、`http-security-build.log`。
- 没有操作 Windows 服务、修改 Docker 配置或数据库结构，JWT 密钥仍按用户要求暂缓。未提交或推送，未修改 `nebula-front` subtree 镜像。

原清单明确列出的 P2 已处理完毕。登录限速、验证码失败次数/邮件冷却原子性、可信代理 IP 边界、管理员重置密码占位及文章状态权限命名仍属于此前保留的独立审查事项，未在本轮扩展处理。
