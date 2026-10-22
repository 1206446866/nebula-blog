# 数据库凭据隔离与邮件日志修复（2026-10-21）

本轮修复了 JWT 换钥时发现的两项配置问题：8 份受 Git 跟踪的数据库明文凭据，以及开启的邮件协议调试。已完成数据库实际切换、旧密码拒绝验证和业务回归，并按要求关闭前后端服务。

## 数据库账号与本机凭据

- 应用从 `root@localhost` 改为专用账号 `nebula_blog_app@localhost`，仅授予 `nebula_blog` 数据库的 `SELECT`、`INSERT`、`UPDATE`、`DELETE`，没有全局数据权限、建库建表、用户管理、授权或 `GRANT OPTION` 权限。实际确认应用账号可以读取业务表，读取 `mysql.user` 被拒绝。
- 原来写入代码的 `root@localhost` 密码已轮换为独立随机密码；使用新连接确认新密码可用、原密码返回 MySQL 1045。应用专用密码与 root 新密码分别生成，均含 32 字节密码学随机数据。
- 所有数据库操作，包括账号检查、创建、授权、改密和权限验证，均通过 **MyBatis-Flex**；没有使用 JDBC 直接执行语句、数据库命令行或其他数据库访问工具。没有修改业务表结构或既有业务账号密码、角色、文章等数据。
- start、article、auth、category、comment、role、tag、user 共 8 份应用配置统一读取 `${NEBULA_DB_USERNAME}`、`${NEBULA_DB_PASSWORD}`，不再包含数据库密码字面量或 root 默认账号。环境变量优先于本地配置树，兼容仓库根目录、启动模块目录及其他相关模块目录的本地启动方式。
- 启动模块增加凭据启动校验：缺失/空白凭据拒绝启动，显式配置 root 也会拒绝；错误信息不包含密码值。

当前本机连接参数：MySQL `localhost:3306`，数据库 `nebula_blog`。应用用户名和密码分别位于：

- `D:\IDEA_project\nebula-blog\nebula-start\.secrets\NEBULA_DB_USERNAME`
- `D:\IDEA_project\nebula-blog\nebula-start\.secrets\NEBULA_DB_PASSWORD`

**IDEA 中保存旧 root 密码的数据库连接需要更新。** 管理账号仍为 `root@localhost`，新的管理密码保存在：

- `D:\IDEA_project\nebula-blog\nebula-start\.admin-secrets\ROOT_PASSWORD`

上述目录的 Windows 访问控制仅允许当前用户，均被 Git 忽略且位于应用资源目录之外。管理密码目录不由应用配置树导入。本地文件保存密码原值，不在报告、源码、运行参数或验收日志中打印。旧 root 密码的临时迁移文件已删除。

新的开发环境或部署应创建对应的独立受限数据库账号，并通过环境变量或受保护的配置挂载提供凭据，不复制本机密码，也不向 Git 提交本地凭据文件。Git 历史中的旧密码未被重写，但已不再能建立新的 root 连接。

## 邮件调试

`spring.mail.properties.mail.debug` 默认改为 `false`。通过实际应用 YAML 和邮件自动配置创建 `JavaMailSenderImpl`，确认实际邮件会话 `getDebug()` 为 false，邮件用户名/密码仍分别读取原 `MAIL_USERNAME`、`MAIL_PASSWORD`，未与数据库凭据混用。本轮只创建测试邮件对象，未连接 SMTP 或发送真实邮件。

## 验证与服务关闭

- **259 项后端安全/业务回归通过**，全部使用受限应用数据库账号执行；原 252 项之外新增 7 项配置验证，覆盖实际文件加载、环境变量覆盖、空密码不回退、空账号、root 配置拒绝、缺失占位符以及真实邮件会话调试关闭。
- **6 项实际 HTTP 验收通过**：匿名访问 401、管理员登录、鉴权读取业务数据、退出、旧会话重放 401、非法登录输入 400。验收使用加载新配置的临时后端。
- 继续排除既有独立模块的 `*ApplicationTests` 启动模板；不据此称默认全仓测试全部通过。前端未修改，未重复运行前端构建。
- 扫描全部受 Git 跟踪的后端应用配置，未发现密码或签名密钥字面量；本地凭据忽略规则、目录权限和当前密码未出现在源码/日志中均已核对。
- 测试登录会话、隔离限流键已清理。按用户要求停止前端 PID `52560`、原后端 PID `48068` 和临时验收后端；**5173、8080、18080 均未监听**。MySQL 3306 与 Redis 6379 保持运行，未操作 Windows 服务或基础设施配置。

记录位于 `nebula-start/target/config-secrets-review/`：`security-regression.log`、`db-provision.json`、`db-rotation.json`、`db-verification.json`、`http-acceptance.json`、`final-services.json`、`cleanup-verification.json`。数据库记录只包含账号名、授权和验证结果，不包含密码。

原 Security 审查问题及后续新增的两项配置问题现均已处理，本轮未发现新的待修问题；此结论限定于已审查和验证的范围。
