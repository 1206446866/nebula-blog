import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from './helpers/vue-modules.mjs'
import ts from 'typescript'
import { JSDOM } from 'jsdom'
import { parse, compileScript } from '@vue/compiler-sfc'
import * as vue from 'vue'
import { buildArticleOutline } from '../src/utils/article-outline.ts'

let dom, renderMarkdown
before(async () => {
  dom = new JSDOM('<!doctype html><body></body>', { url: 'https://nebula.test/' })
  globalThis.window = dom.window
  ;({ renderMarkdown } = await import('../src/utils/markdown.ts'))
})
after(() => {
  dom.window.close()
  delete globalThis.window
})

function body(source) {
  const root = dom.window.document.createElement('div')
  root.innerHTML = renderMarkdown(source)
  return root
}
const entries = (source) => buildArticleOutline(body(source))

test('H1 and H2 produce numbered sections and reset child numbering at the next H1', () => {
  const headings = entries('# 开始\n\n## 环境\n\n## 运行\n\n# 进阶\n\n## 部署')
  assert.deepEqual(
    headings.map((item) => [item.number, item.level, item.title]),
    [
      ['1', 1, '开始'],
      ['1.1', 2, '环境'],
      ['1.2', 2, '运行'],
      ['2', 1, '进阶'],
      ['2.1', 2, '部署'],
    ],
  )
})

test('leading H2 headings are standalone roots without a fictitious zero parent', () => {
  const headings = entries('## 导读\n\n## 说明\n\n# 正文\n\n## 小节')
  assert.deepEqual(
    headings.map((item) => item.number),
    ['1', '2', '3', '3.1'],
  )
  assert.deepEqual(
    headings.map((item) => item.level),
    [1, 1, 1, 2],
  )
})

test('duplicate and punctuation-only titles get distinct stable anchors', () => {
  const root = body('# 重复\n\n## 重复\n\n# !!!\n\n## 重复')
  const first = buildArticleOutline(root)
  const second = buildArticleOutline(root)
  assert.equal(new Set(first.map((item) => item.id)).size, 4)
  assert.deepEqual(
    first.map((item) => item.id),
    second.map((item) => item.id),
  )
  assert.ok(first.every((item) => root.querySelector(`#${item.id}`) === item.element))
})

test('only actual rendered H1/H2 become entries, excluding code fences and lower headings', () => {
  const headings = entries(
    '# **正文**与`代码`\n\n```md\n# 假标题\n## 假小节\n```\n\n### 深层标题\n\n## [链接](https://example.com)',
  )
  assert.deepEqual(
    headings.map((item) => item.title),
    ['正文与代码', '链接'],
  )
})

test('unsafe raw HTML stays escaped in heading text and cannot inject anchors or event handlers', () => {
  const root = body('# <img src=x onerror=alert(1)>\n\n## <script>bad()</script>')
  const headings = buildArticleOutline(root)
  assert.equal(root.querySelector('script,img'), null)
  assert.match(headings[0].title, /<img/)
  assert.ok(headings.every((item) => /^article-section-\d+$/.test(item.id)))
})

test('a body with no supported headings has an empty directory', () => {
  assert.deepEqual(entries('正文\n\n### 三级标题'), [])
  const root = dom.window.document.createElement('div')
  root.innerHTML = '<h1> </h1><h2>有效标题</h2>'
  assert.deepEqual(
    buildArticleOutline(root).map((item) => item.number),
    ['1'],
  )
})

async function sidebar(source = '# 第一章\n\n## 第一节\n\n# 第二章') {
  const document = dom.window.document
  document.body.innerHTML = '<div id="article-content"><div class="markdown-body"></div></div>'
  const article = document.getElementById('article-content')
  article.querySelector('.markdown-body').innerHTML = renderMarkdown(source)
  Object.defineProperty(article, 'scrollHeight', { value: 1000, configurable: true })
  article.getBoundingClientRect = () => ({ top: -200 })
  const frames = new Map(),
    listeners = new Map(),
    mounts = [],
    cleanups = [],
    observers = []
  let nextFrame = 1
  const browser = {
    innerHeight: 800,
    matchMedia: () => ({ matches: true }),
    requestAnimationFrame: (callback) => {
      const id = nextFrame++
      frames.set(id, callback)
      return id
    },
    cancelAnimationFrame: (id) => frames.delete(id),
    addEventListener: (name, handler) => listeners.set(name, handler),
    removeEventListener: (name) => listeners.delete(name),
  }
  const componentSource = compileScript(
    parse(
      readFileSync(
        new URL('../src/views/home/article/detail/components/ArticleSidebar.vue', import.meta.url),
        'utf8',
      ),
    ).descriptor,
    { id: 'sidebar-test' },
  ).content
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(componentSource, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
      .outputText,
    {
      module,
      exports: module.exports,
      document,
      window: browser,
      ResizeObserver: class {
        constructor(callback) {
          this.callback = callback
          this.disconnected = false
          observers.push(this)
        }
        observe() {}
        disconnect() {
          this.disconnected = true
        }
      },
      require: (name) => {
        if (name === 'vue')
          return {
            ...vue,
            onMounted: (callback) => mounts.push(callback),
            onBeforeUnmount: (callback) => cleanups.push(callback),
          }
        if (name === '@/utils/article-outline') return { buildArticleOutline }
        throw new Error(name)
      },
    },
    {
      entry: new URL(
        '../src/views/home/article/detail/components/ArticleSidebar.vue',
        import.meta.url,
      ),
    },
  )
  const props = vue.reactive({ articleId: 1, content: source })
  const scope = vue.effectScope()
  const view = scope.run(() => module.exports.default.setup(props, { expose() {} }))
  mounts.forEach((callback) => callback())
  await new Promise((resolve) => setImmediate(resolve))
  return {
    view,
    props,
    article,
    frames,
    listeners,
    observers,
    dispose: () => {
      cleanups.forEach((callback) => callback())
      scope.stop()
      document.body.innerHTML = ''
    },
    flush: () => {
      const callbacks = [...frames.values()]
      frames.clear()
      callbacks.forEach((callback) => callback())
    },
  }
}

test('the actual sidebar updates progress and current heading, and a directory click focuses its target', async () => {
  const h = await sidebar()
  try {
    h.view.headings.value.forEach((heading, index) => {
      heading.element.getBoundingClientRect = () => ({ top: [-100, 150, 400][index] })
    })
    h.flush()
    assert.equal(h.view.progress.value, 80)
    assert.equal(h.view.activeId.value, h.view.headings.value[1].id)
    assert.equal(h.view.sections.value[0].children.length, 1)
    const target = h.view.headings.value[2]
    let scroll
    target.element.scrollIntoView = (options) => {
      scroll = options
    }
    h.view.goToHeading(target)
    assert.equal(scroll.behavior, 'instant')
    assert.equal(dom.window.document.activeElement, target.element)
    assert.equal(h.view.activeId.value, target.id)
  } finally {
    h.dispose()
  }
})

test('changing articles rebuilds the directory and an image resize schedules an update', async () => {
  const h = await sidebar()
  try {
    h.flush()
    h.article.querySelector('.markdown-body').innerHTML = renderMarkdown('# 新文章\n\n## 新小节')
    h.props.articleId = 2
    h.props.content = '# 新文章\n\n## 新小节'
    await new Promise((resolve) => setImmediate(resolve))
    assert.equal(h.view.headings.value[0].title, '新文章')
    assert.equal(h.view.headings.value.length, 2)
    assert.equal(h.observers[0].disconnected, true)
    h.flush()
    h.observers.at(-1).callback()
    assert.equal(h.frames.size, 1)
  } finally {
    h.dispose()
  }
})

test('unmount cancels pending frames and disconnects layout observation and event listeners', async () => {
  const h = await sidebar()
  h.dispose()
  assert.equal(h.frames.size, 0)
  assert.equal(h.listeners.size, 0)
  assert.ok(h.observers.every((observer) => observer.disconnected))
  h.observers.at(-1).callback()
  assert.equal(h.frames.size, 0)
})
