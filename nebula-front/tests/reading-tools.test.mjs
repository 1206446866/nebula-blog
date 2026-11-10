import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import ts from 'typescript'
import * as vue from 'vue'
import vm from './helpers/vue-modules.mjs'

const entry = new URL('../src/views/home/article/detail/logic/useReadingTools.ts', import.meta.url)
const code = ts.transpileModule(readFileSync(entry, 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS },
}).outputText
const settle = () => new Promise((resolve) => setImmediate(resolve))
function harness(initial = {}, deferred = false, withBanner = false) {
  const context = { token: 'one', calls: [], requests: [], scroll: 0, timers: [] },
    cleanups = []
  const state = { favorite: false, later: false, progress: 0, anchor: '', readAt: null, ...initial }
  const content = {
    getBoundingClientRect: () => ({
      top: 200 - context.scroll + (context.banner?.() ? 60 : 0),
      height: 2000,
    }),
    querySelectorAll: () => [],
    contains: () => false,
  }
  const win = {
    get scrollY() {
      return context.scroll
    },
    innerHeight: 800,
    addEventListener() {},
    removeEventListener() {},
    scrollTo: ({ top }) => {
      context.scroll = top
    },
  }
  const doc = {
    querySelector: (selector) => (selector === '#article-content .markdown-body' ? content : null),
    getElementById: () => null,
    visibilityState: 'visible',
    addEventListener() {},
    removeEventListener() {},
  }
  const dependencies = {
    vue: { ...vue, onBeforeUnmount: (callback) => cleanups.push(callback) },
    '@/stores/auth': { useAuthStore: () => ({ getToken: () => context.token }) },
    '@/api/reading': {
      getReadingApi: (id, signal) =>
        deferred
          ? new Promise((resolve) => context.requests.push({ id, signal, resolve }))
          : Promise.resolve({ data: { ...state } }),
      saveReadingProgressApi: async (id, progress, anchor) => {
        context.calls.push(['progress', id, progress])
        return { data: { ...state, progress, anchor } }
      },
      setReadingFlagsApi: async (id, flags) => {
        context.calls.push(['flags', id, flags])
        return { data: { ...state, ...flags } }
      },
    },
  }
  const module = { exports: {} }
  vm.runInNewContext(
    code,
    {
      module,
      exports: module.exports,
      AbortController,
      window: win,
      document: doc,
      setInterval: (callback) => {
        context.timers.push(callback)
        return context.timers.length
      },
      clearInterval() {},
      require: (name) => {
        assert.ok(name in dependencies, name)
        return dependencies[name]
      },
    },
    { entry },
  )
  const scope = vue.effectScope(),
    id = vue.ref(1),
    enabled = vue.ref(true)
  const view = scope.run(() => module.exports.useReadingTools(id, enabled))
  context.banner = () => withBanner && view.resumePending.value
  return {
    context,
    view,
    id,
    dispose() {
      cleanups.forEach((callback) => callback())
      scope.stop()
    },
  }
}
test('续读提示显示前不会用页首位置覆盖历史，明确从头阅读后才写入', async () => {
  const h = harness({ progress: 6500, anchor: 'chapter' })
  try {
    await settle()
    assert.equal(h.view.resumePending.value, true)
    h.context.timers[0]()
    await settle()
    assert.equal(h.context.calls.length, 0)
    h.view.resume(true)
    await settle()
    assert.equal(h.context.calls[0][2], 0)
  } finally {
    h.dispose()
  }
})
test('继续阅读按比例定位，收藏不改变续读位置', async () => {
  const h = harness({ progress: 6500 })
  try {
    await settle()
    await h.view.toggle('favorite')
    assert.equal(h.view.state.value.progress, 6500)
    h.view.resume(false)
    await settle()
    assert.ok(h.context.scroll > 500)
    assert.equal(h.context.calls.at(-1)[2], 6500)
  } finally {
    h.dispose()
  }
})
test('切换文章和换号后忽略过期阅读状态', async () => {
  const h = harness({}, true)
  try {
    h.id.value = 2
    await settle()
    assert.equal(h.context.requests[0].signal.aborted, true)
    h.context.requests[0].resolve({ data: { favorite: true, progress: 8000 } })
    await settle()
    assert.equal(h.view.ready.value, false)
    h.context.token = 'two'
    h.context.requests[1].resolve({ data: { favorite: true, progress: 8000 } })
    await settle()
    assert.equal(h.view.ready.value, false)
    await h.view.toggle('favorite')
    assert.equal(h.context.calls.length, 0)
  } finally {
    h.dispose()
  }
})

test('先收起恢复提示再定位，不把提示高度算入续读位置', async () => {
  const h = harness({ progress: 6500 }, false, true)
  try {
    await settle()
    await h.view.resume(false)
    assert.equal(h.context.scroll, 972)
    assert.equal(h.view.resumePending.value, false)
  } finally {
    h.dispose()
  }
})
