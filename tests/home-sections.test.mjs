import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { JSDOM } from 'jsdom'
import { compileScript, parse } from '@vue/compiler-sfc'
import ts from 'typescript'
import vm from './helpers/vue-modules.mjs'

const dom = new JSDOM('<!doctype html><html><body></body></html>', { pretendToBeVisual: true })
for (const key of [
  'window',
  'document',
  'Document',
  'Element',
  'HTMLElement',
  'SVGElement',
  'Node',
])
  globalThis[key] = dom.window[key]
const vue = await import('vue')

/** 挂载真实首页板块，只有星云装饰画替换为动效状态探针，不提供任何接口依赖。 */
function mountSection(name) {
  const motionListeners = new Set(),
    observers = [],
    cache = new Map()
  const media = {
    matches: false,
    addEventListener: (_, fn) => motionListeners.add(fn),
    removeEventListener: (_, fn) => motionListeners.delete(fn),
  }
  window.matchMedia = () => media
  class Observer {
    constructor(callback) {
      this.callback = callback
      this.disconnected = false
      observers.push(this)
    }
    observe() {}
    disconnect() {
      this.disconnected = true
    }
  }
  window.IntersectionObserver = Observer
  Object.defineProperty(document, 'hidden', { value: false, configurable: true })
  const load = (entry) => {
    if (cache.has(entry.href)) return cache.get(entry.href)
    let source = readFileSync(entry, 'utf8')
    if (entry.pathname.endsWith('.vue')) {
      const descriptor = parse(source, { filename: fileURLToPath(entry) }).descriptor
      source = compileScript(descriptor, { id: entry.href, inlineTemplate: true }).content
    }
    const module = { exports: {} }
    vm.runInNewContext(
      ts.transpileModule(source, {
        compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
      }).outputText,
      {
        module,
        exports: module.exports,
        window,
        document,
        IntersectionObserver: Observer,
        require(dependency) {
          if (dependency === 'vue') return vue
          if (dependency === 'vue-router')
            return {
              RouterLink: vue.defineComponent({
                props: ['to'],
                setup:
                  (props, { slots }) =>
                  () =>
                    vue.h('a', { href: props.to }, slots.default?.()),
              }),
            }
          if (dependency === './NebulaCloud.vue')
            return vue.defineComponent({
              props: ['animated'],
              setup: (props) => () => vue.h('div', { 'data-animated': String(props.animated) }),
            })
          assert.ok(dependency.startsWith('./'), `首页介绍不应加载接口或外部依赖：${dependency}`)
          return load(new URL(`${dependency}.ts`, entry))
        },
      },
      { entry },
    )
    cache.set(entry.href, module.exports)
    return module.exports
  }
  const entry = new URL(`../src/components/client/home/${name}.vue`, import.meta.url)
  const container = document.createElement('div')
  document.body.appendChild(container)
  const app = vue.createApp(load(entry).default)
  app.mount(container)
  return {
    container,
    media,
    motionListeners,
    observers,
    close() {
      app.unmount()
      container.remove()
    },
  }
}

test('分类航图的星点和领域选择同步介绍，不将示例分类冒充真实分类编号', async () => {
  const runtime = mountSection('CategorySection')
  try {
    const { container } = runtime
    container.querySelector('[aria-label="点亮智能与探索星座"]').click()
    await vue.nextTick()
    assert.match(container.querySelector('.atlas-observation').textContent, /智能与探索.*未知/)
    assert.equal(container.querySelectorAll('.atlas-node[aria-pressed="true"]').length, 1)
    assert.match(container.querySelector('.atlas-route.is-active').textContent, /智能与探索/)
    const routes = container.querySelectorAll('.atlas-route')
    routes[3].click()
    await vue.nextTick()
    assert.equal(
      container.querySelector('[aria-label="点亮工程与实践星座"]').getAttribute('aria-pressed'),
      'true',
    )
    assert.match(container.querySelector('.atlas-observation').textContent, /工程与实践.*稳定运行/)
    assert.equal(container.querySelector('a').getAttribute('href'), '/category')
    assert.equal(container.querySelectorAll('a').length, 1)
  } finally {
    runtime.close()
  }
})

test('星云文章板块保留主题切换和文章入口，抽取脚本后仍能暂停动效', async () => {
  const runtime = mountSection('ArticleSection')
  try {
    const { container } = runtime
    container.querySelector('[aria-label="观测前端星域"]').click()
    await vue.nextTick()
    assert.match(container.querySelector('.topic-route.is-active').textContent, /前端与体验/)
    assert.match(container.querySelector('.observation-note').textContent, /每一个想法/)
    container.querySelector('[aria-label="暂停星云动画"]').click()
    await vue.nextTick()
    assert.equal(container.querySelector('[data-animated]').getAttribute('data-animated'), 'false')
    assert.equal(container.querySelector('a').getAttribute('href'), '/article')
  } finally {
    runtime.close()
  }
})

test('首页星图在离屏、后台及减少动态效果时暂停，离页移除观察和监听', async () => {
  const runtime = mountSection('CategorySection')
  try {
    const still = () =>
      runtime.container.querySelector('#category-atlas').classList.contains('is-still')
    assert.equal(still(), false)
    runtime.observers[0].callback([{ isIntersecting: false }])
    await vue.nextTick()
    assert.equal(still(), true)
    runtime.observers[0].callback([{ isIntersecting: true }])
    Object.defineProperty(document, 'hidden', { value: true, configurable: true })
    document.dispatchEvent(new window.Event('visibilitychange'))
    await vue.nextTick()
    assert.equal(still(), true)
    Object.defineProperty(document, 'hidden', { value: false, configurable: true })
    document.dispatchEvent(new window.Event('visibilitychange'))
    runtime.media.matches = true
    runtime.motionListeners.forEach((fn) => fn())
    await vue.nextTick()
    assert.equal(still(), true)
    assert.equal(runtime.container.querySelector('.atlas-motion').disabled, true)
    runtime.media.matches = false
    runtime.motionListeners.forEach((fn) => fn())
    await vue.nextTick()
    assert.equal(still(), false)
  } finally {
    runtime.close()
  }
  assert.equal(runtime.motionListeners.size, 0)
  assert.ok(runtime.observers.every((observer) => observer.disconnected))
})
