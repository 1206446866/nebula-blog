# 登录账号状态与日志修复（2026-10-21）

已修复 Security 清单接下来的两项 P2：邮箱认证未检查账号状态，以及缺少 `User-Agent` 时登录日志空指针导致认证失败。

## 账号状态

- 邮箱、手机号认证 Provider 在密码验证成功后，通过 Spring Security 的 `AccountStatusUserDetailsChecker` 检查账号状态，检查通过才返回已认证身份。禁用、锁定账号不会进入 JWT 签发和 Redis 会话登记。
- `AuthLoginUser.isAccountNonLocked()` 根据 LOCKED 状态返回结果，NID 登录的 Dao Provider 因此也能正确返回“用户已锁定”，而不是混同为禁用。
- 非正常、未知、缺失状态均不允许登录；身份对象也拒绝已逻辑删除的账号，防止异常 Mapper 返回删除数据时继续认证。正常账号的角色、权限、Token 和会话行为保持兼容。
- 手机号 Provider 已纳入实际 AuthenticationManager 验证；本轮没有新增公开手机号登录入口，现有 `/auth/login` 仍按邮箱/NID 路由。

## 登录日志

- 缺失、空白、无法识别、返回不完整结果或抛出运行时异常的 User-Agent 解析，使用 `Unknown` 浏览器、系统和设备信息，继续保存登录日志。
- User-Agent 入库及解析前最多保留 512 个 Unicode 字符，控制字符替换为空格；浏览器和系统名称最多 50 字符，适配现有字段长度，不截断有效代理对。
- 正常桌面、移动浏览器仍保留浏览器、系统和 PC/Mobile 信息。不再将完全未知的客户端默认认作 PC。
- 成功和失败登录均验证了真实日志写入。没有通过捕获数据库异常或跳过日志保存来掩盖错误；日志仍使用 MyBatis-Flex 保存。

## 验证

与此前一致的后端安全及业务回归 **155 项通过，失败/错误均为 0**，包含新增 39 项：

| 范围 | 数量 | 验证内容 |
| --- | --- | --- |
| Provider 单元验证 | 12 | 邮箱、手机号的正常、禁用、锁定、删除、未知/缺失状态、错误密码及不存在账号 |
| 日志元信息验证 | 5 | 缺失请求头、解析异常、空/不完整解析结果、控制字符和补充 Unicode 字符长度限制 |
| 真实 MySQL/Flex + Redis + Security + 日志集成验证 | 22 | 邮箱/NID 状态拒绝与无 Token 签发；正常桌面和移动登录；缺失、空白、异常和过长 User-Agent；错误密码和逻辑删除账号；实际 AuthenticationManager 的手机号 Provider 状态检查 |

新集成测试使用随机隔离账号，账号与日志写入在测试事务中回滚，登录产生的 Redis 会话逐个撤销；没有修改既有账号的状态、角色、邮箱或密码，没有发送邮件。前端代码未修改。

回归命令（Java 17、已有本地 Maven 缓存）：

```powershell
$env:JAVA_HOME = 'D:\jdk17'
& 'D:\develop\apache-maven-3.9.5\bin\mvn.cmd' -o '-Dmaven.repo.local=D:\MavenRepository' '-Dstyle.color=never' '-Dtest=*Test,!*ApplicationTests' '-Dsurefire.failIfNoSpecifiedTests=false' -pl nebula-start -am test
```

范围说明：无筛选执行全部 Maven 测试时，原有 `NebulaArticleApplicationTests.contextLoads` 因独立模块缺少 `RedisService` Bean 而失败。本轮未改动这些模块的空启动模板；155 项通过指上面明确筛选的安全与业务回归，不表示默认全量 Maven 测试已通过。

日志与运行验收记录位于 `nebula-start/target/login-integrity-review/`。未修改数据库结构、JWT 密钥或 Docker 配置，未提交或推送，未改动 `nebula-front` 镜像。

运行中接口验收通过：使用已授权 admin 账号，不设置 User-Agent（实际请求头数量为 0），登录返回 HTTP 200/业务码 200，新会话可以访问本人接口；随即退出撤销本次会话，重放返回 HTTP 401。只产生一次正常登录日志，没有改动该账号资料或密码。前端页面 HTTP 200。验收结果保存在 `http-acceptance.json`，不记录密码或 Token。

修复已加载到后端 8080（PID `50904`），前端恢复至 5173（PID `52560`）。运行日志为同目录 `backend-8080*.log` 与 `frontend-5173*.log`。本轮未操作 Windows 服务；之后关闭项目服务按用户约定以项目前后端为主。

下一项未修复 P2 为 Security 链 CORS 预检，之后是 HTTP 状态和异常信息一致性。按用户要求，本轮只处理上述两项。
