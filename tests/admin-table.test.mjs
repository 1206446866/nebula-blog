import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import ts from 'typescript'
import * as vue from 'vue'
import vm from './helpers/vue-modules.mjs'
import * as passwordPolicy from '../src/utils/password-policy.ts'
import { Role } from '../src/constants/role.ts'

function evaluate(relative, dependencies, globals = {}) {
  const entry = new URL(relative, import.meta.url)
  const source = readFileSync(entry, 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS },
  }).outputText
  const module = { exports: {} }
  vm.runInNewContext(
    code,
    {
      module,
      exports: module.exports,
      AbortController,
      ...globals,
      require(name) {
        assert.ok(name in dependencies, `Unexpected dependency: ${name}`)
        return dependencies[name]
      },
    },
    { entry },
  )
  return module.exports
}

const settle = () => new Promise((resolve) => setImmediate(resolve))
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}
const page = (current = 1, size = 10, total = 50, records = [{ id: current, status: 0 }]) => ({
  code: 200,
  data: {
    records,
    pageNumber: current,
    pageSize: size,
    totalRow: total,
    totalPage: Math.ceil(total / size),
  },
})

function runtime() {
  const mounts = [],
    timers = new Map()
  let id = 0
  const globals = {
    setTimeout(callback) {
      timers.set(++id, callback)
      return id
    },
    clearTimeout(key) {
      timers.delete(key)
    },
  }
  const mockedVue = { ...vue, onMounted: (callback) => mounts.push(callback) }
  const { useTable } = evaluate('../src/composables/useTable.ts', { vue: mockedVue }, globals)
  return {
    useTable,
    mockedVue,
    globals,
    mounts,
    timers,
    flushTimers() {
      const callbacks = [...timers.values()]
      timers.clear()
      callbacks.forEach((callback) => callback())
    },
  }
}
function harness(query, options = {}) {
  const env = runtime()
  const scope = vue.effectScope()
  const table = scope.run(() => env.useTable(query, { immediate: false, ...options }))
  return { ...env, table, close: () => scope.stop() }
}

test('late results cannot replace the newest query and superseded requests are aborted', async () => {
  const calls = []
  const h = harness((request) => {
    const wait = deferred()
    calls.push({ request, ...wait })
    return wait.promise
  })
  try {
    const old = h.table.reload()
    const latest = h.table.reload()
    assert.equal(calls[0].request.signal.aborted, true)
    calls[1].resolve(page(1, 10, 50, [{ id: 'latest' }]))
    await latest
    calls[0].resolve(page(1, 10, 50, [{ id: 'old' }]))
    await old
    assert.equal(h.table.list.value[0].id, 'latest')
    assert.equal(h.table.loading.value, false)
  } finally {
    h.close()
  }
})

test('an older failure cannot clear the current loading state or expose a stale error', async () => {
  const calls = []
  const h = harness(() => {
    const wait = deferred()
    calls.push(wait)
    return wait.promise
  })
  try {
    const first = h.table.reload(),
      latest = h.table.reload()
    calls[0].reject(new Error('old failure'))
    await first
    assert.equal(h.table.loading.value, true)
    assert.equal(h.table.error.value, null)
    calls[1].resolve(page())
    await latest
    assert.equal(h.table.loading.value, false)
  } finally {
    h.close()
  }
})

test('filter typing immediately invalidates old work and explicit search cancels a queued duplicate', async () => {
  const keyword = vue.ref('old'),
    calls = []
  const h = harness(
    (request) => {
      const wait = deferred()
      calls.push({ keyword: keyword.value, request, ...wait })
      return wait.promise
    },
    { watch: keyword },
  )
  try {
    const old = h.table.reload()
    keyword.value = 'n'
    keyword.value = 'new'
    assert.equal(calls[0].request.signal.aborted, true)
    assert.equal(h.timers.size, 1)
    calls[0].resolve(page(1, 10, 50, [{ id: 'old' }]))
    await old
    assert.equal(h.table.list.value.length, 0)
    const latest = h.table.search()
    assert.equal(h.timers.size, 0)
    assert.equal(calls.length, 2)
    assert.equal(calls[1].keyword, 'new')
    calls[1].resolve(page())
    await latest
    h.flushTimers()
    assert.equal(calls.length, 2)
  } finally {
    h.close()
  }
})

test('changing page size returns to the first page and coalesces the pager current-change event', async () => {
  const calls = []
  const h = harness((request) => {
    calls.push(request)
    return Promise.resolve(page(request.current, request.size, 100))
  })
  try {
    await h.table.changePage(4)
    const changing = h.table.changeSize(20)
    await h.table.changePage(1)
    await changing
    assert.deepEqual(
      calls.map((call) => [call.current, call.size]),
      [
        [4, 10],
        [1, 20],
      ],
    )
    assert.equal(h.table.current.value, 1)
    assert.equal(h.table.size.value, 20)
    await h.table.changePage(-1)
    await h.table.changeSize(NaN)
    assert.equal(calls.length, 2)
  } finally {
    h.close()
  }
})

test('a refresh after deletion relocates an out-of-range page and handles an empty dataset', async () => {
  const calls = []
  let total = 21
  const h = harness((request) => {
    calls.push(request.current)
    return Promise.resolve(page(request.current, request.size, total, []))
  })
  try {
    await h.table.changePage(8)
    assert.deepEqual(calls, [8, 3])
    assert.equal(h.table.current.value, 3)
    total = 0
    await h.table.reload()
    assert.equal(h.table.current.value, 1)
    assert.equal(h.table.total.value, 0)
  } finally {
    h.close()
  }
})

test('query failures are recoverable without leaving stale rows or an unhandled rejection', async () => {
  let failed = false
  const h = harness(() =>
    failed ? Promise.reject(new Error('network failed')) : Promise.resolve(page()),
  )
  try {
    await h.table.reload()
    failed = true
    assert.equal(await h.table.reload(), false)
    assert.match(h.table.error.value, /加载失败/)
    assert.equal(h.table.list.value.length, 0)
    assert.equal(h.table.loading.value, false)
    failed = false
    assert.equal(await h.table.reload(), true)
    assert.equal(h.table.error.value, null)
    assert.equal(h.table.list.value.length, 1)
    const status = vue.computed(() => h.table.list.value[0].status)
    assert.equal(status.value, 0)
    h.table.list.value[0].status = 1
    assert.equal(status.value, 1)
  } finally {
    h.close()
  }
})

test('unmounting cancels timers and requests, and late results cannot revive the table', async () => {
  const keyword = vue.ref(''),
    wait = deferred()
  let requests = 0,
    signal
  const h = harness(
    (request) => {
      requests++
      signal = request.signal
      return wait.promise
    },
    { watch: keyword },
  )
  const loading = h.table.reload()
  keyword.value = 'queued'
  h.close()
  assert.equal(signal.aborted, true)
  assert.equal(h.timers.size, 0)
  wait.resolve(page())
  await loading
  h.flushTimers()
  await h.table.reload()
  await h.table.search()
  await h.table.changePage(2)
  await h.table.changeSize(20)
  assert.equal(requests, 1)
  assert.equal(h.table.list.value.length, 0)
})

const managers = [
  {
    name: 'Article',
    path: 'article',
    method: 'getArticlePageApi',
    filter: ['titleFilter', '标题'],
    args: (view, current, size) => [{ title: view.titleFilter.value, current, size }],
  },
  {
    name: 'User',
    path: 'user',
    method: 'getUsersApi',
    filter: ['userNameFilter', '用户'],
    args: (view, current, size) => [
      view.roleFilter.value,
      view.userNameFilter.value,
      current,
      size,
    ],
  },
  {
    name: 'Category',
    path: 'category',
    method: 'pageCategoryApi',
    filter: ['keyword', '分类'],
    args: (view, current, size) => [{ name: view.keyword.value, currentPage: current, size }],
    search: 'handleSearch',
    page: 'handlePageChange',
    size: 'handleSizeChange',
  },
  {
    name: 'Tag',
    path: 'tag',
    method: 'pageTagApi',
    filter: ['nameFilter', '标签'],
    args: (view, current, size) => [view.nameFilter.value, current, size],
  },
  {
    name: 'Role',
    path: 'role',
    method: 'getRolePageApi',
    filter: ['descriptionFilter', '角色'],
    args: (view, current, size) => [{ description: view.descriptionFilter.value, current, size }],
  },
  {
    name: 'Permission',
    path: 'permission',
    method: 'getPermissionPage',
    filter: ['nameFilter', '权限'],
    args: (view, current, size) => [{ name: view.nameFilter.value, current, size }],
  },
  {
    name: 'Comment',
    path: 'comment',
    method: 'getCommentPageApi',
    filter: ['contentFilter', '评论'],
    args: (view, current, size) => [
      view.articleIdFilter.value,
      view.contentFilter.value,
      current,
      size,
    ],
  },
]

for (const manager of managers)
  test(`${manager.name} management uses the shared filter, page-size and cancellation contract`, async () => {
    const env = runtime(),
      calls = [],
      scope = vue.effectScope()
    let view
    const listApi = (...args) => {
      calls.push(args)
      const [current, size] =
        manager.path === 'article' ||
        manager.path === 'category' ||
        manager.path === 'role' ||
        manager.path === 'permission'
          ? [args[0].current ?? args[0].currentPage, args[0].size]
          : args.slice(-3, -1)
      return Promise.resolve(page(current, size))
    }
    const dependencies = {
      vue: env.mockedVue,
      '@/composables/useTable': { useTable: env.useTable },
      '@/composables/useTable.ts': { useTable: env.useTable },
      '@/utils/date': { formatDateTime: () => '' },
      '@/stores/auth': {
        useAuthStore: () => ({ hasRole: () => false, hasPermission: () => false }),
      },
      '@/constants/role': { Role: { ADMIN: 'ADMIN', SUPER_ADMIN: 'SUPER_ADMIN' } },
      '@/utils/auth': { getCurrentUserId: () => 1 },
      '@/utils/password-policy': {},
      '@/api/auth': {},
      'vue-router': { useRouter: () => ({ push() {} }) },
      'element-plus': { ElMessage: {}, ElMessageBox: {} },
      '@/api/role.ts': { getAllRolesApi: async () => ({ data: [] }) },
      [`@/api/${manager.path}`]: { [manager.method]: listApi },
      [`@/api/${manager.path}.ts`]: { [manager.method]: listApi },
    }
    const fn = `use${manager.name}Manager`
    const exported = evaluate(`../src/views/admin/logic/${fn}.ts`, dependencies, env.globals)
    view = scope.run(() => exported[fn]())
    try {
      env.mounts.forEach((callback) => callback())
      await settle()
      assert.equal(calls.length, 1)
      view[manager.filter[0]].value = manager.filter[1]
      env.flushTimers()
      await settle()
      assert.deepEqual(structuredClone(calls.at(-1).slice(0, -1)), manager.args(view, 1, 10))
      assert.ok(calls.at(-1).at(-1) instanceof AbortSignal)
      await view[manager.page || 'onPageChange'](2)
      assert.deepEqual(structuredClone(calls.at(-1).slice(0, -1)), manager.args(view, 2, 10))
      await view[manager.size || 'onSizeChange'](20)
      assert.deepEqual(structuredClone(calls.at(-1).slice(0, -1)), manager.args(view, 1, 20))
      const count = calls.length
      view[manager.filter[0]].value = '离开前的条件'
      scope.stop()
      env.flushTimers()
      await settle()
      assert.equal(calls.length, count)
    } finally {
      scope.stop()
    }
  })

test('the extracted user manager preserves administrator boundaries and the six-character new-password rule', async () => {
  const env = runtime(),
    scope = vue.effectScope(),
    calls = []
  const exported = evaluate(
    '../src/views/admin/logic/useUserManager.ts',
    {
      vue: env.mockedVue,
      '@/composables/useTable': { useTable: env.useTable },
      '@/utils/date': { formatDateTime: () => '' },
      '@/stores/auth': {
        useAuthStore: () => ({ hasRole: (role) => role === Role.ADMIN, hasPermission: () => true }),
      },
      '@/constants/role': { Role },
      '@/utils/auth': { getCurrentUserId: () => 1 },
      '@/utils/password-policy': passwordPolicy,
      '@/api/user': { editUserApi: () => assert.fail('ordinary admin cannot assign roles') },
      '@/api/role.ts': {},
      '@/api/auth': {
        resetUserPasswordApi: async (id, data) => {
          calls.push({ id, data })
          return { data: true }
        },
      },
      'element-plus': { ElMessage: { success() {}, warning: assert.fail } },
    },
    env.globals,
  )
  const view = scope.run(() => exported.useUserManager())
  try {
    assert.equal(view.canResetPassword({ id: 1, roleIds: [] }), false)
    assert.equal(view.canResetPassword({ id: 2, roleIds: [1] }), false)
    assert.equal(view.canResetPassword({ id: 2, roleIds: [2] }), false)
    assert.equal(view.canResetPassword({ id: 2, roleIds: [] }), true)
    await view.saveEdit()
    view.openPasswordReset({ id: 2, roleIds: [] })
    Object.assign(view.passwordResetForm, {
      operatorPassword: '123',
      newPassword: 'abc123',
      confirmPassword: 'abc123',
    })
    await view.submitPasswordReset()
    assert.equal(calls.length, 1)
    assert.equal(calls[0].id, 2)
    assert.equal(calls[0].data.operatorPassword, '123')
    assert.equal(calls[0].data.newPassword, 'abc123')
    assert.equal(view.passwordResetVisible.value, false)
    assert.equal(view.passwordResetForm.operatorPassword, '')
  } finally {
    scope.stop()
  }
})

test('the extracted permissions dialog preserves selections across modules and skips super-administrator submission', async () => {
  const calls = []
  const exported = evaluate('../src/components/dialog/logic/useRolePermissionDialog.ts', {
    vue,
    '@/constants/role': { Role },
    'element-plus': { ElMessage: { success() {} } },
    '@/api/permission.ts': {
      getPermissionsApi: async () => ({ data: { user: [{ id: 1 }], article: [{ id: 2 }] } }),
      getRolePermissionsApi: async () => ({ data: { article: [{ id: 2 }] } }),
      saveRolePermissionsApi: async (data) => calls.push(structuredClone(data)),
    },
  })
  const view = exported.useRolePermissionDialog()
  await view.open({ id: 3, name: 'AUTHOR', description: '作者' })
  assert.equal(view.visible.value, true)
  assert.equal(view.checkedMap.value.article[0], 2)
  view.checkedMap.value.user = []
  await view.submit()
  assert.deepEqual(calls, [{ roleId: 3, permissionIds: [2] }])
  view.checkedMap.value.article = []
  await view.submit()
  assert.deepEqual(calls[1], { roleId: 3, permissionIds: [] })
  await view.open({ id: 1, name: Role.SUPER_ADMIN, description: '超级管理员' })
  assert.equal(view.isSuperAdminRole.value, true)
  await view.submit()
  assert.equal(calls.length, 2)
})

test('权限分配将未知前缀与other合并，保留已有选择，允许取消并按原ID保存', async () => {
  const calls = []
  const all = {
    user: [{ id: 1, name: 'user:list' }],
    other: [{ id: 8, name: 'other:export' }],
    Test: [{ id: 10, name: 'Test:test' }],
    a: [{ id: 11, name: 'a:test' }],
    future: [{ id: 12, name: 'future:read' }],
  }
  const original = structuredClone(all)
  const exported = evaluate('../src/components/dialog/logic/useRolePermissionDialog.ts', {
    vue,
    '@/constants/role': { Role },
    'element-plus': { ElMessage: { success() {} } },
    '@/api/permission.ts': {
      getPermissionsApi: async () => ({ data: all }),
      getRolePermissionsApi: async () => ({
        data: { Test: [all.Test[0]], a: [all.a[0]], other: [all.other[0]] },
      }),
      saveRolePermissionsApi: async (data) => calls.push(structuredClone(data)),
    },
  })
  const view = exported.useRolePermissionDialog()
  await view.open({ id: 4, name: 'T111', description: '测试角色' })
  view.currentModule.value = 'other'
  assert.deepEqual(
    Array.from(view.currentPermissions.value, (p) => p.name),
    ['other:export', 'Test:test', 'a:test', 'future:read'],
  )
  assert.deepEqual(Array.from(view.checkedMap.value.other), [10, 11, 8])
  await view.submit()
  assert.deepEqual(calls[0], { roleId: 4, permissionIds: [10, 11, 8] })
  view.checkedMap.value.other = [8, 10, 12]
  await view.submit()
  assert.deepEqual(calls[1], { roleId: 4, permissionIds: [8, 10, 12] })
  assert.deepEqual(all, original)
  await view.open({ id: 1, name: Role.SUPER_ADMIN, description: '超级管理员' })
  assert.deepEqual(Array.from(view.checkedMap.value.other), [8, 10, 11, 12])
  await view.submit()
  assert.equal(calls.length, 2)
})

test('只有未知前缀时默认进入其它，重复权限只展示一次', async () => {
  const legacy = { id: 10, name: 'Test:test' }
  const exported = evaluate('../src/components/dialog/logic/useRolePermissionDialog.ts', {
    vue,
    '@/constants/role': { Role },
    'element-plus': { ElMessage: { success() {} } },
    '@/api/permission.ts': {
      getPermissionsApi: async () => ({
        data: {
          Test: [legacy],
          other: [legacy],
          constructor: [{ id: 11, name: 'constructor:test' }],
        },
      }),
      getRolePermissionsApi: async () => ({ data: { Test: [legacy] } }),
      saveRolePermissionsApi: async () => {},
    },
  })
  const view = exported.useRolePermissionDialog()
  await view.open({ id: 4, name: 'T111', description: '测试角色' })
  assert.equal(view.currentModule.value, 'other')
  assert.equal(view.currentModuleName.value, '其它')
  assert.deepEqual(
    Array.from(view.currentPermissions.value, (p) => p.id),
    [10, 11],
  )
  assert.deepEqual(Array.from(view.checkedMap.value.other), [10])
})

test('all seven management list APIs forward the cancellation signal to the HTTP client', async () => {
  const controller = new AbortController(),
    calls = []
  const request = {
    get: async (url, config) => {
      calls.push({ url, config })
      return page()
    },
  }
  const cases = [
    ['article', 'getArticlePageApi', [{ title: '文章', current: 2, size: 20 }], '/articles'],
    ['user', 'getUsersApi', ['ADMIN', '用户', 2, 20], '/users'],
    ['category', 'pageCategoryApi', [{ name: '分类', currentPage: 2, size: 20 }], '/categories'],
    ['tag', 'pageTagApi', ['标签', 2, 20], '/tags'],
    ['role', 'getRolePageApi', [{ description: '角色', current: 2, size: 20 }], '/roles/page'],
    [
      'permission',
      'getPermissionPage',
      [{ name: '权限', current: 2, size: 20 }],
      '/permissions/page',
    ],
    ['comment', 'getCommentPageApi', [9, '评论', 2, 20], '/comments'],
  ]
  for (const [file, method, args, url] of cases) {
    const api = evaluate(`../src/api/${file}.ts`, {
      '@/utils/request': request,
      '@/utils/request.ts': request,
    })
    await api[method](...args, controller.signal)
    assert.equal(calls.at(-1).url, url)
    assert.equal(calls.at(-1).config.signal, controller.signal)
  }
})
