import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { createRequire } from 'node:module'
import { test } from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'
import { JSDOM } from 'jsdom'
import { parse, compileScript } from '@vue/compiler-sfc'

const dom = new JSDOM('<!doctype html><html><body></body></html>')
for (const key of ['window', 'document', 'Element', 'HTMLElement', 'SVGElement', 'Node'])
  globalThis[key] = dom.window[key]
const vue = await import('vue')
const settle = async () => {
  await new Promise((resolve) => setImmediate(resolve))
  await vue.nextTick()
}
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}
function evaluate(source, dependencies) {
  const module = { exports: {} }
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS },
  }).outputText
  vm.runInNewContext(code, {
    module,
    exports: module.exports,
    console,
    require(name) {
      assert.ok(name in dependencies, name)
      return dependencies[name]
    },
  })
  return module.exports
}
function initialize(engine, loadFull) {
  return evaluate(readFileSync(new URL('../src/utils/particles.ts', import.meta.url), 'utf8'), {
    '@tsparticles/engine': { tsParticles: engine },
    tsparticles: { loadFull },
  })
}
function background(initializedParticles) {
  const descriptor = parse(
    readFileSync(new URL('../src/components/StarBackground.vue', import.meta.url), 'utf8'),
  ).descriptor
  return evaluate(
    compileScript(descriptor, { id: 'background-regression', inlineTemplate: true }).content,
    {
      vue,
      '@/utils/particles': { initializedParticles },
    },
  ).default
}
function mount(component) {
  const container = document.createElement('div')
  document.body.append(container)
  const app = vue.createApp(component)
  app.mount(container)
  return {
    container,
    close() {
      app.unmount()
      container.remove()
    },
  }
}

test('粒子功能并发注册只执行一次，失败后可以重新初始化', async () => {
  const pending = deferred()
  let calls = 0
  const engine = {}
  const helper = initialize(engine, () => {
    calls++
    return pending.promise
  })
  const first = helper.initializedParticles()
  assert.equal(first, helper.initializedParticles())
  pending.resolve()
  assert.equal(await first, engine)
  assert.equal(await helper.initializedParticles(), engine)
  assert.equal(calls, 1)
  let retries = 0
  const retry = initialize(engine, async () => {
    if (++retries === 1) throw new Error('fixture')
  })
  await assert.rejects(retry.initializedParticles())
  assert.equal(await retry.initializedParticles(), engine)
  assert.equal(retries, 2)
})

test('同一页面多个背景使用独立ID，切换页面会销毁每个实例', async () => {
  const created = []
  let registrations = 0
  const engine = {
    async load(options) {
      const item = {
        id: options.id,
        destroyed: 0,
        destroy() {
          this.destroyed++
        },
      }
      created.push(item)
      return item
    },
  }
  const helper = initialize(engine, async () => {
    registrations++
  })
  const component = background(helper.initializedParticles)
  const root = vue.defineComponent({
    setup: () => () => vue.h('div', [vue.h(component), vue.h(component)]),
  })
  const view = mount(root)
  await settle()
  assert.equal(registrations, 1)
  assert.equal(created.length, 2)
  assert.notEqual(created[0].id, created[1].id)
  view.close()
  assert.deepEqual(
    created.map((item) => item.destroyed),
    [1, 1],
  )
  const next = mount(component)
  await settle()
  assert.equal(registrations, 1)
  next.close()
  assert.equal(created[2].destroyed, 1)
})

test('初始化尚未完成就离开页面，不再创建背景', async () => {
  const pending = deferred()
  let loads = 0
  const view = mount(background(() => pending.promise))
  view.close()
  pending.resolve({
    load() {
      loads++
    },
  })
  await settle()
  assert.equal(loads, 0)
})

test('背景加载完成前离开页面，迟到的实例立即释放', async () => {
  const pending = deferred()
  const view = mount(background(async () => ({ load: () => pending.promise })))
  await settle()
  view.close()
  let destroyed = 0
  pending.resolve({
    destroy() {
      destroyed++
    },
  })
  await settle()
  assert.equal(destroyed, 1)
})

test('页面使用的全部图标均有本地SVG，入口不再运行外部脚本', () => {
  const require = createRequire(import.meta.url)
  let icons,
    registrations = 0
  evaluate(readFileSync(new URL('../src/icons.ts', import.meta.url), 'utf8'), {
    ionicons: {
      addIcons(value) {
        icons = value
      },
    },
    'ionicons/components/ion-icon': {
      defineCustomElement() {
        registrations++
      },
    },
    'ionicons/icons': require('ionicons/icons'),
  })
  const used = new Set()
  function scan(directory) {
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      const file = new URL(entry.name + (entry.isDirectory() ? '/' : ''), directory)
      if (entry.isDirectory()) scan(file)
      else if (entry.name.endsWith('.vue'))
        for (const match of readFileSync(file, 'utf8').matchAll(/<ion-icon\s+name="([\w-]+)"/g))
          used.add(match[1])
    }
  }
  scan(new URL('../src/', import.meta.url))
  assert.ok(used.size >= 14)
  for (const name of used) assert.match(icons[name] || '', /<svg\b/, name)
  assert.equal(registrations, 1)
  assert.doesNotMatch(
    readFileSync(new URL('../index.html', import.meta.url), 'utf8'),
    /<script[^>]+src="https?:/,
  )
})
