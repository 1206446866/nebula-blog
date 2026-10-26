import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createNavigationGuard, safeRedirect, hasRouteAccess } from '../src/router/policy.ts'
import {
  tokenPayload,
  readAuthCache,
  normalizeRoles,
  persistAuth,
  singleFlight,
  authorizationKey,
} from '../src/utils/session-cache.ts'

for (const path of [
  '/LOGIN',
  '/login/',
  '/LOGIN///?redirect=/article',
  '/Register/',
  '/SESSION-ERROR/',
  '/403/',
  '/%6cogin',
]) {
  test(`authentication and error return URL variants cannot loop (${path})`, () => {
    assert.equal(safeRedirect(path), null)
  })
}
test('authorization identity ignores ordering and duplicate grants', () => {
  assert.equal(
    authorizationKey(['ADMIN', 'USER'], ['tag:list', 'article:list']),
    authorizationKey(['USER', 'ADMIN', 'ADMIN'], ['article:list', 'tag:list', 'tag:list']),
  )
  assert.notEqual(authorizationKey(['ADMIN'], ['article:list']), authorizationKey(['ADMIN'], []))
})
test('rendering an already mounted page requires the same parent and child authorization', () => {
  assert.equal(
    hasRouteAccess(
      { matched: [{ meta: { roles: ['ADMIN'] } }, { meta: { permission: 'user:list' } }] },
      { hasAnyRole: () => true, hasPermission: () => false },
    ),
    false,
  )
})
import { positiveId, readArticleQuery, articleFilterQuery } from '../src/utils/article-query.ts'

const jwt = (payload = {}) =>
  `${Buffer.from('{"alg":"HS256"}').toString('base64url')}.${Buffer.from(JSON.stringify({ sub: '1', exp: Date.now() / 1000 + 3600, ...payload })).toString('base64url')}.signature`
const target = (extra = {}) => ({
  fullPath: '/article/1?view=detail#comments',
  meta: {},
  params: {},
  query: {},
  matched: [],
  ...extra,
})
function fixture(options = {}) {
  const state = {
    token: jwt(),
    roles: ['USER'],
    permissions: [],
    cleared: 0,
    checks: 0,
    ...options,
  }
  const auth = {
    getToken: () => state.token,
    clearAuth: () => {
      state.token = ''
      state.cleared++
    },
    refreshSession: async () => {
      state.checks++
      if (state.refresh) return state.refresh()
      state.roles = state.serverRoles || ['USER']
      state.permissions = state.serverPermissions || []
      return true
    },
    hasAnyRole: (roles) =>
      roles.some(
        (role) =>
          state.roles.includes(role) ||
          (state.roles.includes('SUPER_ADMIN') && ['ADMIN', 'USER'].includes(role)) ||
          (role === 'USER' && state.roles.includes('ADMIN')),
      ),
    hasPermission: (permission) =>
      state.roles.includes('SUPER_ADMIN') || state.permissions.includes(permission),
  }
  return { state, guard: createNavigationGuard(auth) }
}

for (const token of [
  '',
  'broken',
  jwt({ exp: 0 }),
  jwt({ sub: '0' }),
  jwt({ sub: '1.5' }),
  jwt({ exp: 'later' }),
  jwt({ sub: '9007199254740993' }),
]) {
  test(`invalid or expired token cannot mount protected pages (${token.slice(0, 15)})`, async () => {
    const { state, guard } = fixture({ token })
    const result = await guard(target())
    assert.equal(result.path, '/login')
    assert.equal(result.query.redirect, '/article/1?view=detail#comments')
    assert.equal(state.checks, 0)
  })
}
test('public and error pages do not require a server session check', async () => {
  const { state, guard } = fixture({ token: '' })
  assert.equal(await guard(target({ meta: { public: true } })), true)
  assert.equal(state.checks, 0)
})
test('local role forgery is replaced with current server authorization before checking routes', async () => {
  const { state, guard } = fixture({ roles: ['SUPER_ADMIN'], serverRoles: ['USER'] })
  assert.equal((await guard(target({ matched: [{ meta: { roles: ['ADMIN'] } }] }))).path, '/403')
  assert.deepEqual(state.roles, ['USER'])
  assert.equal(state.checks, 1)
})
test('an admin must satisfy both the parent and the more restrictive child role', async () => {
  const { guard } = fixture({ serverRoles: ['ADMIN'] })
  assert.equal(
    (
      await guard(
        target({ matched: [{ meta: { roles: ['ADMIN'] } }, { meta: { roles: ['SUPER_ADMIN'] } }] }),
      )
    ).path,
    '/403',
  )
})
test('super admin inherits admin access while keeping access to the restricted child', async () => {
  const { guard } = fixture({ serverRoles: ['SUPER_ADMIN'] })
  assert.equal(
    await guard(
      target({
        matched: [
          { meta: { roles: ['ADMIN'] } },
          { meta: { roles: ['SUPER_ADMIN'], permission: 'user:list' } },
        ],
      }),
    ),
    true,
  )
})
test('page permission checks use current server permissions', async () => {
  const { guard } = fixture({ serverRoles: ['ADMIN'], permissions: ['user:list'] })
  assert.equal(
    (await guard(target({ matched: [{ meta: { permission: 'user:list' } }] }))).path,
    '/403',
  )
})
test('newly granted roles and permissions take effect without a new token', async () => {
  const { guard } = fixture({ serverRoles: ['ADMIN'], serverPermissions: ['article:list'] })
  assert.equal(
    await guard(target({ matched: [{ meta: { roles: ['ADMIN'], permission: 'article:list' } }] })),
    true,
  )
})
test('a rejected current session is cleared and keeps the intended destination', async () => {
  const { state, guard } = fixture({
    refresh: async () => {
      throw { response: { status: 401 } }
    },
  })
  const result = await guard(target())
  assert.equal(result.path, '/login')
  assert.equal(state.token, '')
  assert.equal(state.cleared, 1)
})
for (const failure of [new Error('offline'), { response: { status: 503 } }]) {
  test(`temporary verification failure preserves the session (${failure.message || 503})`, async () => {
    const { state, guard } = fixture({
      refresh: async () => {
        throw failure
      },
    })
    const token = state.token
    assert.equal((await guard(target())).path, '/session-error')
    assert.equal(state.token, token)
    assert.equal(state.cleared, 0)
  })
}
test('logout during verification cannot permit the pending protected navigation', async () => {
  let finish
  const { state, guard } = fixture({
    refresh: () =>
      new Promise((resolve) => {
        finish = resolve
      }),
  })
  const pending = guard(target())
  state.token = ''
  finish(true)
  assert.equal((await pending).path, '/login')
})
test('a late 401 for an older token cannot clear a newer login', async () => {
  let reject
  const { state, guard } = fixture({
    refresh: () =>
      new Promise((_, fail) => {
        reject = fail
      }),
  })
  const pending = guard(target())
  const next = jwt({ sub: '2' })
  state.token = next
  state.refresh = async () => true
  reject({ response: { status: 401 } })
  assert.equal(await pending, true)
  assert.equal(state.token, next)
  assert.equal(state.cleared, 0)
})

for (const value of [
  '0',
  '-1',
  '1.5',
  '1e2',
  '0x10',
  'NaN',
  'Infinity',
  '9007199254740992',
  ['1'],
]) {
  test(`invalid route identity is rejected before fetching (${value})`, async () => {
    const { state, guard } = fixture()
    const result = await guard(target({ meta: { idParam: 'id' }, params: { id: value } }))
    assert.equal(result.path, '/404')
    assert.equal(state.checks, 0)
  })
}
test('positive decimal route identities are accepted', () => {
  assert.equal(positiveId('123'), true)
  assert.equal(positiveId('01'), false)
})
for (const value of [
  'https://evil.test',
  '//evil.test',
  '/%2F%2Fevil.test',
  '/\\evil.test',
  '/\nunsafe',
  '/login',
  '/register?token=x',
  '/session-error',
  '/%zz',
]) {
  test(`unsafe or looping login return path is rejected (${JSON.stringify(value)})`, () =>
    assert.equal(safeRedirect(value), null))
}
test('a return path retains application filters and anchors', () =>
  assert.equal(
    safeRedirect('/article?categoryId=2&page=3#comments'),
    '/article?categoryId=2&page=3#comments',
  ))
test('malformed browser cache cannot crash initialization or become role arrays', () => {
  const data = { token: jwt(), user: '{broken', roles: '"SUPER_ADMIN"', permissions: '{}' }
  const cache = readAuthCache({ getItem: (key) => data[key] ?? null })
  assert.equal(cache.user, null)
  assert.deepEqual(cache.roles, [])
  assert.deepEqual(cache.permissions, [])
  assert.deepEqual(normalizeRoles(['ROLE_ADMIN', 'ADMIN', 3]), ['ADMIN'])
})
test('blocked browser storage is recoverable', () => {
  assert.equal(
    readAuthCache({
      getItem: () => {
        throw Error('denied')
      },
    }).token,
    '',
  )
  assert.doesNotThrow(() =>
    persistAuth(
      {
        setItem: () => {
          throw Error('denied')
        },
        removeItem: () => {},
      },
      { token: jwt(), user: null, roles: [], permissions: [] },
    ),
  )
})
test('clearing authentication removes only the four authentication keys', () => {
  const removed = []
  persistAuth(
    { setItem: () => {}, removeItem: (key) => removed.push(key) },
    { token: '', user: null, roles: [], permissions: [] },
  )
  assert.deepEqual(removed, ['token', 'user', 'roles', 'permissions'])
})
test('valid session claims are readable without treating decoding as signature verification', () => {
  assert.equal(tokenPayload(jwt()).sub, '1')
  assert.equal(tokenPayload('invalid'), null)
})
test('concurrent checks share a request and allow retry after a failure', async () => {
  let calls = 0
  const load = singleFlight(async () => {
    calls++
    if (calls === 1) throw Error('offline')
    return 'ok'
  })
  const results = await Promise.allSettled([load('same'), load('same')])
  assert.equal(calls, 1)
  assert.ok(results.every((r) => r.status === 'rejected'))
  assert.equal(await load('same'), 'ok')
  assert.equal(calls, 2)
})
test('query filters survive reload and support scalar, repeated and comma separated identities', () => {
  assert.deepEqual(
    readArticleQuery({ categoryId: '1', categoryIds: ['2,3', '1'], tagId: ['4', '5'], page: '2' }),
    { categoryIds: [1, 2, 3], tagIds: [4, 5], page: 2 },
  )
  assert.deepEqual(articleFilterQuery([], [4]), {
    categoryId: undefined,
    tagId: undefined,
    categoryIds: undefined,
    tagIds: '4',
    page: undefined,
  })
})
for (const query of [
  { categoryId: '0' },
  { tagId: '-1' },
  { categoryIds: ',' },
  { page: '0' },
  { page: ['1', '2'] },
  { categoryIds: Array.from({ length: 101 }, (_, i) => String(i + 1)) },
]) {
  test(`invalid article URL query is rejected (${JSON.stringify(query).slice(0, 45)})`, () =>
    assert.throws(() => readArticleQuery(query)))
}

function memoryRouter(guard) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: {} },
      { path: '/login', component: {}, meta: { public: true } },
      { path: '/403', component: {}, meta: { public: true } },
      { path: '/404', component: {}, meta: { public: true } },
      { path: '/session-error', component: {}, meta: { public: true } },
      { path: '/user/:id', component: {}, meta: { idParam: 'id' } },
      { path: '/article', component: {}, meta: { articleQuery: true } },
      {
        path: '/admin',
        component: {},
        meta: { roles: ['ADMIN'] },
        children: [{ path: 'role', component: {}, meta: { roles: ['SUPER_ADMIN'] } }],
      },
    ],
  })
  router.beforeEach(guard)
  return router
}
test('real router preserves a deep link when redirecting an anonymous visitor to login', async () => {
  const { guard } = fixture({ token: '' })
  const router = memoryRouter(guard)
  await router.push('/user/12?tab=comment')
  assert.equal(router.currentRoute.value.path, '/login')
  assert.equal(router.currentRoute.value.query.redirect, '/user/12?tab=comment')
})
test('real router checks identities again when reusing the same user page', async () => {
  const { guard } = fixture()
  const router = memoryRouter(guard)
  await router.push('/user/1')
  await router.push('/user/-1')
  assert.equal(router.currentRoute.value.path, '/404')
})
test('real router applies parent and child roles without a redirect loop', async () => {
  const { guard } = fixture({ serverRoles: ['ADMIN'] })
  const router = memoryRouter(guard)
  await router.push('/admin/role')
  assert.equal(router.currentRoute.value.path, '/403')
})
test('real router keeps filter and page state on back navigation', async () => {
  const { guard } = fixture()
  const router = memoryRouter(guard)
  await router.push('/article?categoryId=2&page=1')
  await router.push('/article?categoryId=2&page=2')
  await new Promise((resolve) => {
    const stop = router.afterEach(() => {
      stop()
      resolve()
    })
    router.back()
  })
  assert.deepEqual(readArticleQuery(router.currentRoute.value.query), {
    categoryIds: [2],
    tagIds: [],
    page: 1,
  })
})
