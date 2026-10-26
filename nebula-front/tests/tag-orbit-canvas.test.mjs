import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import ts from 'typescript'
import * as vue from 'vue'
import vm from './helpers/vue-modules.mjs'

/** 不依赖图形驱动，核对画布的暂停、后台切换和离页资源释放。 */
function canvasRuntime(available = true) {
  const scope = vue.effectScope(),
    mounted = [],
    unmounted = [],
    frames = new Map(),
    documentListeners = new Map(),
    motionListeners = new Map()
  let sequence = 0,
    disconnected = false
  const ctx = new Proxy(
    {
      createRadialGradient: () => ({ addColorStop() {} }),
      createLinearGradient: () => ({ addColorStop() {} }),
    },
    {
      get(target, key) {
        return key in target ? target[key] : () => {}
      },
      set(target, key, value) {
        target[key] = value
        return true
      },
    },
  )
  const element = {
    width: 0,
    height: 0,
    getContext: () => (available ? ctx : null),
    getBoundingClientRect: () => ({ width: 1600, height: 1000 }),
  }
  const motion = {
    matches: false,
    addEventListener: (name, fn) => motionListeners.set(name, fn),
    removeEventListener: (name) => motionListeners.delete(name),
  }
  const document = {
    hidden: false,
    createElement: () => ({ width: 0, height: 0, getContext: () => ctx }),
    addEventListener: (name, fn) => documentListeners.set(name, fn),
    removeEventListener: (name) => documentListeners.delete(name),
  }
  const entry = new URL('../src/views/home/tag/logic/useOrbitCanvas.ts', import.meta.url)
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(readFileSync(entry, 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS },
    }).outputText,
    {
      module,
      exports: module.exports,
      document,
      window: { devicePixelRatio: 2, matchMedia: () => motion },
      ResizeObserver: class {
        observe() {}
        disconnect() {
          disconnected = true
        }
      },
      requestAnimationFrame: (fn) => {
        frames.set(++sequence, fn)
        return sequence
      },
      cancelAnimationFrame: (id) => frames.delete(id),
      require(name) {
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
  const state = vue.reactive({ paused: false, pointerX: 0, pointerY: 0 })
  scope.run(() => module.exports.useOrbitCanvas(vue.ref(element), state))
  for (const fn of mounted) fn()
  return {
    state,
    frames,
    document,
    motion,
    documentListeners,
    motionListeners,
    element,
    get disconnected() {
      return disconnected
    },
    close() {
      for (const fn of unmounted) fn()
      scope.stop()
    },
  }
}

test('星环画布在暂停、后台及减少动态效果时停止循环，离页释放全部监听', async () => {
  const runtime = canvasRuntime()
  try {
    assert.equal(runtime.frames.size, 1)
    assert.equal(runtime.element.width, 2400)
    assert.equal(runtime.element.height, 1500)
    runtime.state.paused = true
    await vue.nextTick()
    assert.equal(runtime.frames.size, 0)
    runtime.state.paused = false
    await vue.nextTick()
    assert.equal(runtime.frames.size, 1)
    runtime.document.hidden = true
    runtime.documentListeners.get('visibilitychange')()
    assert.equal(runtime.frames.size, 0)
    runtime.document.hidden = false
    runtime.documentListeners.get('visibilitychange')()
    assert.equal(runtime.frames.size, 1)
    runtime.motion.matches = true
    runtime.motionListeners.get('change')()
    assert.equal(runtime.frames.size, 0)
    runtime.motion.matches = false
    runtime.motionListeners.get('change')()
    assert.equal(runtime.frames.size, 1)
  } finally {
    runtime.close()
  }
  assert.equal(runtime.frames.size, 0)
  assert.equal(runtime.documentListeners.size, 0)
  assert.equal(runtime.motionListeners.size, 0)
  assert.equal(runtime.disconnected, true)
})

test('浏览器无法提供二维画布时不启动循环和全局监听，页面交互仍可挂载', () => {
  const runtime = canvasRuntime(false)
  try {
    assert.equal(runtime.frames.size, 0)
    assert.equal(runtime.documentListeners.size, 0)
    assert.equal(runtime.motionListeners.size, 0)
  } finally {
    runtime.close()
  }
})
