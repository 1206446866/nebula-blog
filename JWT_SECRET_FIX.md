# JWT 签名密钥隔离与轮换（2026-10-21）

原问题：受 Git 跟踪的 `application.yaml` 直接包含 JWT 签名密钥，所有使用该配置的环境共用同一个值。现已移除该字面量，并为本地服务生成、加载全新的随机密钥；旧密钥签发的 Token 已通过实际接口确认返回 HTTP 401。

## 配置与运行

- `jwt.secret` 改为 `${NEBULA_JWT_SECRET:}`。无固定默认密钥，也不会在应用每次启动时自动生成临时密钥。
- 本机密钥位于 `nebula-start/.secrets/NEBULA_JWT_SECRET`，为 32 字节密码学随机数据的标准 Base64 表示。目录仅允许当前 Windows 用户访问；`.secrets/` 已加入 Git 忽略规则，文件位于资源目录之外，不进入应用资源。
- Spring 配置树兼容从仓库根目录或 `nebula-start` 模块目录启动，现有 IDEA 启动方式无需手填密钥。操作系统环境变量 `NEBULA_JWT_SECRET` 优先于本地文件；不同部署环境应分别注入各自的随机密钥，不复制本机密钥。
- 缺失、空白、非法 Base64、解码不足 32 字节的密钥使初始化失败；错误信息不附带密钥值或解码异常。Token 有效期必须大于零，继续采用现有 7 天配置。
- `JwtUtil` 在构造时初始化不可变的签名密钥和解析器，移除公开 setter；签发与接受的签名算法均为 HS256。现有 Redis 会话、退出撤销、改密失效及数据库权限重载继续生效。
- 本次更换了正在运行的服务密钥，原登录需要重新登录一次。使用相同密钥重启服务不会使新 Token 失效。没有修改现有账号密码、角色、数据库结构或 Docker 配置，没有操作 Windows 服务。

首次配置本地开发密钥：

```powershell
.\scripts\Initialize-JwtSecret.ps1
```

脚本不显示密钥值，存在密钥时拒绝意外覆盖。明确需要让所有当前登录失效时，执行下列命令，然后重启后端：

```powershell
.\scripts\Initialize-JwtSecret.ps1 -Rotate
```

轮换使用同目录临时文件原子替换，不生成包含旧密钥的备份。部署若通过环境变量或挂载目录注入密钥，应更换相应部署密钥来源；本地脚本不修改系统环境变量。

Git 历史中的旧值不会因为修改当前配置而消失，本次通过实际换钥使它无法继续签发可用登录凭证；没有重写用户的 Git 历史。其他仍使用旧值的部署也需分别换钥和重新登录。

## 验证

后端安全与业务回归 **252 项通过**，新增 19 项：

- 15 项密钥与签名验证：缺失/空白、非法编码、按解码字节数判断强度、32/48/64 字节密钥、HS256、其他算法/无签名拒绝、有效期校验，以及同密钥重启与换钥行为。
- 3 项使用实际应用 YAML 的配置加载验证：外部文件、环境变量覆盖、空环境变量不回退到本地密钥。
- 1 项完整 Security + 真实 Redis/MySQL 验证：即使旧密钥 Token 的 Redis 会话仍有效，当前接口也返回 401。集成测试每次 JVM 启动生成独立测试密钥，不使用开发或部署密钥。

回归命令（另为本次执行设置随机 `nebula.security.abuse.key-prefix` 隔离限流键）：

```powershell
$env:JAVA_HOME='D:\jdk17'
& 'D:\develop\apache-maven-3.9.5\bin\mvn.cmd' -o '-Dmaven.repo.local=D:\MavenRepository' '-Dstyle.color=never' '-Dtest=*Test,!*ApplicationTests' '-Dsurefire.failIfNoSpecifiedTests=false' -pl nebula-start -am test
```

继续排除既有独立模块的 `*ApplicationTests` 启动模板；它们缺少完整应用依赖，不能据此称默认全仓测试全部通过。前端未修改，未重复执行上一轮已通过的前端检查。

运行中 HTTP 验收 **11 项通过**：更换前 Token 可访问、更换后旧 Token 401、新登录与访问正常、退出后重放 401、前端代理拒绝旧 Token，以及模块目录与根目录加载相同密钥。临时 18080 后端已停止，测试登录会话和 11 个隔离限流键已清理。后端现在运行 PID `48068`，前端仍为 PID `52560`。

密钥脚本额外验证了首次生成、拒绝非显式覆盖、显式轮换、临时文件清理和仅当前用户访问。对源码与文档进行内存比对，确认没有出现当前密钥值；`git check-ignore` 确认实际密钥文件不会参与提交。

留档位于 `nebula-start/target/jwt-secret-review/`：`security-regression.log`、`http-acceptance.json`、`secret-script-acceptance.json` 和 `backend-8080*.log`。报告、测试输出和运行参数不记录 JWT 或密钥值。

实现参考：[JJWT 密钥格式说明](https://github.com/jwtk/jjwt#secretkey-formats)、[Spring Boot 外部配置与配置树](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html#features.external-config.files.configtree)。

## 当时新发现的配置问题（后续已修复）

原 Security 审查清单中的 P1/P2 及独立边界已全部修复。当时另外确认的以下两项问题也已在后续完成修复：数据库切换为受限专用账号、轮换旧 root 密码、8 份配置改为外部注入，邮件调试默认关闭；259 项后端回归与 6 项 HTTP 验收通过，前后端已按要求关闭。见 [凭据与邮件配置修复记录](CONFIG_CREDENTIALS_FIX.md)。以下保留当时的问题描述：

1. **P1：数据库连接密码仍是 Git 跟踪文件中的字面量。** 启动模块及 article、auth、category、comment、role、tag、user，共 8 份应用配置存在该情况。需要统一改为外部注入，并在数据库中轮换已记录的密码，确认实际应用账号权限符合最小权限要求。当前只确认代码中的凭据存储问题，不据此认定数据库公网可达或已遭访问。报告不记录密码。
2. **P2：SMTP 调试日志仍开启。** 启动配置 `spring.mail.properties.mail.debug: true` 可能将邮件收发元数据及验证码邮件内容写入控制台，应默认关闭并按需短时启用。邮件密码本身已经采用 `${MAIL_PASSWORD}` 环境变量；本次没有发真实邮件，因此不宣称动态复现了邮件内容泄露。
