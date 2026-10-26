import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import ts from 'typescript'
import * as vue from 'vue'
import * as three from 'three'
import vm from './helpers/vue-modules.mjs'

/** 使用真实场景和几何体，仅替换显卡渲染，检查动画与共享资源的生命周期。 */
function sphereRuntime(available = true) {
  const scope = vue.effectScope()
  const mounted = [],
    unmounted = [],
    frames = new Map(),
    hostEvents = new Map(),
    documentEvents = new Map(),
    motionEvents = new Map()
  let nextId = 0,
    scene,
    renderer,
    resizeCallback,
    intersectionCallback
  let resizeDisconnected = false,
    intersectionDisconnected = false,
    canvasRemoved = false
  const host = {
    getBoundingClientRect: () => ({ width: 800, height: 620 }),
    appendChild() {},
    addEventListener: (name, fn) => hostEvents.set(name, fn),
    removeEventListener: (name) => hostEvents.delete(name),
    hasPointerCapture: () => false,
  }
  const motion = {
    matches: false,
    addEventListener: (name, fn) => motionEvents.set(name, fn),
    removeEventListener: (name) => motionEvents.delete(name),
  }
  const document = {
    hidden: false,
    createElement: () => ({ width: 0, height: 0, getContext: () => ({ fillText() {} }) }),
    addEventListener: (name, fn) => documentEvents.set(name, fn),
    removeEventListener: (name) => documentEvents.delete(name),
  }
  class Renderer {
    constructor() {
      if (!available) throw new Error('图形能力不可用')
      renderer = this
      this.disposed = false
      this.contextReleased = false
      this.domElement = {
        setAttribute() {},
        addEventListener() {},
        removeEventListener() {},
        remove() {
          canvasRemoved = true
        },
      }
    }
    setPixelRatio(value) {
      this.pixelRatio = value
    }
    setClearColor() {}
    setSize() {}
    render(value) {
      scene = value
    }
    dispose() {
      this.disposed = true
    }
    forceContextLoss() {
      this.contextReleased = true
    }
  }
  const entry = new URL('../src/components/client/home/logic/useTagSphere.ts', import.meta.url)
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(readFileSync(entry, 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS },
    }).outputText,
    {
      module,
      exports: module.exports,
      document,
      window: { devicePixelRatio: 3, matchMedia: () => motion },
      ResizeObserver: class {
        constructor(callback) {
          resizeCallback = callback
        }
        observe() {}
        disconnect() {
          resizeDisconnected = true
        }
      },
      IntersectionObserver: class {
        constructor(callback) {
          intersectionCallback = callback
        }
        observe() {}
        disconnect() {
          intersectionDisconnected = true
        }
      },
      requestAnimationFrame: (fn) => {
        frames.set(++nextId, fn)
        return nextId
      },
      cancelAnimationFrame: (id) => frames.delete(id),
      require(name) {
        if (name === 'three') return { ...three, WebGLRenderer: Renderer }
        assert.equal(name, 'vue')
        return {
          ...vue,
          onMounted: (fn) => mounted.push(fn),
          onBeforeUnmount: (fn) => unmounted.push(fn),
        }
      },
    },
    { entry },
  )
  const state = scope.run(() => module.exports.useTagSphere())
  state.container.value = host
  mounted.forEach((fn) => fn())
  return {
    state,
    frames,
    hostEvents,
    documentEvents,
    motionEvents,
    motion,
    document,
    get scene() {
      return scene
    },
    get renderer() {
      return renderer
    },
    resize: () => resizeCallback(),
    visibility: (visible) => intersectionCallback([{ isIntersecting: visible }]),
    get cleaned() {
      return resizeDisconnected && intersectionDisconnected && canvasRemoved
    },
    close() {
      unmounted.forEach((fn) => fn())
      scope.stop()
    },
  }
}

test('首页标签球在暂停、离屏、后台及减少动态效果时停止，恢复后只保留一个循环', async () => {
  const runtime = sphereRuntime()
  try {
    assert.equal(runtime.state.ready.value, true)
    assert.equal(runtime.renderer.pixelRatio, 1.5)
    assert.equal(runtime.frames.size, 1)
    runtime.state.paused.value = true
    await vue.nextTick()
    assert.equal(runtime.frames.size, 0)
    runtime.state.paused.value = false
    await vue.nextTick()
    runtime.visibility(false)
    assert.equal(runtime.frames.size, 0)
    runtime.visibility(true)
    assert.equal(runtime.frames.size, 1)
    runtime.document.hidden = true
    runtime.documentEvents.get('visibilitychange')()
    assert.equal(runtime.frames.size, 0)
    runtime.document.hidden = false
    runtime.documentEvents.get('visibilitychange')()
    runtime.motion.matches = true
    runtime.motionEvents.get('change')()
    assert.equal(runtime.frames.size, 0)
    runtime.motion.matches = false
    runtime.motionEvents.get('change')()
    runtime.resize()
    runtime.resize()
    assert.equal(runtime.frames.size, 1)
    const [id, frame] = [...runtime.frames][0]
    runtime.frames.delete(id)
    frame(100)
    assert.equal(runtime.frames.size, 1)
  } finally {
    runtime.close()
  }
  assert.equal(runtime.frames.size, 0)
  assert.equal(runtime.hostEvents.size + runtime.documentEvents.size + runtime.motionEvents.size, 0)
  assert.equal(runtime.cleaned, true)
})

test('首页标签球卸载时释放全部几何体、共享材质、文字纹理及图形上下文', () => {
  const runtime = sphereRuntime()
  const resources = new Set(),
    disposed = new Map()
  runtime.scene.traverse((object) => {
    if (object.geometry) resources.add(object.geometry)
    for (const material of Array.isArray(object.material)
      ? object.material
      : object.material
        ? [object.material]
        : []) {
      resources.add(material)
      if (material.map) resources.add(material.map)
    }
  })
  for (const resource of resources)
    resource.addEventListener('dispose', () =>
      disposed.set(resource, (disposed.get(resource) ?? 0) + 1),
    )
  runtime.close()
  assert.ok(resources.size > 80)
  assert.equal(disposed.size, resources.size)
  assert.ok([...disposed.values()].every((count) => count === 1))
  assert.equal(runtime.renderer.disposed, true)
  assert.equal(runtime.renderer.contextReleased, true)
  assert.equal(runtime.state.ready.value, false)
})

test('图形能力不可用时回退静态展示，不启动动画或注册持续监听', () => {
  const runtime = sphereRuntime(false)
  assert.equal(runtime.state.ready.value, false)
  assert.equal(runtime.frames.size, 0)
  assert.equal(runtime.hostEvents.size + runtime.documentEvents.size + runtime.motionEvents.size, 0)
  runtime.close()
})
