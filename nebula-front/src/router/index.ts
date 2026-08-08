import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { Role } from '@/constants/role.ts'

const staticRoutes: RouteRecordRaw[] = [
  /** * 登录页 */
  { path: '/login', component: () => import('@/views/login/index.vue') },
  {
    path: '/register',
    component: () => import('@/views/login/components/RegisterConfirm.vue'),
  },
  {
    path: '/test',
    component: () => import('@/views/login/Test.vue'),
    meta: {
      title: '测试页面',
    },
  },
  /** * AdminLayout */
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
          // roles: [Role.SUPER_ADMIN],
        },
      },
      {
        path: 'permission',
        name: 'Permission',
        component: () => import('@/views/admin/PermissionManager.vue'),
        meta: {
          title: '权限管理',
          // roles: [Role.SUPER_ADMIN],
        },
      },
      {
        meta: { title: '账号管理' },
        path: 'user',
        component: () => import('@/views/admin/UserManager.vue'),
      },
      {
        meta: { title: '文章管理' },
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
        },
      },
      {
        path: 'tag',
        component: () => import('@/views/admin/TagManager.vue'),
        meta: {
          title: '标签管理',
        },
      },
    ],
  },

  {
    path: '/',
    component: () => import('@/layout/ClientLayout.vue'),
    children: [
      { path: '', component: () => import('@/views/home/index.vue'), meta: { title: '首页' } },
      { path: 'article', component: () => import('@/views/home/article/index.vue') },
      {
        path: 'article/:id',
        name: 'ArticleDetail',
        component: () => import('@/views/home/article/detail/ArticleDetail.vue'),
        meta: { title: '文章详情' },
      },
      {
        path: 'category',
        component: () => import('@/views/home/category/index.vue'),
        meta: { title: '分类主页' },
      },
      {
        path: 'tag',
        component: () => import('@/views/home/tag/index.vue'),
        meta: { title: '标签主页' },
      },
      {
        path: 'user/:userId',
        name: 'UserProfile',
        component: () => import('@/views/home/profile/UserProfile.vue'),
        meta: { title: '用户主页' },
        beforeEnter: (to) => {
          const id = Number(to.params.userId)
          if (!Number.isFinite(id)) {
            return '/404'
          }
        },
      },
    ],
  },

  /**403*/
  { path: '/403', component: () => import('@/views/error/403.vue') },
  /**404*/
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
  },
]
const router = createRouter({ history: createWebHistory(), routes: staticRoutes })
export default router
