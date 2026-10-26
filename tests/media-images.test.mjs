import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { JSDOM } from 'jsdom'
import * as resource from '../src/utils/resource.ts'
const dom = new JSDOM('<!doctype html><body></body>', { url: 'http://localhost/' })
for (const key of ['window', 'document', 'Element', 'HTMLElement', 'SVGElement', 'Node'])
  globalThis[key] = dom.window[key]
const vue = await import('vue')
const path = 'upload/article/admin/20261023/0123456789abcdef0123456789abcdef.png'
const markup = `<img alt="星光" data-media-path="${path}">`
const settle = async () => {
  await vue.nextTick()
  await new Promise((resolve) => setImmediate(resolve))
  await vue.nextTick()
}

async function harness(loader = async () => new Blob(['image'], { type: 'image/png' })) {
  const calls = [],
    revoked = [],
    made = []
  const token = vue.ref('owner')
  const html = vue.ref(markup)
  const module = { exports: {} }
  const code = ts.transpileModule(
    readFileSync(new URL('../src/composables/useMediaImages.ts', import.meta.url), 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS } },
  ).outputText
  const deps = {
    vue,
    '@/api/media': {
      mediaImageApi: (path, signal) => {
        calls.push({ path, signal })
        return loader()
      },
    },
    '@/stores/auth': { useAuthStore: () => ({ getToken: () => token.value }) },
    '@/utils/resource': resource,
  }
  vm.runInNewContext(code, {
    module,
    exports: module.exports,
    AbortController,
    URL: {
      createObjectURL() {
        const url = `blob:media-${made.length}`
        made.push(url)
        return url
      },
      revokeObjectURL(url) {
        revoked.push(url)
      },
    },
    require: (name) => {
      assert.ok(name in deps, name)
      return deps[name]
    },
  })
  const host = document.createElement('div')
  document.body.append(host)
  const app = vue.createApp({
    setup() {
      const root = vue.ref(null)
      module.exports.useMediaImages(root, html)
      return () => vue.h('div', { ref: root, innerHTML: html.value })
    },
  })
  app.mount(host)
  await settle()
  return {
    calls,
    revoked,
    made,
    token,
    html,
    host,
    dispose() {
      app.unmount()
      host.remove()
    },
  }
}
test('typing reuses the loaded image and removing it releases the object URL', async () => {
  const h = await harness()
  try {
    assert.equal(h.calls.length, 1)
    assert.equal(h.host.querySelector('img').getAttribute('src'), 'blob:media-0')
    h.html.value = '<p>继续写作</p>' + markup
    await settle()
    assert.equal(h.calls.length, 1)
    assert.equal(h.host.querySelector('img').getAttribute('src'), 'blob:media-0')
    h.html.value = '<p>移除图片</p>'
    await settle()
    assert.deepEqual(h.revoked, ['blob:media-0'])
  } finally {
    h.dispose()
  }
})
test('pending images cannot land in another session and are aborted on unmount', async () => {
  const resolves = []
  const h = await harness(() => new Promise((resolve) => resolves.push(resolve)))
  try {
    h.token.value = 'other'
    await settle()
    assert.equal(h.calls[0].signal.aborted, true)
    resolves[0](new Blob(['old'], { type: 'image/png' }))
    await settle()
    assert.equal(h.made.length, 0)
    resolves[1](new Blob(['new'], { type: 'image/png' }))
    await settle()
    assert.equal(h.host.querySelector('img').getAttribute('src'), 'blob:media-0')
  } finally {
    h.dispose()
  }
  assert.equal(h.calls[1].signal.aborted, true)
  assert.deepEqual(h.revoked, ['blob:media-0'])
})
test('multiple references share one request and non-images never become object URLs', async () => {
  const h = await harness(async () => new Blob(['no'], { type: 'text/html' }))
  try {
    assert.equal(h.made.length, 0)
    assert.equal(h.host.querySelector('img').getAttribute('src'), null)
    assert.match(h.host.querySelector('img').title, /无法加载/)
    h.html.value = markup + markup
    await settle()
    assert.equal(h.calls.length, 2)
    assert.equal(h.made.length, 0)
  } finally {
    h.dispose()
  }
})
