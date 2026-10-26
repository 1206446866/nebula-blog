import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from './helpers/vue-modules.mjs'
import ts from 'typescript'
import { JSDOM } from 'jsdom'
import { parse, compileScript } from '@vue/compiler-sfc'
import { insertMarkdownCode, setMarkdownHeading } from '../src/utils/markdown-edit.ts'

let dom, vue, renderMarkdown
before(async () => {
  dom = new JSDOM('<!doctype html><body></body>', { url: 'https://nebula.test/' })
  for (const key of ['window', 'document', 'Element', 'HTMLElement', 'SVGElement', 'Node'])
    globalThis[key] = dom.window[key]
  vue = await import('vue')
  ;({ renderMarkdown } = await import('../src/utils/markdown.ts'))
})
after(() => {
  dom.window.close()
  for (const key of ['window', 'document', 'Element', 'HTMLElement', 'SVGElement', 'Node'])
    delete globalThis[key]
})

test('heading actions format the current line and switch levels without accumulating markers', () => {
  const source = '前言\n正在写的小节\n下一段'
  const first = setMarkdownHeading(source, 8, 8, 1)
  assert.equal(first.content, '前言\n# 正在写的小节\n下一段')
  const second = setMarkdownHeading(first.content, first.selectionStart, first.selectionEnd, 2)
  assert.equal(second.content, '前言\n## 正在写的小节\n下一段')
  assert.equal(
    setMarkdownHeading(second.content, second.selectionStart, second.selectionEnd, 2).content,
    second.content,
  )
})

test('heading actions select the empty-line placeholder so typing replaces it', () => {
  for (const level of [1, 2]) {
    const result = setMarkdownHeading('', 0, 0, level)
    assert.equal(
      result.content.slice(result.selectionStart, result.selectionEnd),
      level === 1 ? '一级标题' : '二级标题',
    )
    assert.match(renderMarkdown(result.content), new RegExp(`<h${level}>`))
  }
})

test('multiline heading formatting keeps blank lines and does not modify the next unselected line', () => {
  const result = setMarkdownHeading('第一行\n\n第二行\n第三行', 0, 9, 2)
  assert.equal(result.content, '## 第一行\n\n## 第二行\n第三行')
})

test('inline code insertion preserves literal backticks inside JavaScript templates', () => {
  const code = 'const name = `Nebula`'
  const result = insertMarkdownCode(code, 0, code.length, false)
  const root = document.createElement('div')
  root.innerHTML = renderMarkdown(result.content)
  assert.equal(root.querySelector('code').textContent, code)
  assert.equal(root.textContent.trim(), code)
})

test('inline code insertion preserves backticks at both edges instead of merging delimiters', () => {
  const code = '`field`'
  const result = insertMarkdownCode(code, 0, code.length, false)
  const root = document.createElement('div')
  root.innerHTML = renderMarkdown(result.content)
  assert.equal(root.querySelector('code').textContent, code)
})

test('code-block insertion can contain Markdown fences without closing the outer fence', () => {
  const code = '```javascript\nconst name = `Nebula`\n```'
  const result = insertMarkdownCode(code, 0, code.length, true)
  const root = document.createElement('div')
  root.innerHTML = renderMarkdown(result.content)
  assert.equal(root.querySelectorAll('pre').length, 1)
  assert.equal(root.querySelector('pre code').textContent, code + '\n')
  assert.equal(root.querySelector('p'), null)
})

function mount(relative, props) {
  const source = compileScript(
    parse(readFileSync(new URL(relative, import.meta.url), 'utf8')).descriptor,
    { id: 'markdown-preview-test', inlineTemplate: true },
  ).content
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText,
    {
      module,
      exports: module.exports,
      require: (name) => {
        if (name === 'vue') return vue
        if (name === '@/utils/markdown') return { renderMarkdown }
        if (name === '@/composables/useMediaImages') return { useMediaImages() {} }
        if (name === '@/styles/markdown.css') return {}
        throw new Error(name)
      },
    },
    { entry: new URL(relative, import.meta.url) },
  )
  const container = document.createElement('div')
  document.body.append(container)
  const app = vue.createApp(module.exports.default, props)
  app.mount(container)
  return {
    body: container.querySelector('.markdown-body'),
    dispose: () => {
      app.unmount()
      container.remove()
    },
  }
}

const cases = [
  ['inline code', '执行 `npm run build` 后打开页面。', ['npm run build']],
  ['fenced JavaScript', '```javascript\nconst name = "Nebula"\n```', ['const name = "Nebula"\n']],
  ['inline code containing a literal backtick', '使用 `` `field` `` 表示字段。', ['`field`']],
  [
    'a fenced Markdown example',
    '````md\n```javascript\nconst name = `Nebula`\n```\n````',
    ['```javascript\nconst name = `Nebula`\n```\n'],
  ],
  ['code in headings', '# 安装 `Vue`\n\n## 执行 `npm install`', ['Vue', 'npm install']],
]
for (const [name, content, expected] of cases) {
  test(`actual authoring preview and article body render ${name} identically`, () => {
    const preview = mount('../src/components/article/ArticlePreview.vue', {
      title: '预览',
      content,
      author: '作者',
      tags: [],
    })
    const article = mount('../src/views/home/article/detail/components/ArticleContent.vue', {
      content,
    })
    try {
      assert.deepEqual(
        [...preview.body.querySelectorAll('code')].map((code) => code.textContent),
        expected,
      )
      assert.equal(preview.body.innerHTML, article.body.innerHTML)
      assert.equal(preview.body.textContent, article.body.textContent)
    } finally {
      preview.dispose()
      article.dispose()
    }
  })
}
