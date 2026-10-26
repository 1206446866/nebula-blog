import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { JSDOM } from 'jsdom'
import { compileScript, parse } from '@vue/compiler-sfc'
import ts from 'typescript'
import vm from './helpers/vue-modules.mjs'
import * as markdownEdit from '../src/utils/markdown-edit.ts'
import * as textHistory from '../src/utils/text-history.ts'
import * as publicationTime from '../src/utils/publication-time.ts'
import { positiveId } from '../src/utils/article-query.ts'
import { Role } from '../src/constants/role.ts'

const dom = new JSDOM('<!doctype html><html><body></body></html>')
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
const settle = async () => {
  await new Promise((resolve) => setImmediate(resolve))
  await vue.nextTick()
}

function mount(relative, props, dependencies = {}) {
  const entry = new URL(relative, import.meta.url)
  const source = readFileSync(entry, 'utf8')
  const descriptor = parse(source, { filename: fileURLToPath(entry) }).descriptor
  const compiled = compileScript(descriptor, { id: relative, inlineTemplate: true })
  const code = ts.transpileModule(compiled.content, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
  }).outputText
  const module = { exports: {} }
  vm.runInNewContext(
    code,
    {
      module,
      exports: module.exports,
      AbortController,
      setInterval,
      clearInterval,
      window,
      require(name) {
        if (name === 'vue') return vue
        assert.ok(name in dependencies, `Unexpected dependency: ${name}`)
        return dependencies[name]
      },
    },
    { entry },
  )
  const container = document.createElement('div')
  document.body.append(container)
  const app = vue.createApp(module.exports.default, props)
  const slot = vue.defineComponent({
    inheritAttrs: false,
    setup(_, { slots }) {
      return () => vue.h('div', slots.default?.())
    },
  })
  for (const name of new Set(
    [...descriptor.template.content.matchAll(/<(el-[\w-]+|RouterLink)\b/g)].map((m) => m[1]),
  ))
    app.component(name, slot)
  app.directive('loading', {})
  app.config.warnHandler = (message) => {
    if (/not defined on instance|Failed to resolve/.test(message)) assert.fail(message)
  }
  app.mount(container)
  return {
    container,
    close() {
      app.unmount()
      container.remove()
    },
  }
}

test('the rendered writer keeps its textarea selection and undo history after TS extraction', async () => {
  const route = vue.reactive({ params: { id: '7' }, query: {} })
  const h = mount(
    '../src/views/home/article/write/ArticleWriter.vue',
    {},
    {
      'vue-router': {
        useRoute: () => route,
        useRouter: () => ({ replace: async () => {} }),
        onBeforeRouteLeave() {},
        onBeforeRouteUpdate() {},
      },
      'element-plus': { ElMessage: {}, ElMessageBox: {} },
      '@element-plus/icons-vue': Object.fromEntries(
        ['ArrowLeft', 'Check', 'Document', 'EditPen', 'View', 'Monitor', 'Cellphone'].map(
          (name) => [name, {}],
        ),
      ),
      '@/components/article/ArticlePreview.vue': { render: () => null },
      '@/components/media/MediaLibrary.vue': { render: () => null },
      '@/api/article': {
        getArticleForEditApi: async () => ({
          data: {
            id: 7,
            status: 0,
            title: '测试文章',
            content: '原始正文',
            categoryId: 3,
            tagIds: [],
          },
        }),
      },
      '@/api/category': { listCategoryApi: async () => ({ data: [] }) },
      '@/api/tag': { listTagApi: async () => ({ data: [] }) },
      '@/api/media': {},
      '@/stores/auth': {
        useAuthStore: () => ({
          user: { id: 1, username: '作者' },
          getToken: () => 'one',
          hasPermission: () => true,
          hasRole: () => true,
        }),
      },
      '@/constants/role': { Role },
      '@/utils/article-query': { positiveId },
      '@/utils/date': { formatDateTime: (value) => value },
      '@/utils/markdown-edit': markdownEdit,
      '@/utils/text-history': textHistory,
      '@/utils/publication-time': publicationTime,
      '@/utils/resource': {},
    },
  )
  try {
    await settle()
    const textarea = h.container.querySelector('textarea')
    assert.equal(textarea.value, '原始正文')
    textarea.setSelectionRange(0, 2)
    h.container.querySelector('[title="加粗"]').click()
    await settle()
    assert.equal(textarea.value, '**原始**正文')
    assert.equal(textarea.selectionStart, 2)
    assert.equal(textarea.selectionEnd, 4)
    textarea.dispatchEvent(
      new window.KeyboardEvent('keydown', {
        key: 'z',
        ctrlKey: true,
        bubbles: true,
        cancelable: true,
      }),
    )
    await settle()
    assert.equal(textarea.value, '原始正文')
    h.container.querySelector('[aria-label="重做正文修改"]').click()
    await settle()
    assert.equal(textarea.value, '**原始**正文')
  } finally {
    h.close()
  }
})

test('profile paging still emits changes, follows reactive props and clamps a submitted page', async () => {
  const events = []
  const props = vue.reactive({ currentPage: 2, pageSize: 10, total: 100, loading: false })
  // 通过父组件挂载，使属性更新过程与实际个人主页、搜索页面一致。
  const entry = new URL('../src/views/home/profile/ProfilePagination.vue', import.meta.url)
  const source = readFileSync(entry, 'utf8')
  const compiled = compileScript(parse(source).descriptor, {
    id: 'reactive-pager',
    inlineTemplate: true,
  })
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
      .outputText,
    { module, exports: module.exports, require: () => vue },
    { entry },
  )
  const parent = vue.defineComponent({
    setup: () => () =>
      vue.h(module.exports.default, {
        ...props,
        'onUpdate:currentPage': (page) => events.push(page),
      }),
  })
  const container = document.createElement('div')
  document.body.append(container)
  const app = vue.createApp(parent)
  app.mount(container)
  try {
    container.querySelector('[aria-label="下一页"]').click()
    assert.deepEqual(events, [3])
    props.currentPage = 4
    await vue.nextTick()
    const input = container.querySelector('input')
    assert.equal(input.value, '4')
    input.value = '999'
    input.dispatchEvent(new window.Event('input', { bubbles: true }))
    container.querySelector('form').dispatchEvent(new window.Event('submit', { cancelable: true }))
    assert.deepEqual(events, [3, 10])
    props.loading = true
    await vue.nextTick()
    container.querySelector('[aria-label="下一页"]').click()
    assert.deepEqual(events, [3, 10])
  } finally {
    app.unmount()
    container.remove()
  }
})
