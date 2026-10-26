import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { Role } from '@/constants/role.ts'
import { clientFeatures } from '@/constants/client-features'

const staticRoutes: RouteRecordRaw[] = [
  /** * 登录页 */
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/register',
    component: () => import('@/views/login/components/RegisterConfirm.vue'),
    meta: { public: true, title: '注册确认' },
  },
  {
    path: '/test',
    component: () => import('@/views/login/Test.vue'),
    meta: {
      title: '测试页面',
      public: true,
    },
  },
  /** 后台管理布局 */
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: {
      roles: [Role.SUPER_ADMIN, Role.ADMIN],
    },
    children: [
      {
        meta: { title: '管理员主页' },
        path: '',
        component: () => import('@/views/admin/index.vue'),
      },
      {
        path: 'role',
        component: () => import('@/views/admin/RoleManager.vue'),
        meta: {
          title: '角色管理',
          roles: [Role.SUPER_ADMIN],
        },
      },
      {
        path: 'permission',
        name: 'Permission',
        component: () => import('@/views/admin/PermissionManager.vue'),
        meta: {
          title: '权限管理',
          roles: [Role.SUPER_ADMIN],
        },
      },
      {
        meta: { title: '账号管理', permission: 'user:list' },
        path: 'user',
        component: () => import('@/views/admin/UserManager.vue'),
      },
      {
        meta: { title: '文章管理', permission: 'article:list' },
        path: 'article',
        component: () => import('@/views/admin/ArticleManager.vue'),
      },
      {
        path: 'comment',
        component: () => import('@/views/admin/CommentManager.vue'),
        meta: { title: '评论管理' },
      },
      {
        path: 'category',
        component: () => import('@/views/admin/CategoryManager.vue'),
        meta: {
          title: '分类管理',
          permission: 'category:list',
        },
      },
      {
        path: 'tag',
        component: () => import('@/views/admin/TagManager.vue'),
        meta: {
          title: '标签管理',
          permission: 'tag:list',
        },
      },
    ],
  },

  {
    path: '/',
    component: () => import('@/layout/ClientLayout.vue'),
    children: [
      { path: '', component: () => import('@/views/home/index.vue'), meta: { title: '首页' } },
      {
        path: 'search',
        name: 'SiteSearch',
        component: () => import('@/views/home/search/index.vue'),
        meta: { title: '全站搜索' },
      },
      {
        path: 'community',
        name: 'Community',
        // 收起的板块不加载页面，旧地址返回首页，避免出现未开放内容。
        ...(clientFeatures.community
          ? { component: () => import('@/views/home/community/index.vue') }
          : { redirect: '/' }),
        meta: { title: '社区' },
      },
      {
        path: 'media',
        name: 'MediaLibrary',
        component: () => import('@/views/home/media/index.vue'),
        meta: { title: '媒体库' },
      },
      {
        path: 'write',
        name: 'ArticleCreate',
        component: () => import('@/views/home/article/write/ArticleWriter.vue'),
        meta: { title: '写文章', permission: 'article:create' },
      },
      {
        path: 'write/:id',
        name: 'ArticleEdit',
        component: () => import('@/views/home/article/write/ArticleWriter.vue'),
        meta: { title: '编辑与预览', permission: 'article:update', idParam: 'id' },
      },
      {
        path: 'article',
        component: () => import('@/views/home/article/index.vue'),
        name: 'ArticleHome',
        meta: { title: '文章', permission: 'article:list', articleQuery: true },
      },
      {
        path: 'article/:id',
        name: 'ArticleDetail',
        component: () => import('@/views/home/article/detail/ArticleDetail.vue'),
        meta: { title: '文章详情', permission: 'article:list', idParam: 'id' },
      },
      {
        path: 'category',
        component: () => import('@/views/home/category/index.vue'),
        meta: { title: '分类主页', permission: 'category:list' },
      },
      {
        path: 'tag',
        component: () => import('@/views/home/tag/index.vue'),
        meta: { title: '标签主页', permission: 'tag:list' },
      },
      {
        path: 'user/:userId',
        name: 'UserProfile',
        component: () => import('@/views/home/profile/UserProfile.vue'),
        meta: { title: '用户主页', idParam: 'userId' },
      },
    ],
  },

  /**403*/
  {
    path: '/403',
    component: () => import('@/views/error/403.vue'),
    meta: { public: true, title: '无权访问' },
  },
  {
    path: '/404',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { public: true, title: '页面不存在' },
  },
  {
    path: '/session-error',
    component: () => import('@/views/error/SessionUnavailable.vue'),
    meta: { public: true, title: '暂时无法确认登录' },
  },
  /**404*/
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/404',
    meta: { public: true },
  },
]
const router = createRouter({ history: createWebHistory(), routes: staticRoutes })
export default router
