# Git Subtree 前后端项目管理说明

## 1. 项目结构

Nebula 项目采用前后端分离开发，同时使用 Git Subtree
将前端同步到后端主仓库。

    nebula-blog
    ├── nebula-start
    ├── nebula-common
    ├── nebula-auth
    ├── nebula-user
    ├── nebula-role
    ├── nebula-article
    └── nebula-front

后端主仓库：

    D:\IDEA_project\nebula-blog

前端独立仓库：

    D:\Webstorm_project\nebula_blog

------------------------------------------------------------------------

## 2. Git Subtree 作用

前后端最初独立维护：

-   后端：Spring Boot 项目
-   前端：Vue3 项目

通过 Git Subtree，将前端同步到主仓库：

    前端仓库
        |
        | git subtree pull
        ↓
    nebula-blog/nebula-front
        |
        ↓
    GitHub 主仓库

这样可以：

-   保持前端独立开发
-   保留完整项目展示
-   不需要移动原项目目录

------------------------------------------------------------------------

## 3. 日常开发流程

### 前端开发

使用 WebStorm：

    D:\Webstorm_project\nebula_blog

修改代码后：

``` bash
git add .
git commit -m "feat: update frontend feature"
git push
```

提交到前端独立仓库。

------------------------------------------------------------------------

## 4. 同步前端到主仓库

进入后端根目录：

``` powershell
cd D:\IDEA_project\nebula-blog
```

执行：

``` powershell
git subtree pull --prefix nebula-front frontend master --squash
```

参数：

-   `--prefix nebula-front`：同步目标目录
-   `frontend`：前端仓库 remote 名称
-   `master`：前端分支
-   `--squash`：压缩前端历史提交

------------------------------------------------------------------------

## 5. 推送主仓库

同步完成：

``` powershell
git status
```

确认无误：

``` powershell
git push origin main
```

GitHub 中最终结构：

    nebula-blog

    ├── Backend
    │
    └── nebula-front

------------------------------------------------------------------------

## 6. 注意事项

不要直接开发：

    D:\IDEA_project\nebula-blog\nebula-front

这里是 subtree 同步目录。

正确开发位置：

    D:\Webstorm_project\nebula_blog

修改完成后：

    前端 commit/push
            ↓
    subtree pull
            ↓
    主仓库 push

------------------------------------------------------------------------

## 7. 查看远程仓库

查看 Git remote：

``` bash
git remote -v
```

示例：

    frontend D:\Webstorm_project\nebula_blog
    origin https://github.com/1206446866/nebula-blog.git

------------------------------------------------------------------------

## 8. 当前项目 Git 架构

    WebStorm
        |
        | commit/push
        ↓
    前端独立仓库

        |
        | subtree pull
        ↓

    IDEA 主仓库

    ├── Spring Boot Backend
    └── nebula-front

        |
        | push

        ↓

    GitHub
