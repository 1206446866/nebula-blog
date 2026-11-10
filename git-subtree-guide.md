# Git subtree 前后端管理指南

## 目录与分工

| 用途 | 路径 | 当前分支 |
| --- | --- | --- |
| 后端主仓库 | `D:/IDEA_project/nebula-blog` | `main` |
| 实际前端仓库 | `D:/Webstorm_project/nebula_blog` | `master` |
| 前端子树镜像 | 后端内的 `nebula-front` | 随后端仓库维护 |

前后端保持两个独立开发路径。前端始终在实际前端仓库修改，`nebula-front` 只接收子树同步，不直接开发。后端的 `frontend` remote 指向实际前端本地目录。实际前端目前未配置远端，前端内容随 `nebula-front` 一起推送到后端 `origin/main`。

问题、修复及功能记录分别见 [ISSUES.md](ISSUES.md)、[FIXES.md](FIXES.md) 和 [更新日志.md](更新日志.md)；环境与运行入口见 [LOCAL_ENVIRONMENT.md](LOCAL_ENVIRONMENT.md)。

## 提交顺序与授权

用户要求提交时，按 **提交实际前端 → 合入前端子树 → 提交后端 → 核对并推送** 的顺序处理，提交说明使用中文。用户明确要求仅本地提交时，止于提交。

用户的提交请求决定本次操作范围。操作前检查两个仓库的分支、差异和未跟踪文件，保留已有成果，按范围明确选择暂存文件。

## 操作参考

### 1. 检查两个工作区

```powershell
git -C D:\Webstorm_project\nebula_blog status --short
git -C D:\Webstorm_project\nebula_blog branch --show-current
git -C D:\IDEA_project\nebula-blog status --short
git -C D:\IDEA_project\nebula-blog branch --show-current
git -C D:\IDEA_project\nebula-blog remote -v
```

核对差异后，在实际前端仓库暂存本次需要提交的文件，再提交：

```powershell
git -C D:\Webstorm_project\nebula_blog diff --cached
git -C D:\Webstorm_project\nebula_blog commit -m "完善前端功能与页面交互"
```

说明应改为本次实际改动的中文概括。未提交的前端修改不会进入子树。

### 2. 合入前端子树

`git subtree pull` 要求后端工作区和暂存区干净。后端有未提交成果时，不直接执行下方命令，也不要为满足条件而清理、恢复文件或抢先提交后端。应先在临时集成工作区合入子树，再把后端成果纳入最终提交；具体步骤按当次差异制定，并保留原工作区及未跟踪文件。

后端工作区干净，且已核对 `frontend` remote 和前端分支后，使用：

```powershell
git -C D:\IDEA_project\nebula-blog subtree pull --prefix nebula-front frontend master --squash -m "同步前端子树"
```

此命令会创建子树同步提交。随后检查镜像与已提交前端内容是否一致；需要处理冲突时保留双方有效改动，不整目录覆盖。

### 3. 提交后端并推送

子树合入后，检查并暂存后端本次改动，使用中文说明提交，再核对两个仓库的最终状态。示例：

```powershell
git -C D:\IDEA_project\nebula-blog diff --cached
git -C D:\IDEA_project\nebula-blog commit -m "完善后端功能与维护记录"
```

核对镜像和目标分支后，推送后端 `main`：

```powershell
git -C D:\IDEA_project\nebula-blog push origin main
```

将来若为实际前端配置独立远端，先核对该远端和目标分支，推送前端后再推送后端。

没有后端独立改动时，不为凑步骤创建空提交；保留子树同步提交即可。
