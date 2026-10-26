import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from './helpers/vue-modules.mjs'
import ts from 'typescript'
import { compileScript, parse } from '@vue/compiler-sfc'
import * as vue from 'vue'
import * as searchUtils from '../src/utils/site-search.ts'
import { publicationTime } from '../src/utils/publication-time.ts'

test('publication times are interpreted in Shanghai even when the browser uses another timezone', () => {
  const now = Date.parse('2026-10-23T00:00:00Z')
  assert.equal(publicationTime('2026-10-23T10:30', now), '2026-10-23T10:30:00+08:00')
  for (const value of [
    '2026-10-23T07:59',
    '2026-02-30T10:30',
    '2026-13-01T10:30',
    '',
    'tomorrow',
    '2026-10-23T10:30Z',
  ])
    assert.equal(publicationTime(value, now), null, value)
})

test('search route validation rejects malformed keywords, types and pages', () => {
  assert.deepEqual(searchUtils.readSiteSearchQuery({ q: '  星云  ', type: 'article', page: '2' }), {
    q: '星云',
    type: 'article',
    page: 2,
  })
  for (const query of [
    { q: ['a', 'b'] },
    { q: 'a'.repeat(51) },
    { type: 'private' },
    { type: ['all'] },
    { page: '0' },
    { page: '1.1' },
    { page: '2147483648' },
  ])
    assert.throws(() => searchUtils.readSiteSearchQuery(query))
})
test('each search type navigates to a fixed internal destination', () => {
  for (const [type, path] of Object.entries({
    article: '/article/7',
    comment: '/article/7#article-comments',
    user: '/user/7',
    category: '/article?categoryIds=7',
    tag: '/article?tagIds=7',
  }))
    assert.equal(searchUtils.searchTarget({ type, targetId: 7 }), path)
  assert.equal(searchUtils.searchTarget({ type: 'https://example.com', targetId: 7 }), '/404')
  assert.equal(searchUtils.searchTarget({ type: 'article', targetId: -1 }), '/404')
})

const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}
const flush = async () => {
  await vue.nextTick()
  await Promise.resolve()
  await Promise.resolve()
}
function harness(path, query, api) {
  const route = vue.reactive({ name: 'SiteSearch', query })
  const auth = vue.reactive({
    token: 'one',
    getToken() {
      return this.token
    },
    hasPermission() {
      return true
    },
  })
  const routes = [],
    cleanups = []
  const source = readFileSync(new URL(path, import.meta.url), 'utf8')
  const code = ts.transpileModule(
    compileScript(parse(source).descriptor, { id: 'site-search' }).content,
    { compilerOptions: { module: ts.ModuleKind.CommonJS } },
  ).outputText
  const deps = {
    vue: {
      ...vue,
      onBeforeUnmount(fn) {
        cleanups.push(fn)
      },
    },
    'vue-router': {
      useRoute: () => route,
      useRouter: () => ({
        push: (target) => {
          routes.push(target)
          return Promise.resolve()
        },
      }),
    },
    '@/api/search': { searchSite: api },
    '@/utils/site-search': searchUtils,
    '@/utils/date': { formatDateTime: (value) => value },
    '@/stores/auth': { useAuthStore: () => auth },
    '@/views/home/profile/ProfilePagination.vue': {},
  }
  const module = { exports: {} }
  vm.runInNewContext(
    code,
    {
      module,
      exports: module.exports,
      AbortController,
      require: (name) => {
        assert.ok(name in deps, name)
        return deps[name]
      },
    },
    { entry: new URL(path, import.meta.url) },
  )
  const scope = vue.effectScope()
  const view = scope.run(() => module.exports.default.setup({}, { expose() {} }))
  return {
    view,
    route,
    routes,
    auth,
    close() {
      cleanups.forEach((fn) => fn())
      scope.stop()
    },
  }
}
const page = (id) => ({
  page: { records: [{ id }], pageNumber: 1, totalRow: 1 },
  counts: { article: 1, comment: 0, user: 0, category: 0, tag: 0 },
})

test('search discards older responses when query, page or session changes', async () => {
  const calls = []
  const h = harness('../src/views/home/search/index.vue', { q: 'first' }, (...args) => {
    const wait = deferred()
    calls.push({ args, ...wait })
    return wait.promise
  })
  try {
    h.route.query = { q: 'second', type: 'article', page: '2' }
    await flush()
    assert.equal(calls[0].args[3].aborted, true)
    calls[1].resolve({ data: page(2) })
    await flush()
    calls[0].resolve({ data: page(1) })
    await flush()
    assert.equal(h.view.result.value.page.records[0].id, 2)
    h.auth.token = 'two'
    await flush()
    assert.equal(h.view.result.value, null)
    h.close()
    assert.equal(calls[2].args[3].aborted, true)
  } finally {
    h.close()
  }
})
test('empty or invalid search URLs make no API request and search navigation preserves the query', async () => {
  const calls = []
  const h = harness('../src/views/home/search/index.vue', {}, (...args) => {
    calls.push(args)
    return Promise.resolve({ data: page(1) })
  })
  try {
    assert.equal(calls.length, 0)
    h.route.query = { q: 'x', page: '0' }
    await flush()
    assert.equal(calls.length, 0)
    assert.match(h.view.error.value, /搜索地址无效/)
    h.route.query = { q: '星云', type: 'article' }
    await flush()
    h.view.navigate('tag', 3)
    assert.equal(h.routes[0].query.q, '星云')
    assert.equal(h.routes[0].query.page, 3)
  } finally {
    h.close()
  }
})
test('the center navigation search ignores empty input and routes trimmed keywords internally', () => {
  const h = harness('../src/components/client/home/HeaderSearch.vue', { q: '旧词' }, () =>
    assert.fail(),
  )
  try {
    assert.equal(h.view.keyword.value, '旧词')
    h.view.keyword.value = '   '
    h.view.submit()
    assert.equal(h.routes.length, 0)
    h.view.keyword.value = '  星云  '
    h.view.submit()
    assert.equal(h.routes[0].name, 'SiteSearch')
    assert.equal(h.routes[0].query.q, '星云')
  } finally {
    h.close()
  }
})
