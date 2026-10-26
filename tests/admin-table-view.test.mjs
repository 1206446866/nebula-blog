import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { JSDOM } from 'jsdom'
import { parse, compileScript } from '@vue/compiler-sfc'
import ts from 'typescript'
import vm from './helpers/vue-modules.mjs'
import { Role } from '../src/constants/role.ts'

const dom = new JSDOM('<!doctype html><html><body></body></html>', {
  pretendToBeVisual: true,
  url: 'http://localhost/',
})
for (const key of [
  'window',
  'document',
  'Document',
  'Element',
  'HTMLElement',
  'SVGElement',
  'Node',
  'MouseEvent',
  'Event',
  'CustomEvent',
  'FocusEvent',
  'KeyboardEvent',
  'HTMLInputElement',
  'HTMLButtonElement',
  'MutationObserver',
  'getComputedStyle',
  'requestAnimationFrame',
  'cancelAnimationFrame',
])
  globalThis[key] =
    typeof dom.window[key] === 'function' &&
    ['getComputedStyle', 'requestAnimationFrame', 'cancelAnimationFrame'].includes(key)
      ? dom.window[key].bind(dom.window)
      : dom.window[key]
const vue = await import('vue')
const element = await import('element-plus')
const entry = new URL('../src/components/basic/BaseTable.vue', import.meta.url)
const source = readFileSync(entry, 'utf8')
const compiled = compileScript(parse(source, { filename: fileURLToPath(entry) }).descriptor, {
  id: 'base-table-view',
  inlineTemplate: true,
})
const module = { exports: {} }
vm.runInNewContext(
  ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
    .outputText,
  {
    module,
    exports: module.exports,
    require: (name) => {
      assert.equal(name, 'vue')
      return vue
    },
  },
  { entry },
)
const BaseTable = module.exports.default
const settle = async () => {
  await new Promise((resolve) => setImmediate(resolve))
  await vue.nextTick()
}

test('the real table renders row slots and reactive edits, displays its loading mask and offers a retry', async () => {
  const state = vue.reactive({
    data: [{ id: 7, status: '草稿' }],
    loading: true,
    error: null,
    total: 21,
    current: 2,
    size: 10,
  })
  const events = []
  const root = vue.defineComponent({
    setup: () => () =>
      vue.h(
        BaseTable,
        {
          ...state,
          columns: [
            { label: '编号', prop: 'id' },
            { label: '状态', slot: 'status' },
          ],
          onRetry: () => events.push('retry'),
          'onPage-change': (page) => events.push(['page', page]),
        },
        {
          status: ({ row, $index }) =>
            vue.h('strong', { class: 'row-status' }, `${row.id}/${row.status}/${$index}`),
        },
      ),
  })
  const container = document.createElement('div')
  document.body.append(container)
  const app = vue.createApp(root)
  app
    .component('ElTable', element.ElTable)
    .component('ElTableColumn', element.ElTableColumn)
    .component('ElPagination', element.ElPagination)
    .component('ElButton', element.ElButton)
    .directive('loading', element.ElLoading.directive)
  const warnings = []
  app.config.warnHandler = (message) => warnings.push(message)
  app.mount(container)
  try {
    await settle()
    assert.equal(container.querySelector('.row-status').textContent, '7/草稿/0')
    assert.ok(container.querySelector('.el-loading-mask'))
    assert.equal(container.querySelector('.el-pagination .btn-next').disabled, true)
    state.loading = false
    state.data[0].status = '已发布'
    await settle()
    assert.equal(container.querySelector('.row-status').textContent, '7/已发布/0')
    state.error = '列表加载失败，请重试'
    await settle()
    container.querySelector('.table-error button').click()
    assert.deepEqual(events, ['retry'])
    container.querySelector('.el-pagination .btn-next').click()
    assert.deepEqual(events, ['retry', ['page', 3]])
    state.total = 0
    state.data = []
    await settle()
    assert.equal(container.querySelector('.el-pagination'), null)
    assert.match(container.querySelector('.el-table__empty-text').textContent, /未能加载/)
    state.error = null
    await settle()
    assert.match(container.querySelector('.el-table__empty-text').textContent, /暂无数据/)
    assert.deepEqual(warnings, [])
  } finally {
    app.unmount()
    container.remove()
  }
})

function loadComponent(relative, dependencies) {
  const target = new URL(relative, import.meta.url)
  const descriptor = parse(readFileSync(target, 'utf8'), {
    filename: fileURLToPath(target),
  }).descriptor
  const code = ts.transpileModule(
    compileScript(descriptor, { id: relative, inlineTemplate: true }).content,
    { compilerOptions: { module: ts.ModuleKind.CommonJS } },
  ).outputText
  const module = { exports: {} }
  vm.runInNewContext(
    code,
    {
      module,
      exports: module.exports,
      require: (name) => {
        if (name === 'vue') return vue
        assert.ok(name in dependencies, `Unexpected dependency: ${name}`)
        return dependencies[name]
      },
    },
    { entry: target },
  )
  return module.exports.default
}

test('角色管理弹窗将历史前缀归入其它，跨栏目选择保留并按原ID保存', async () => {
  const saved = []
  const permissions = {
    getPermissionsApi: async () => ({
      data: {
        user: [{ id: 9, name: 'user:list', description: '查看用户' }],
        other: [{ id: 20, name: 'other:export', description: '导出' }],
        Test: [{ id: 10, name: 'Test:test', description: 'Test' }],
        a: [{ id: 11, name: 'a:test', description: 'aa' }],
      },
    }),
    getRolePermissionsApi: async (id) => {
      assert.equal(id, 4)
      return { data: { user: [{ id: 9 }], Test: [{ id: 10 }] } }
    },
    saveRolePermissionsApi: async (data) => saved.push(structuredClone(data)),
  }
  const dialog = loadComponent('../src/components/dialog/RolePermissionDialog.vue', {
    '@/api/permission.ts': permissions,
    '@/constants/role': { Role },
    'element-plus': { ElMessage: { success() {} } },
  })
  const tableEntry = new URL('../src/composables/useTable.ts', import.meta.url)
  const tableModule = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(readFileSync(tableEntry, 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS },
    }).outputText,
    {
      module: tableModule,
      exports: tableModule.exports,
      AbortController,
      setTimeout,
      clearTimeout,
      require: (name) => {
        assert.equal(name, 'vue')
        return vue
      },
    },
    { entry: tableEntry },
  )
  const component = loadComponent('../src/views/admin/RoleManager.vue', {
    '@/components/basic/BaseTable.vue': BaseTable,
    '@/components/basic/SafeButton.vue': loadComponent(
      '../src/components/basic/SafeButton.vue',
      {},
    ),
    '@/components/dialog/RolePermissionDialog.vue': dialog,
    '@/composables/useTable': tableModule.exports,
    '@/api/role': {
      getRolePageApi: async ({ current, size }) => ({
        data: {
          records: [{ id: 4, name: 'AUTHOR', description: '作者', createTime: '' }],
          pageNumber: current,
          pageSize: size,
          totalRow: 1,
        },
      }),
    },
    '@/utils/date': { formatDateTime: () => '' },
    'element-plus': { ElMessage: { success() {} }, ElMessageBox: {} },
  })
  const container = document.createElement('div')
  document.body.append(container)
  const app = vue.createApp(component),
    warnings = []
  for (const name of [
    'ElTable',
    'ElTableColumn',
    'ElPagination',
    'ElButton',
    'ElForm',
    'ElFormItem',
    'ElInput',
    'ElDialog',
    'ElAlert',
    'ElCard',
    'ElCheckboxGroup',
    'ElCheckbox',
  ])
    app.component(name, element[name])
  app.directive('loading', element.ElLoading.directive)
  app.config.warnHandler = (message) => warnings.push(message)
  app.mount(container)
  try {
    await settle()
    const open = [...container.querySelectorAll('.el-table__body-wrapper button')].find((button) =>
      button.textContent.includes('分配权限'),
    )
    assert.ok(open)
    open.click()
    await settle()
    const dialogElement = document.querySelector('.perm-dialog')
    assert.ok(dialogElement)
    assert.match(dialogElement.textContent, /作者/)
    assert.equal(dialogElement.querySelector('input[type="checkbox"]').checked, true)
    const switchModule = async (name) => {
      const button = [...dialogElement.querySelectorAll('.tabs button')].find(
        (item) => item.textContent.trim() === name,
      )
      assert.ok(button)
      button.click()
      await settle()
    }
    await switchModule('其它')
    assert.match(dialogElement.querySelector('.perm-card').textContent, /other:export/)
    assert.match(dialogElement.querySelector('.perm-card').textContent, /Test:test/)
    assert.match(dialogElement.querySelector('.perm-card').textContent, /a:test/)
    const legacy = [...dialogElement.querySelectorAll('.perm-item')].find((item) =>
      item.textContent.includes('Test:test'),
    )
    assert.equal(legacy.querySelector('input').checked, true)
    const anotherLegacy = [...dialogElement.querySelectorAll('.perm-item')].find((item) =>
      item.textContent.includes('a:test'),
    )
    const checkbox = anotherLegacy.querySelector('input')
    checkbox.checked = true
    checkbox.dispatchEvent(new window.Event('change', { bubbles: true }))
    await settle()
    await switchModule('用户权限')
    assert.equal(dialogElement.querySelector('input[type="checkbox"]').checked, true)
    const save = [...dialogElement.querySelectorAll('button')].find((button) =>
      button.textContent.includes('保存'),
    )
    save.click()
    await settle()
    assert.deepEqual(saved, [{ roleId: 4, permissionIds: [9, 10, 11] }])
    assert.deepEqual(warnings, [])
  } finally {
    app.unmount()
    container.remove()
  }
})
