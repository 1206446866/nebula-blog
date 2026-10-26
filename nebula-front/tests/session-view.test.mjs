import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from './helpers/vue-modules.mjs'
import ts from 'typescript'
import { JSDOM } from 'jsdom'
import { parse, compileScript } from '@vue/compiler-sfc'
import * as cache from '../src/utils/session-cache.ts'
import * as policy from '../src/router/policy.ts'
import { revokeCurrentSession } from '../src/utils/auth-session.ts'
import { Role } from '../src/constants/role.ts'
import { clientFeatures } from '../src/constants/client-features.ts'
import * as resource from '../src/utils/resource.ts'
import * as passwordPolicy from '../src/utils/password-policy.ts'

const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://localhost/' })
for (const key of ['window', 'document', 'Element', 'HTMLElement', 'SVGElement', 'Node'])
  globalThis[key] = dom.window[key]
globalThis.history = dom.window.history
const vue = await import('vue')
const piniaModule = await import('pinia')
const routerModule = await import('vue-router')

test('the actual public article route keeps its identity for case and trailing slash variants', () => {
  const { default: router } = evaluate(
    readFileSync(new URL('../src/router/index.ts', import.meta.url), 'utf8'),
    {
      'vue-router': { ...routerModule, createWebHistory: routerModule.createMemoryHistory },
      '@/constants/role.ts': { Role },
      '@/constants/client-features': { clientFeatures },
    },
  )
  assert.equal(router.resolve({ name: 'ArticleHome' }).path, '/article')
  for (const path of ['/article/', '/ARTICLE', '/ARTICLE/']) {
    assert.equal(router.resolve(path).name, 'ArticleHome')
    assert.equal(router.resolve(path).meta.articleQuery, true)
  }
})

function evaluate(source, dependencies, globals = {}, entry) {
  const module = { exports: {} }
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
  })
  vm.runInNewContext(
    outputText,
    {
      module,
      exports: module.exports,
      AbortController,
      ...globals,
      require(name) {
        if (name === '@/utils/resource') return resource
        assert.ok(name in dependencies, `Unexpected dependency: ${name}`)
        return dependencies[name]
      },
    },
    { entry },
  )
  return module.exports
}

const token = (id) =>
  `header.${Buffer.from(JSON.stringify({ sub: String(id), exp: Date.now() / 1000 + 3600 })).toString('base64url')}.signature`
const identity = (id, permissions = ['article:list', 'tag:list']) => ({
  user: { id, username: `user${id}` },
  roles: ['USER'],
  permissions,
})
const settle = async () => {
  await Promise.resolve()
  await vue.nextTick()
}

async function harness(path = '/user/1') {
  const storage = new Map()
  const state = { server: identity(1), wait: null, mounts: 0, unmounts: 0 }
  const { useAuthStore } = evaluate(
    readFileSync(new URL('../src/stores/auth.ts', import.meta.url), 'utf8'),
    {
      pinia: piniaModule,
      '@/api/auth.ts': {
        currentSessionApi: async () => ({ data: state.wait ? await state.wait : state.server }),
        logoutApi: async () => {},
      },
      '@/utils/auth-session': { revokeCurrentSession },
      '@/constants/role': { Role },
      '@/utils/session-cache': cache,
    },
    {
      localStorage: {
        getItem: (key) => storage.get(key),
        setItem: (key, value) => storage.set(key, value),
        removeItem: (key) => storage.delete(key),
      },
    },
  )
  const pinia = piniaModule.createPinia()
  piniaModule.setActivePinia(pinia)
  const auth = useAuthStore(pinia)
  auth.setAuth({ token: token(1), ...identity(1) })
  const snapshot = vue.defineComponent({
    setup() {
      state.mounts++
      vue.onBeforeUnmount(() => state.unmounts++)
      const text = `private-user-${auth.user.id}-${auth.hasPermission('tag:list') ? 'tag-snapshot' : 'basic'}`
      return () => vue.h('p', text)
    },
  })
  const publicPage = vue.defineComponent({
    setup() {
      state.mounts++
      return () => vue.h('p', 'public-login')
    },
  })
  const router = routerModule.createRouter({
    history: routerModule.createMemoryHistory(),
    routes: [
      { path: '/user/:id', component: snapshot, meta: { idParam: 'id' } },
      { path: '/login', component: publicPage, meta: { public: true } },
      { path: '/403', component: publicPage, meta: { public: true } },
    ],
  })
  router.beforeEach(policy.createNavigationGuard(auth))
  const descriptor = parse(
    readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8'),
  ).descriptor
  const compiled = compileScript(descriptor, { id: 'session-boundary-test', inlineTemplate: true })
  const component = evaluate(compiled.content, {
    vue,
    'vue-router': routerModule,
    '@/stores/auth': { useAuthStore },
    '@/router/policy': policy,
  }).default
  await router.push(path)
  const container = document.createElement('div')
  document.body.append(container)
  const app = vue.createApp(component)
  app.use(pinia).use(router).mount(container)
  await settle()
  return {
    state,
    auth,
    router,
    container,
    cleanup: () => {
      app.unmount()
      container.remove()
    },
  }
}

test('switching accounts removes the old mounted private view before the new session is verified', async () => {
  const h = await harness()
  try {
    assert.match(h.container.textContent, /private-user-1/)
    let confirm
    h.state.wait = new Promise((resolve) => {
      confirm = resolve
    })
    h.auth.setAuth({ token: token(2), ...identity(2) })
    const navigation = h.router.replace({ path: '/user/1', force: true })
    await settle()
    assert.doesNotMatch(h.container.textContent, /private-user-1/)
    assert.match(h.container.textContent, /正在确认登录状态/)
    assert.equal(h.state.unmounts, 1)
    confirm(identity(2))
    await navigation
    await settle()
    assert.match(h.container.textContent, /private-user-2/)
    assert.equal(h.state.mounts, 2)
  } finally {
    h.cleanup()
  }
})

test('changed authorization replaces cached page data even when the account and URL stay the same', async () => {
  const h = await harness()
  try {
    assert.match(h.container.textContent, /tag-snapshot/)
    h.state.server = identity(1, ['article:list'])
    await h.router.replace({ path: '/user/1', force: true })
    await settle()
    assert.doesNotMatch(h.container.textContent, /tag-snapshot/)
    assert.match(h.container.textContent, /basic/)
    assert.equal(h.state.mounts, 2)
  } finally {
    h.cleanup()
  }
})

test('unchanged authorization preserves the mounted view during normal query navigation', async () => {
  const h = await harness()
  try {
    await h.router.push('/user/1?tab=comments')
    await settle()
    assert.equal(h.state.mounts, 1)
    assert.equal(h.state.unmounts, 0)
  } finally {
    h.cleanup()
  }
})

test('public login views stay mounted while a new login is being installed', async () => {
  const h = await harness('/login')
  try {
    h.auth.setAuth({ token: token(2), ...identity(2) })
    await settle()
    assert.equal(h.container.textContent, 'public-login')
    assert.equal(h.state.mounts, 1)
    assert.equal(h.state.unmounts, 0)
  } finally {
    h.cleanup()
  }
})

function setupComponent(relative, dependencies, props = {}) {
  const cleanups = []
  const descriptor = parse(readFileSync(new URL(relative, import.meta.url), 'utf8')).descriptor
  const compiled = compileScript(descriptor, { id: 'flow-test' })
  const component = evaluate(
    compiled.content,
    {
      vue: { ...vue, onBeforeUnmount: (callback) => cleanups.push(callback), onMounted: () => {} },
      ...dependencies,
    },
    {},
    new URL(relative, import.meta.url),
  ).default
  return {
    view: component.setup(props, { expose() {}, emit() {} }),
    dispose: () => cleanups.forEach((callback) => callback()),
  }
}

for (const action of ['password', 'cancellation']) {
  for (const switched of [false, true]) {
    test(`${action} completion clears only its own session (account switched: ${switched})`, async () => {
      const state = { token: 'old', clears: 0, routes: [], messages: [] }
      const auth = {
        getToken: () => state.token,
        clearAuth: () => {
          state.token = ''
          state.clears++
        },
      }
      let confirm
      const request = () =>
        new Promise((resolve) => {
          confirm = resolve
        })
      const { view, dispose } = setupComponent(
        '../src/views/home/profile/ProfileInfo.vue',
        {
          '@/stores/auth.ts': { useAuthStore: () => auth },
          'element-plus': {
            ElMessage: {
              success: (message) => state.messages.push(message),
              error: assert.fail,
              warning: assert.fail,
            },
          },
          '@/api/auth.ts': { changePasswordApi: request },
          '@/api/user.ts': { cancelSelfAccountApi: request },
          '@/router': { replace: async (path) => state.routes.push(path) },
          '@/utils/date.ts': { formatDateTime: () => '' },
          '@/utils/password-policy': passwordPolicy,
        },
        { isSelf: true, profile: null },
      )
      try {
        view.pwdForm.value = {
          oldPassword: 'fixture-old',
          newPassword: 'fixture-new',
          confirmPassword: 'fixture-new',
        }
        view.cancellationForm.value = { password: 'fixture-old', confirmed: true }
        const pending =
          action === 'password' ? view.submitChangePassword() : view.submitCancellation()
        if (switched) state.token = 'new'
        confirm({ code: 200, data: true })
        await pending
        assert.equal(state.clears, switched ? 0 : 1)
        assert.equal(state.token, switched ? 'new' : '')
        assert.deepEqual(state.routes, switched ? [] : ['/login'])
        assert.equal(state.messages.length, switched ? 0 : 1)
      } finally {
        dispose()
      }
    })
  }
}

test('a completed avatar upload cannot assign the former account avatar to the new account', async () => {
  let currentToken = 'old',
    confirm,
    updates = 0
  const { useUploadAvatar } = evaluate(
    readFileSync(new URL('../src/composables/useUploadAvatar.ts', import.meta.url), 'utf8'),
    {
      vue,
      'element-plus': { ElMessage: { error: assert.fail, success: assert.fail } },
      '@/api/user': {
        uploadAvatarApi: () =>
          new Promise((resolve) => {
            confirm = resolve
          }),
      },
      '../stores/auth': {
        useAuthStore: () => ({ getToken: () => currentToken, updateAvatar: () => updates++ }),
      },
    },
  )
  const { uploadAvatar } = useUploadAvatar()
  const pending = uploadAvatar({ type: 'image/png', size: 10 })
  currentToken = 'new'
  confirm({ code: 200, data: 'old-avatar.png' })
  assert.equal(await pending, undefined)
  assert.equal(updates, 0)
})

test('leaving the login form cancels its pending login before it can change the session or route', async () => {
  let signal,
    installs = 0,
    routes = 0
  const { view, dispose } = setupComponent('../src/views/login/components/LoginForm.vue', {
    'vue-router': {
      useRoute: () => ({ query: {} }),
      useRouter: () => ({ replace: () => routes++ }),
    },
    '@/router/policy': policy,
    '@/stores/auth.ts': { useAuthStore: () => ({ setAuth: () => installs++ }) },
    '@/api/auth.ts': {
      loginApi: (_form, currentSignal) =>
        new Promise((_resolve, reject) => {
          signal = currentSignal
          signal.addEventListener('abort', () => reject(new Error('cancelled')))
        }),
    },
    'element-plus': { ElMessage: { warning: assert.fail, error: assert.fail } },
    '@/constants/role.ts': { Role },
  })
  view.form.account = 'fixture'
  view.form.password = 'fixture-password'
  const pending = view.handleLogin()
  dispose()
  await pending
  assert.equal(signal.aborted, true)
  assert.equal(installs, 0)
  assert.equal(routes, 0)
})

function articleView() {
  const route = vue.reactive({ params: { id: '1' } })
  const requests = []
  const { view, dispose } = setupComponent('../src/views/home/article/detail/ArticleDetail.vue', {
    'vue-router': { useRoute: () => route },
    '@/api/article': {
      getArticleByIdApi: (id, signal) =>
        new Promise((resolve, reject) => requests.push({ id, signal, resolve, reject })),
    },
    '@/api/category': { listCategoryApi: assert.fail },
    '@/api/tag': { listTagApi: assert.fail },
    './components/ArticleHero.vue': {},
    './components/ArticleContent.vue': {},
    './components/ArticleSidebar.vue': {},
    './components/ArticleNavigation.vue': {},
    './components/ArticleComment.vue': {},
    '@/api/user.ts': {
      getUserById: async (id) => ({ code: 200, data: { id, username: 'author' } }),
    },
    '@/stores/auth': { useAuthStore: () => ({ hasPermission: () => false }) },
  })
  return { route, requests, view, dispose }
}

test('article parameter changes cancel the previous load and ignore its late response', async () => {
  const h = articleView()
  try {
    h.route.params.id = '2'
    await settle()
    assert.equal(h.requests[0].signal.aborted, true)
    h.requests[1].resolve({ code: 200, data: { id: 2, userId: 2 } })
    await settle()
    h.requests[0].resolve({ code: 200, data: { id: 1, userId: 1 } })
    await settle()
    assert.equal(h.view.article.value.id, 2)
    h.route.params = {}
    await settle()
    assert.equal(h.requests.length, 2)
  } finally {
    h.dispose()
  }
})

test('article loading failures are handled and can be retried', async () => {
  const h = articleView()
  try {
    h.requests[0].reject(new Error('offline'))
    await settle()
    assert.equal(h.view.loadError.value, true)
    const retry = h.view.fetchArticle()
    h.requests[1].resolve({ code: 200, data: { id: 1, userId: 1 } })
    await retry
    assert.equal(h.view.loadError.value, false)
    assert.equal(h.view.article.value.id, 1)
  } finally {
    h.dispose()
  }
})

test('unmounting the article details cancels the pending request', () => {
  const h = articleView()
  h.dispose()
  assert.equal(h.requests[0].signal.aborted, true)
})
