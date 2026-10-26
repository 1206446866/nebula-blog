import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { JSDOM } from 'jsdom'
import { compileScript, parse } from '@vue/compiler-sfc'
import ts from 'typescript'
import vm from './helpers/vue-modules.mjs'
import { clientFeatures } from '../src/constants/client-features.ts'
import { Role } from '../src/constants/role.ts'

const dom = new JSDOM('<!doctype html><html><body></body></html>')
for (const key of [
  'window',
  'document',
  'Document',
  'Element',
  'HTMLElement',
  'SVGElement',
  'Node',
  'history',
  'location',
])
  globalThis[key] = dom.window[key]
// 路由也会引入 Vue，统一在浏览器对象准备好后加载，避免提前缓存空文档。
const vue = await import('vue')
const { createRouter, createMemoryHistory } = await import('vue-router')
const empty = { render: () => null }
const link = vue.defineComponent({
  props: ['to'],
  setup:
    (props, { slots }) =>
    () =>
      vue.h('a', { href: props.to }, slots.default?.()),
})
const settle = async () => {
  await Promise.resolve()
  await Promise.resolve()
  await vue.nextTick()
}

/** 挂载真实模板和抽取脚本，只替换会话、请求及本次不测试的子组件。 */
function mount(relative, props = {}, api = {}) {
  const auth = vue.reactive({
    permissions: ['article:list', 'tag:list', 'category:list'],
    user: { username: '测试用户' },
    loggingOut: false,
    getToken: () => 'test-session',
    getAvatar: () => '',
    hasPermission(permission) {
      return this.permissions.includes(permission)
    },
  })
  const entry = new URL(`../src/components/${relative}.vue`, import.meta.url)
  const descriptor = parse(readFileSync(entry, 'utf8'), {
    filename: fileURLToPath(entry),
  }).descriptor
  const compiled = compileScript(descriptor, { id: relative, inlineTemplate: true })
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(compiled.content, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
    }).outputText,
    {
      module,
      exports: module.exports,
      window,
      document,
      AbortController,
      require(name) {
        if (name === 'vue') return vue
        if (name === 'vue-router')
          return {
            RouterLink: link,
            useRouter: () => ({ currentRoute: vue.ref({ path: '/', name: 'Home' }), push() {} }),
          }
        if (name === '@/constants/client-features') return { clientFeatures }
        if (name === '@/stores/auth' || name === '@/stores/auth.ts')
          return { useAuthStore: () => auth }
        if (name === '@/utils/auth') return { getCurrentUserId: () => 1 }
        if (name === '@/utils/resource') return { avatarUrl: () => '' }
        if (name === '@/api/community') return api
        if (
          [
            './HeaderSearch.vue',
            './FollowButton.vue',
            './CommunityUserList.vue',
            '@/views/home/profile/ProfilePagination.vue',
          ].includes(name)
        )
          return empty
        assert.fail(`未准备的依赖：${name}`)
      },
    },
    { entry },
  )
  const container = document.createElement('div')
  document.body.appendChild(container)
  const app = vue.createApp(module.exports.default, props)
  app.component('RouterLink', link)
  app.component('ElAvatar', empty)
  app.mount(container)
  return {
    auth,
    container,
    close() {
      app.unmount()
      container.remove()
    },
  }
}

test('收起社区后导航、本人资料和关注空列表不再引导进入社区，关注计数仍保留', async () => {
  assert.equal(clientFeatures.community, false)
  const header = mount('client/home/ClientHeader')
  const profile = mount(
    'community/ProfileConnections',
    { userId: 1 },
    {
      getFollowSummary: async () => ({ data: { self: true, followingCount: 0, followerCount: 0 } }),
    },
  )
  const list = mount(
    'community/CommunityUserList',
    { userId: 1, mode: 'following' },
    {
      getCommunityUsers: async () => ({
        data: { records: [], totalRow: 0, pageNumber: 1, pageSize: 12, totalPage: 0 },
      }),
    },
  )
  try {
    await settle()
    for (const runtime of [header, profile, list])
      assert.equal(runtime.container.querySelector('a[href="/community"]'), null)
    assert.match(profile.container.textContent, /关注/)
    assert.match(profile.container.textContent, /粉丝/)
    assert.match(list.container.textContent, /还没有关注的人/)
    assert.ok(header.container.querySelector('a[href="/article"]'))
  } finally {
    header.close()
    profile.close()
    list.close()
  }
})

test('旧社区地址按实际路由配置返回首页，不挂载被收起的页面', async () => {
  const entry = new URL('../src/router/index.ts', import.meta.url)
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(readFileSync(entry, 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS },
    }).outputText,
    {
      module,
      exports: module.exports,
      require(name) {
        if (name === 'vue-router')
          return { createRouter: (options) => options, createWebHistory: () => null }
        if (name === '@/constants/role.ts') return { Role }
        if (name === '@/constants/client-features') return { clientFeatures }
        assert.fail(name)
      },
    },
    { entry },
  )
  const stubViews = (routes) =>
    routes.map((route) => ({
      ...route,
      ...(route.component ? { component: empty } : {}),
      ...(route.children ? { children: stubViews(route.children) } : {}),
    }))
  const router = createRouter({
    history: createMemoryHistory(),
    routes: stubViews(module.exports.default.routes),
  })
  await router.push('/community?tab=following')
  assert.equal(router.currentRoute.value.path, '/')
  assert.ok(router.currentRoute.value.matched.every((route) => route.name !== 'Community'))
})

test('首页结尾只提供有权限的真实阅读入口，权限收回后同步收起链接', async () => {
  const runtime = mount('client/home/HomeClosing')
  try {
    assert.deepEqual(
      [...runtime.container.querySelectorAll('a')].map((a) => a.getAttribute('href')),
      ['/article', '/article', '/tag', '/category'],
    )
    runtime.auth.permissions = []
    await vue.nextTick()
    assert.equal(runtime.container.querySelectorAll('a').length, 0)
    assert.match(runtime.container.querySelector('button').textContent, /回到起点/)
  } finally {
    runtime.close()
  }
})

test('回到起点移动键盘焦点，并尊重减少动态效果的偏好', () => {
  const runtime = mount('client/home/HomeClosing')
  const anchor = document.createElement('section')
  anchor.id = 'home-start'
  anchor.tabIndex = -1
  document.body.prepend(anchor)
  const calls = []
  let reduced = false
  window.matchMedia = () => ({ matches: reduced })
  window.scrollTo = (options) => calls.push(options)
  try {
    runtime.container.querySelector('button').click()
    assert.equal(document.activeElement, anchor)
    assert.equal(calls[0].top, 0)
    assert.equal(calls[0].behavior, 'smooth')
    reduced = true
    runtime.container.querySelector('button').click()
    assert.equal(calls[1].behavior, 'auto')
  } finally {
    runtime.close()
    anchor.remove()
  }
})
