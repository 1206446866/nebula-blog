# 后台管理接口与服务审查（2026-10-22）

本轮检查文章、用户、评论、角色权限、分类和标签的管理接口及对应服务，并对照前端实际调用和返回值处理。审查最初列出 9 组问题；其中 **8 组 P2 已全部修复**，见 [后台管理修复记录](ADMIN_MANAGEMENT_FIX.md)。第 1 项经用户确认属于尚未实现的功能，随后已按追加要求开发完成，见 [草稿管理功能记录](DRAFT_MANAGEMENT_FIX.md)。最新后端 312 项、前端 50 项回归和构建通过，前后端已关闭。下文保留修复前的复现证据与当时源码行号。

## 1. 草稿编辑或删除功能（已开发完成）

新建文章固定为草稿，但编辑和删除都调用 `checkOwnerOrAdmin()`，该方法先要求文章必须已发布，再判断作者或管理员身份。因此即使是超级管理员编辑自己的草稿，也收到 HTTP 404、业务码 40402。

修复前实际复现了草稿编辑和删除两条路径，均被拒绝且草稿仍存在。最初按写作流程阻断列为 P1，用户说明这是尚未完成的功能，上一轮暂缓。现按追加要求开发完成：有对应权限的作者及管理员可以直接管理草稿，保存保留未发布状态；具体实现和验证见 [草稿管理功能记录](DRAFT_MANAGEMENT_FIX.md)。

位置：[ArticleServiceImpl.java:287](D:/IDEA_project/nebula-blog/nebula-article/src/main/java/com/nebula/article/service/impl/ArticleServiceImpl.java:287)。建议将编辑/删除的归属授权与发布状态检查分开，允许合法作者及有权限管理员管理草稿。

## 2. P2：评论点赞接受管理角色伪造的用户身份（已修复）

`CommentServiceImpl.like()` 信任请求中的 `userId`；`checkOwnerOrAdmin()` 又允许 ADMIN 管理角色提交任意用户 ID，随后把该 ID 写入 `comment_like`。

实际使用仅绑定 `comment:update` 的管理角色复现了两种写入：

- 冒用另一个隔离用户的 ID，点赞记录归到对方名下。
- 提交不存在的 `Long.MAX_VALUE` 用户 ID，仍返回成功并保存记录。

该身份字段应固定取当前登录用户，不能把管理他人资源的权限解释为代替他人表达点赞。位置：[CommentServiceImpl.java:123](D:/IDEA_project/nebula-blog/nebula-comment/src/main/java/com/nebula/comment/service/impl/CommentServiceImpl.java:123)。

## 3. P2：用户管理分页丢失页码、页大小和总记录数（已修复）

服务查询后重新创建 `Page<UserVO>`，只复制 `records` 和 `totalPage`，遗漏 `pageNumber`、`pageSize`、`totalRow`。

实际创建 3 个隔离用户，以 `current=2&size=1` 查询，结果是第 2 个用户，但返回 `pageNumber=1`、`pageSize=10`、`totalRow=-1`。前端 `useTable` 会直接采用这些值，因此页码、每页条数和分页总数被错误覆盖。

位置：[UserServiceImpl.java:86](D:/IDEA_project/nebula-blog/nebula-user/src/main/java/com/nebula/user/service/impl/UserServiceImpl.java:86)，前端影响见 [useTable.ts:18](D:/Webstorm_project/nebula_blog/src/composables/useTable.ts:18)。建议保留原分页全部元数据，或用统一的分页转换方法。

## 4. P2：用户角色筛选未实现，多角色用户还会重复（已修复）

传入 `role` 后只连接 `user_role`，没有根据角色 ID、编码或描述增加筛选条件，也没有去重。

实际传入不存在的角色仍返回用户；给一个隔离用户绑定两个角色后，该用户在筛选结果中出现两次。用户名筛选仍有效，问题集中在角色条件和多角色连接。

位置：[UserServiceImpl.java:73](D:/IDEA_project/nebula-blog/nebula-user/src/main/java/com/nebula/user/service/impl/UserServiceImpl.java:73)。建议先明确前端角色筛选参数含义，再使用受约束的角色条件和 `EXISTS` 等不放大用户行数的查询方式。

## 5. P2：文章实际已删除，后台却提示删除失败（已修复）

文章删除忽略数据库删除返回值，直接把 Redis 缓存键的删除结果作为接口结果。缓存不存在时，数据库删除成功也会返回 `HTTP 200 / data=false`。

实际对一个未访问过、没有详情缓存的已发布隔离文章执行删除，SQL 原始行状态为 `deleted=1`，接口却返回 false。前端遇到 false 会显示“删除失败”并不刷新列表。这里用原始 SQL 状态核对结果，排除了测试事务内 ORM 一级缓存的影响。

位置：[ArticleServiceImpl.java:275](D:/IDEA_project/nebula-blog/nebula-article/src/main/java/com/nebula/article/service/impl/ArticleServiceImpl.java:275)，前端影响见 [ArticleManager.vue:268](D:/Webstorm_project/nebula_blog/src/views/admin/ArticleManager.vue:268)。建议以数据库写入结果决定成功，缓存失效按事务提交后处理。

## 6. P2：文章分类/标签引用可以无效，删除后仍返回已删除的 ID（已修复）

确认了三条实际路径：

- 新建文章传不存在的分类、标签 ID，接口返回成功，关联表保存这些 ID。
- 删除已有文章使用的分类后，文章关联仍在，详情继续返回已删除分类的 ID。
- 删除已有文章使用的标签后，详情继续返回已删除标签的 ID。

这会造成编辑下拉选项、文章元数据和筛选结果不一致。建议绑定前验证所有目标存在且未删除；删除目录项时明确采用“有关联则拒绝删除”或“事务内解绑”，并在提交后处理受影响文章缓存。

位置：[ArticleServiceImpl.java:142](D:/IDEA_project/nebula-blog/nebula-article/src/main/java/com/nebula/article/service/impl/ArticleServiceImpl.java:142)、[CategoryServiceImpl.java:49](D:/IDEA_project/nebula-blog/nebula-category/src/main/java/com/nebula/category/service/impl/CategoryServiceImpl.java:49)、[TagServiceImpl.java:49](D:/IDEA_project/nebula-blog/nebula-tag/src/main/java/com/nebula/tag/service/impl/TagServiceImpl.java:49)。

## 7. P2：文章筛选条件被忽略，已发布分页还会重复文章（已修复）

- 管理文章列表 `/articles` 接收 `categoryIds`、`tagIds`，但查询未使用这两项。实际只应匹配 1 篇的条件返回了 2 篇。
- `/articles/published` 直接连接标签关联表，缺少去重。实际 1 篇文章关联两个命中标签后，返回总数 2，第一页和第二页出现同一文章。

建议两个入口统一筛选语义，优先使用 `EXISTS`，确保记录分页与总数均按文章计算。已发布列表也应配置稳定排序。

位置：[ArticleServiceImpl.java:65](D:/IDEA_project/nebula-blog/nebula-article/src/main/java/com/nebula/article/service/impl/ArticleServiceImpl.java:65)、[ArticleServiceImpl.java:187](D:/IDEA_project/nebula-blog/nebula-article/src/main/java/com/nebula/article/service/impl/ArticleServiceImpl.java:187)。当前管理页面只使用标题筛选，分类/标签条件失效由直接接口调用确认，没有声称页面已经提供这些筛选按钮。

## 8. P2：管理接口参数校验有缺口（已修复）

实际确认：

- 用户状态可设为 `99`，返回成功并写入数据库，绕过正常/禁用/锁定的状态范围。
- 用户分页和角色分页接受 `size=101`；角色 DTO 虽有 `@Max(100)`，控制器参数缺少触发校验的 `@Valid`，用户分页参数也没有范围约束。
- 新建文章缺少分类时绕过请求校验，直到数据库非空约束返回 409；应在写入前按输入错误拒绝。相关事务在结束后确实回滚，没有确认到残留文章。

建议将状态约束限定到现有枚举，为分页统一限制 `current >= 1`、`1 <= size <= 100`，补齐创建参数的必填校验。

位置：[UserServiceImpl.java:95](D:/IDEA_project/nebula-blog/nebula-user/src/main/java/com/nebula/user/service/impl/UserServiceImpl.java:95)、[UserController.java:36](D:/IDEA_project/nebula-blog/nebula-user/src/main/java/com/nebula/user/controller/UserController.java:36)、[RoleController.java:40](D:/IDEA_project/nebula-blog/nebula-role/src/main/java/com/nebula/role/controller/RoleController.java:40)、[CreateArticleDto.java:32](D:/IDEA_project/nebula-blog/nebula-article/src/main/java/com/nebula/article/dto/CreateArticleDto.java:32)。

## 9. P2：删除自定义角色后残留权限关联（已修复）

角色删除只清理 `user_role`，没有清理 `role_permission`。实际删除一个包含权限和用户绑定的隔离角色后，用户绑定为 0，角色不可查询，但权限关联仍为 1。

当前未据此确认越权，主要影响关联数据及后续维护一致性。建议在同一事务内清理角色的用户和权限关联，并与权限分配操作保持一致的并发约束。

位置：[RoleServiceImpl.java:60](D:/IDEA_project/nebula-blog/nebula-role/src/main/java/com/nebula/role/service/impl/RoleServiceImpl.java:60)。

## 修复前的验证范围与留档

最后一轮执行共 **50 项**，失败/错误均为 0，含义分别是：

- 17 项问题复现成功：证明上面的问题确实存在，不能把这些断言称为业务正确性回归通过。
- 2 项新增拒绝对照：普通用户的管理写入返回 403，匿名管理访问返回 401。
- 31 项既有授权及管理边界回归通过，包括 SUPER_ADMIN 管理保护、角色权限分配边界及密码重置等。

复现使用完整 Spring Security、MockMvc、真实 MySQL 和 Redis。数据库访问全部通过 MyBatis-Flex；写入为隔离测试数据并在事务后核对回滚。用户、文章、分类、标签、角色和评论测试主记录残留均为 0。Redis 测试会话与文章缓存键按本次生成的精确键清理，没有修改既有账号、密码、角色权限或业务记录，没有发送邮件。

本轮没有启动前后端监听服务，也没有浏览器逐屏操作或完整重跑其他业务测试；前后端继续保持关闭，MySQL/Redis 运行状态未改。未完成真实并发下所有缓存、状态变更和目录删除竞争情况的动态验证，不将其扩展为已确认的问题。

问题复现源码已从正常测试目录移至以下留档位置，同时移除对应编译产物，避免后续把“允许问题存在”的断言混入正式回归：

- `nebula-start/target/admin-management-review/AdminManagementAuditTest.java`
- `nebula-start/target/admin-management-review/TEST-com.nebula.start.AdminManagementAuditTest.xml`
- `nebula-start/target/admin-management-review/admin-audit.log`
- `nebula-start/target/admin-management-review/observations.json`

第 2–9 项的复现断言现已改为正常业务预期并纳入正式回归，另增加并发与提交后缓存验证。第 1 项随后也已开发完成。实现与历史验收见 [后台管理修复记录](ADMIN_MANAGEMENT_FIX.md)，最新 312 项后端回归、草稿功能及服务关闭状态见 [草稿管理功能记录](DRAFT_MANAGEMENT_FIX.md)。
