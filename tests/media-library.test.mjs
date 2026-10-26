import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { compileScript, parse } from '@vue/compiler-sfc'
import * as vue from 'vue'
import { Role } from '../src/constants/role.ts'

function harness() {
  const state = vue.reactive({ permissions: [], deletes: 0 })
  const source = readFileSync(new URL('../src/components/media/MediaLibrary.vue', import.meta.url), 'utf8')
  const code = ts.transpileModule(compileScript(parse(source).descriptor, { id: 'media-library' }).content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const deps = {
    vue: { ...vue, onMounted() {}, onBeforeUnmount() {} },
    'element-plus': { ElMessage: {}, ElMessageBox: { confirm: assert.fail } },
    '@/api/media': { deleteMediaApi: () => { state.deletes++; return Promise.resolve({ data: true }) } },
    '@/stores/auth': { useAuthStore: () => ({ user: { id: 1 }, getToken: () => 'owner', hasRole: () => true, hasPermission: (permission) => state.permissions.includes(permission) }) },
    '@/constants/role': { Role },
    '@/utils/date': { formatDateTime: () => '' },
    './MediaThumbnail.vue': {},
  }
  const module = { exports: {} }
  vm.runInNewContext(code, { module, exports: module.exports, AbortController, require: (name) => { assert.ok(name in deps, name); return deps[name] } })
  return { state, view: module.exports.default.setup({}, { emit() {}, expose() {} }) }
}
test('media management actions require both the admin role and the matching business permission', () => {
  const h = harness()
  h.view.selected.value = { asset: { id: 9, userId: 2, business: 'article' }, references: [] }
  assert.equal(h.view.canViewAll.value, false)
  assert.equal(h.view.canDelete.value, false)
  h.state.permissions.push('article:list')
  assert.equal(h.view.canViewAll.value, true)
  assert.equal(h.view.canDelete.value, false)
  h.state.permissions.push('article:delete')
  assert.equal(h.view.canDelete.value, true)
  h.view.selected.value.asset.business = 'avatar'
  assert.equal(h.view.canDelete.value, false)
  h.state.permissions.push('user:update')
  assert.equal(h.view.canDelete.value, true)
})
test('an owner can manage unused media but referenced images cannot enter the deletion flow', async () => {
  const h = harness()
  h.view.selected.value = { asset: { id: 9, userId: 1, business: 'avatar' }, references: [{ business: 'avatar' }] }
  assert.equal(h.view.canDelete.value, true)
  await h.view.remove()
  assert.equal(h.state.deletes, 0)
})
