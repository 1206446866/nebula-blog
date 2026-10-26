import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import ts from 'typescript'
import * as vue from 'vue'
import vm from './helpers/vue-modules.mjs'

/** 在独立响应式作用域内检查寻星状态，结束时释放全部监听。 */
function fixture(names, currentSize = 12, entryName = 'useTagSearch') {
  const scope = vue.effectScope()
  const entry = new URL(`../src/views/home/tag/logic/${entryName}.ts`, import.meta.url)
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(readFileSync(entry, 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS },
    }).outputText,
    {
      module,
      exports: module.exports,
      require(name) {
        assert.equal(name, 'vue')
        // 本组检查纯状态；真实浏览器负责挂载、尺寸监听与离页验证。
        return { ...vue, onMounted() {}, onBeforeUnmount() {} }
      },
    },
    { entry },
  )
  const tags = vue.ref(
    names.map((name, index) => ({
      id: index + 1,
      name,
      description: '仅简介包含 secret',
      articleCount: index,
      createTime: '',
      updateTime: '',
    })),
  )
  const pageSize = vue.ref(currentSize)
  const pageTags = vue.ref(tags.value.slice(0, currentSize))
  const state = scope.run(() =>
    entryName === 'useTagSearch'
      ? module.exports.useTagSearch(tags, pageTags, pageSize)
      : module.exports.useTagConstellation(tags),
  )
  return { state, tags, pageTags, pageSize, close: () => scope.stop() }
}
const ids = (tags) => Array.from(tags, (tag) => tag.id)

test('标签寻星只匹配名称，忽略大小写、全角和首尾空白，原标签顺序不变', async () => {
  const f = fixture(['JavaScript', 'java', 'Java 并发', 'Vue'])
  try {
    const original = ids(f.tags.value)
    f.state.searchDraft.value = '  ＪＡＶＡ  '
    f.state.searchTags()
    assert.deepEqual(ids(f.state.searchMatches.value), [2, 1, 3])
    assert.deepEqual(ids(f.state.incomingTags.value), [])
    assert.deepEqual(ids(f.tags.value), original)
    f.state.searchDraft.value = 'secret'
    f.state.searchTags()
    assert.equal(f.state.searchMatches.value.length, 0)
    f.state.searchDraft.value = '   '
    await vue.nextTick()
    assert.equal(f.state.isSearching.value, false)
    assert.equal(f.state.matchedIds.value.size, 0)
  } finally {
    f.close()
  }
})

test('异页命中分束召入且不重复当前标签，所有结果可抵达，清除后恢复', async () => {
  const f = fixture([
    'Java',
    ...Array.from({ length: 11 }, (_, i) => `其他${i}`),
    ...Array.from({ length: 7 }, (_, i) => `Java ${i}`),
  ])
  try {
    f.state.searchDraft.value = 'java'
    f.state.searchTags()
    assert.equal(f.state.matchedIds.value.has(1), true)
    assert.equal(f.state.searchBatchCount.value, 3)
    const discovered = new Set()
    for (let wave = 0; wave < 3; wave++) {
      for (const tag of f.state.incomingTags.value) {
        assert.equal(tag.id > 12, true)
        assert.equal(discovered.has(tag.id), false)
        discovered.add(tag.id)
      }
      f.state.turnSearchBatch(1)
    }
    assert.equal(discovered.size, 7)
    assert.equal(f.state.searchBatch.value, 0)
    f.state.clearSearch()
    await vue.nextTick()
    assert.equal(f.state.incomingTags.value.length, 0)
    assert.equal(f.state.matchedIds.value.size, 0)
    assert.equal(f.pageTags.value.length, 12)
  } finally {
    f.close()
  }
})

test('切换原星图、手机密度或刷新标签后不会留下重复和过时的命中', async () => {
  const f = fixture(Array.from({ length: 18 }, (_, i) => `Java ${i}`))
  try {
    f.state.searchDraft.value = 'java'
    f.state.searchTags()
    f.state.turnSearchBatch(1)
    f.pageTags.value = f.tags.value.slice(12)
    await vue.nextTick()
    assert.equal(f.state.searchBatch.value, 0)
    assert.equal(
      f.state.incomingTags.value.every((tag) => tag.id <= 12),
      true,
    )
    f.pageSize.value = 6
    await vue.nextTick()
    assert.equal(f.state.incomingTags.value.length, 1)
    f.state.turnSearchBatch(-1)
    f.tags.value = [{ ...f.tags.value[12], name: 'Python' }]
    f.pageTags.value = f.tags.value
    await vue.nextTick()
    assert.equal(f.state.searchBatch.value, 0)
    assert.equal(f.state.searchMatches.value.length, 0)
    assert.equal(f.state.incomingTags.value.length, 0)
  } finally {
    f.close()
  }
})

test('寻星保留原页与原星尘坐标，异页标签获得独立落点与真实编号', async () => {
  const f = fixture(
    [
      'Java',
      ...Array.from({ length: 11 }, (_, i) => `其他${i}`),
      'Java 并发',
      'Java 工程',
      'Java 虚拟机',
    ],
    12,
    'useTagConstellation',
  )
  try {
    const originalTags = ids(f.state.visibleTags.value)
    const originalPosition = { ...f.state.nodeStyle(0, 1) }
    f.state.searchDraft.value = 'java'
    f.state.searchTags()
    await vue.nextTick()
    assert.equal(f.state.page.value, 0)
    assert.deepEqual(ids(f.state.visibleTags.value).slice(0, 12), originalTags)
    assert.deepEqual({ ...f.state.nodeStyle(0, 1) }, originalPosition)
    assert.equal(f.state.activeTag.value.id, 13)
    assert.equal(f.state.tagNumber(13), '13')
    const destinations = [13, 14, 15].map((id, index) => {
      const style = f.state.nodeStyle(12 + index, id)
      return `${style['--x']},${style['--y']}`
    })
    assert.equal(new Set(destinations).size, 3)
    f.state.clearSearch()
    await vue.nextTick()
    assert.deepEqual(ids(f.state.visibleTags.value), originalTags)
  } finally {
    f.close()
  }
})
