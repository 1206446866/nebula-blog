import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from './helpers/vue-modules.mjs'
import ts from 'typescript'
import * as vue from 'vue'
import { positiveId } from '../src/utils/article-query.ts'
import { Role } from '../src/constants/role.ts'
import * as markdownEdit from '../src/utils/markdown-edit.ts'
import * as textHistory from '../src/utils/text-history.ts'
import * as publicationTime from '../src/utils/publication-time.ts'
import * as resource from '../src/utils/resource.ts'

const source = readFileSync(
  new URL('../src/views/home/article/write/logic/useArticleWriter.ts', import.meta.url),
  'utf8',
)
const code = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
}).outputText
const settle = () => new Promise((resolve) => setImmediate(resolve))
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}

function harness(options = {}) {
  const state = {
    token: 'owner',
    permissions: ['article:create', 'article:update'],
    roles: ['ADMIN'],
    calls: [],
    routes: [],
    confirms: 0,
    confirm: true,
    ...options,
  }
  const route = vue.reactive({ params: options.id ? { id: String(options.id) } : {}, query: {} })
  const auth = {
    user: { id: 1, username: 'Author' },
    getToken: () => state.token,
    hasPermission: (permission) => state.permissions.includes(permission),
    hasRole: (role) => state.roles.includes(role),
  }
  const cleanups = []
  const hooks = {}
  const initial = {
    id: options.id || 12,
    title: 'Loaded article',
    content: 'Existing body',
    categoryId: 3,
    tagIds: [4],
    author: 'Original author',
    status: 0,
    ...options.article,
  }
  const deps = {
    vue: { ...vue, onMounted: () => {}, onBeforeUnmount: (callback) => cleanups.push(callback) },
    'vue-router': {
      useRoute: () => route,
      useRouter: () => ({
        replace: async (target) => {
          state.routes.push(target)
          route.params = { id: String(target.params.id) }
        },
      }),
      onBeforeRouteLeave: (callback) => {
        hooks.leave = callback
      },
      onBeforeRouteUpdate: (callback) => {
        hooks.update = callback
      },
    },
    'element-plus': {
      ElMessage: { warning() {}, info() {}, success() {} },
      ElMessageBox: {
        confirm: async () => {
          state.confirms++
          if (!state.confirm) throw new Error('cancel')
        },
      },
    },
    '@element-plus/icons-vue': {},
    '@/components/article/ArticlePreview.vue': {},
    '@/components/media/MediaLibrary.vue': {},
    '@/api/media': {
      uploadArticleImageApi: async (file, signal) => {
        state.calls.push(['image', file, signal])
        return state.image
          ? state.image(file)
          : {
              data: {
                id: 1,
                path: 'upload/article/owner/20261023/0123456789abcdef0123456789abcdef.png',
                originalName: file.name,
              },
            }
      },
    },
    '@/utils/resource': resource,
    '@/api/article': {
      createDraftApi: async (data) => {
        state.calls.push(['create', data])
        return state.create ? state.create() : { data: 12 }
      },
      updateArticleApi: async (data) => {
        state.calls.push(['update', data])
        return state.update ? state.update() : { data: true }
      },
      changeArticleStatusApi: async (data) => {
        state.calls.push(['publish', data])
        return state.publish ? state.publish() : { data: true }
      },
      scheduleArticleApi: async (id, time) => {
        state.calls.push(['schedule', id, time])
        return state.schedule ? state.schedule(id, time) : { data: '2099-01-01 10:30:00' }
      },
      cancelArticleScheduleApi: async (id) => {
        state.calls.push(['cancelSchedule', id])
        return state.cancelSchedule ? state.cancelSchedule(id) : { data: true }
      },
      getArticleForEditApi: async (id, signal) => {
        state.calls.push(['load', id, signal])
        return state.load ? state.load(id) : { data: { ...initial, id } }
      },
    },
    '@/api/category': { listCategoryApi: async () => ({ data: [] }) },
    '@/api/tag': { listTagApi: async () => ({ data: [] }) },
    '@/stores/auth': { useAuthStore: () => auth },
    '@/constants/role': { Role },
    '@/utils/article-query': { positiveId },
    '@/utils/date': { formatDateTime: () => '2026-10-22 20:00:00' },
    '@/utils/markdown-edit': markdownEdit,
    '@/utils/text-history': textHistory,
    '@/utils/publication-time': publicationTime,
  }
  const module = { exports: {} }
  vm.runInNewContext(
    code,
    {
      module,
      exports: module.exports,
      AbortController,
      window: { removeEventListener() {} },
      require: (name) => {
        assert.ok(name in deps, name)
        return deps[name]
      },
    },
    {
      entry: new URL('../src/views/home/article/write/logic/useArticleWriter.ts', import.meta.url),
    },
  )
  const scope = vue.effectScope()
  const view = scope.run(() => module.exports.useArticleWriter())
  return {
    state,
    route,
    hooks,
    view,
    fill: () =>
      Object.assign(view.form, {
        title: ' New title ',
        content: '## Draft body',
        categoryId: 3,
        tagIds: [4],
      }),
    dispose: () => {
      cleanups.forEach((callback) => callback())
      scope.stop()
    },
  }
}

function attachBody(h, content = '正文', start = content.length, end = start) {
  h.view.form.content = content
  const input = {
    value: content,
    selectionStart: start,
    selectionEnd: end,
    selectionDirection: 'none',
    focus() {},
    setSelectionRange(start, end, direction = 'none') {
      this.selectionStart = start
      this.selectionEnd = end
      this.selectionDirection = direction
    },
  }
  h.view.textarea.value = input
  // Vue 会代理响应式引用，此处使用与编辑器相同的输入框对象。
  return h.view.textarea.value
}
function typeBody(h, input, value, inputType = 'insertText') {
  h.view.beforeBodyInput({ inputType })
  input.value = value
  input.setSelectionRange(value.length, value.length)
  h.view.bodyInput({ target: input, inputType })
}
function shortcut(h, key, extra = {}) {
  let prevented = false
  h.view.bodyKeydown({
    key,
    ctrlKey: true,
    preventDefault() {
      prevented = true
    },
    ...extra,
  })
  return prevented
}

for (const [name, action] of [
  ['加粗', (v) => v.insertMarkdown('**', '**')],
  ['斜体', (v) => v.insertMarkdown('*', '*')],
  ['链接', (v) => v.insertMarkdown('[', '](https://example.com)')],
  ['一级标题', (v) => v.insertHeading(1)],
  ['二级标题', (v) => v.insertHeading(2)],
  ['行内代码', (v) => v.insertCode(false)],
  ['代码块', (v) => v.insertCode(true)],
  ['引用', (v) => v.insertMarkdown('\n> ', '\n')],
  ['列表', (v) => v.insertMarkdown('\n- ', '\n')],
]) {
  test(`${name} participates in the same undo and redo history as normal body input`, async () => {
    const h = harness()
    try {
      const input = attachBody(h)
      typeBody(h, input, '正文输入')
      input.setSelectionRange(0, 4)
      await action(h.view)
      await settle()
      const formatted = h.view.form.content
      assert.notEqual(formatted, '正文输入')
      assert.equal(shortcut(h, 'z'), true)
      await settle()
      assert.equal(h.view.form.content, '正文输入')
      assert.equal(input.selectionStart, 0)
      assert.equal(input.selectionEnd, 4)
      shortcut(h, 'z')
      await settle()
      assert.equal(h.view.form.content, '正文')
      shortcut(h, 'y')
      await settle()
      assert.equal(h.view.form.content, '正文输入')
      shortcut(h, 'Z', { shiftKey: true })
      await settle()
      assert.equal(h.view.form.content, formatted)
    } finally {
      h.dispose()
    }
  })
}

test('media reuse and asynchronous upload insertions can each be undone as one operation', async () => {
  for (const upload of [false, true]) {
    const h = harness()
    try {
      attachBody(h, 'before after', 6)
      h.view.captureImagePosition()
      if (upload) await h.view.uploadImages([imageFile])
      else await h.view.selectMedia(mediaAsset)
      const withImage = h.view.form.content
      assert.match(withImage, /!\[/)
      h.view.moveBodyHistory()
      await settle()
      assert.equal(h.view.form.content, 'before after')
      h.view.moveBodyHistory(true)
      await settle()
      assert.equal(h.view.form.content, withImage)
    } finally {
      h.dispose()
    }
  }
})

test('a Chinese composition is recorded as a whole and undo is ignored while the IME is active', async () => {
  const h = harness()
  try {
    const input = attachBody(h, '')
    h.view.startBodyComposition()
    typeBody(h, input, 'ni', 'insertCompositionText')
    assert.equal(shortcut(h, 'z', { isComposing: true }), false)
    assert.equal(h.view.form.content, 'ni')
    input.value = '你好'
    input.setSelectionRange(2, 2)
    h.view.endBodyComposition({ target: input })
    shortcut(h, 'z')
    await settle()
    assert.equal(h.view.form.content, '')
    shortcut(h, 'z', { ctrlKey: false, metaKey: true, shiftKey: true })
    await settle()
    assert.equal(h.view.form.content, '你好')
  } finally {
    h.dispose()
  }
})

test('native history input events use the shared history and a new edit discards the redo branch', async () => {
  const h = harness()
  try {
    const input = attachBody(h)
    await h.view.insertMarkdown('**', '**')
    await settle()
    let prevented = false
    h.view.beforeBodyInput({
      inputType: 'historyUndo',
      cancelable: true,
      preventDefault() {
        prevented = true
      },
    })
    await settle()
    assert.equal(prevented, true)
    assert.equal(h.view.form.content, '正文')
    typeBody(h, input, '新正文', 'insertFromPaste')
    assert.equal(h.view.canRedoBody.value, false)
    shortcut(h, 'y')
    await settle()
    assert.equal(h.view.form.content, '新正文')
  } finally {
    h.dispose()
  }
})

test('saving preserves undo history and undo after saving marks the article dirty', async () => {
  const h = harness()
  try {
    h.fill()
    attachBody(h, '正文')
    await h.view.insertMarkdown('**', '**')
    await settle()
    await h.view.save()
    await settle()
    assert.equal(h.view.dirty.value, false)
    h.view.moveBodyHistory()
    await settle()
    assert.equal(h.view.form.content, '正文')
    assert.equal(h.view.dirty.value, true)
    h.route.params.id = '99'
    await settle()
    assert.equal(h.view.canUndoBody.value, false)
    h.view.moveBodyHistory()
    await settle()
    assert.equal(h.view.form.content, 'Existing body')
  } finally {
    h.dispose()
  }
})

test('saving a new draft returns to its own editor and later saves update it', async () => {
  const h = harness()
  try {
    h.fill()
    await h.view.save()
    await settle()
    assert.equal(h.view.id.value, 12)
    assert.equal(h.view.dirty.value, false)
    assert.equal(h.view.form.title, 'New title')
    assert.equal(h.state.routes[0].name, 'ArticleEdit')
    h.view.form.content = 'Changed'
    await h.view.save()
    assert.deepEqual(
      h.state.calls.map((call) => call[0]),
      ['create', 'update'],
    )
  } finally {
    h.dispose()
  }
})

test('failed publication keeps the created draft ID and a retry does not create twice', async () => {
  const h = harness({
    publish: async () => {
      throw new Error('offline')
    },
  })
  try {
    h.fill()
    await h.view.save(true)
    await settle()
    assert.equal(h.view.id.value, 12)
    assert.equal(h.view.status.value, 0)
    assert.match(h.view.notice.value, /草稿已保存.*发布未完成/)
    h.state.publish = async () => ({ data: true })
    await h.view.save(true)
    assert.deepEqual(
      h.state.calls.map((call) => call[0]),
      ['create', 'publish', 'update', 'publish'],
    )
    assert.equal(h.view.status.value, 1)
  } finally {
    h.dispose()
  }
})

test('failed saves keep unsaved content and leave protection, while canceling publication makes no request', async () => {
  const h = harness({
    create: async () => {
      throw new Error('offline')
    },
  })
  try {
    h.fill()
    await h.view.save()
    assert.equal(h.view.dirty.value, true)
    assert.equal(h.view.form.content, '## Draft body')
    h.state.confirm = false
    assert.equal(await h.hooks.leave(), false)
    assert.equal(await h.hooks.update({ params: { id: '2' } }, { params: {} }), false)
    const count = h.state.calls.length
    await h.view.save(true)
    assert.equal(h.state.calls.length, count)
  } finally {
    h.dispose()
  }
})

test('read mode and narrow preview do not perform writes', async () => {
  const h = harness({ id: 7 })
  try {
    await settle()
    h.view.mode.value = 'preview'
    h.view.previewWidth.value = 'mobile'
    await settle()
    assert.equal(h.state.calls.length, 1)
    assert.equal(h.state.calls[0][0], 'load')
    assert.equal(h.state.calls[0][1], 7)
    assert.equal(h.view.author.value, 'Original author')
    assert.equal(h.view.dirty.value, false)
  } finally {
    h.dispose()
  }
})

test('stale article loads cannot overwrite the newly selected article', async () => {
  const pending = deferred()
  const h = harness({
    id: 7,
    load: (id) =>
      id === 7
        ? pending.promise
        : Promise.resolve({
            data: { id, title: 'New route', content: 'New', tagIds: [], status: 0 },
          }),
  })
  try {
    h.route.params.id = '8'
    await settle()
    pending.resolve({ data: { id: 7, title: 'Old route', content: 'Old', tagIds: [] } })
    await settle()
    assert.equal(h.view.id.value, 8)
    assert.equal(h.view.form.title, 'New route')
    assert.equal(h.state.calls[0][2].aborted, true)
  } finally {
    h.dispose()
  }
})

for (const reason of ['account switch', 'unmount']) {
  test(`${reason} during save cannot trigger publication or navigation`, async () => {
    const pending = deferred()
    const h = harness({ create: () => pending.promise })
    try {
      h.fill()
      const saving = h.view.save(true)
      await settle()
      if (reason === 'account switch') h.state.token = 'new-account'
      else h.dispose()
      pending.resolve({ data: 12 })
      await saving
      assert.deepEqual(
        h.state.calls.map((call) => call[0]),
        ['create'],
      )
      assert.equal(h.state.routes.length, 0)
    } finally {
      h.dispose()
    }
  })
}

test('ordinary authors cannot publish and creation-only permission does not permit later editing', async () => {
  const h = harness({ roles: ['USER'], permissions: ['article:create'] })
  try {
    h.fill()
    await h.view.save(true)
    assert.equal(h.state.calls.length, 0)
    await h.view.save()
    assert.equal(h.view.id.value, 12)
    assert.equal(h.state.routes.length, 0)
    await h.view.save()
    assert.equal(h.state.calls.length, 1)
    assert.equal(h.view.canEdit.value, false)
  } finally {
    h.dispose()
  }
})

test('double clicks cannot create duplicate drafts while a save is pending', async () => {
  const pending = deferred()
  const h = harness({ create: () => pending.promise })
  try {
    h.fill()
    const saving = h.view.save()
    await h.view.save()
    assert.equal(h.state.calls.length, 1)
    assert.equal(await h.hooks.leave(), false)
    pending.resolve({ data: 12 })
    await saving
  } finally {
    h.dispose()
  }
})

test('missing required fields block saving while empty content can still be previewed', async () => {
  const h = harness()
  try {
    await h.view.save()
    h.view.form.title = 'Title'
    await h.view.save()
    h.view.form.categoryId = 3
    await h.view.save()
    assert.equal(h.state.calls.length, 0)
  } finally {
    h.dispose()
  }
})

test('the shared create/edit writer heading controls format the selected line and return focus', async () => {
  const h = harness()
  try {
    let focused = false
    let selection
    h.view.form.content = '小节标题'
    h.view.textarea.value = {
      selectionStart: 2,
      selectionEnd: 2,
      focus: () => {
        focused = true
      },
      setSelectionRange: (...range) => {
        selection = range
      },
    }
    h.view.insertHeading(1)
    await settle()
    assert.equal(h.view.form.content, '# 小节标题')
    assert.equal(focused, true)
    assert.deepEqual(selection.slice(0, 2), [4, 4])
  } finally {
    h.dispose()
  }
})

const mediaAsset = {
  id: 1,
  path: 'upload/article/owner/20261023/0123456789abcdef0123456789abcdef.png',
  originalName: '星光.png',
}
const imageFile = { type: 'image/png', size: 100, name: '星光.png' }
test('scheduled publication saves a draft first and retains its ID if arranging the schedule fails', async () => {
  const h = harness({
    schedule: async () => {
      throw new Error('offline')
    },
  })
  try {
    h.fill()
    h.view.scheduleTime.value = '2099-01-01T10:30'
    await h.view.schedulePublication()
    await settle()
    assert.equal(h.view.id.value, 12)
    assert.match(h.view.notice.value, /草稿已保存.*定时安排未完成/)
    h.state.schedule = async () => ({ data: '2099-01-01 10:30:00' })
    await h.view.schedulePublication()
    await settle()
    assert.deepEqual(
      h.state.calls.map((c) => c[0]),
      ['create', 'schedule', 'update', 'schedule'],
    )
    assert.equal(h.state.calls[1][2], '2099-01-01T10:30:00+08:00')
    assert.equal(h.view.scheduledPublishAt.value, '2099-01-01 10:30:00')
  } finally {
    h.dispose()
  }
})
test('invalid times and ordinary authors cannot schedule publication', async () => {
  const h = harness({ roles: ['USER'] })
  try {
    h.fill()
    h.view.scheduleTime.value = '2099-01-01T10:30'
    await h.view.schedulePublication()
    h.state.roles = ['ADMIN']
    h.view.scheduleTime.value = '2020-01-01T10:30'
    await h.view.schedulePublication()
    assert.equal(h.state.calls.length, 0)
  } finally {
    h.dispose()
  }
})
test('canceling a schedule preserves unsaved body edits and failed cancellations retain the plan', async () => {
  const h = harness({
    id: 7,
    article: { scheduledPublishAt: '2099-01-01 10:30:00' },
    cancelSchedule: async () => {
      throw new Error('offline')
    },
  })
  try {
    await settle()
    h.view.form.content = 'Unsaved text'
    await h.view.cancelPublication()
    assert.equal(h.view.scheduledPublishAt.value, '2099-01-01 10:30:00')
    h.state.cancelSchedule = async () => ({ data: true })
    await h.view.cancelPublication()
    assert.equal(h.view.scheduledPublishAt.value, null)
    assert.equal(h.view.form.content, 'Unsaved text')
    assert.equal(h.view.dirty.value, true)
  } finally {
    h.dispose()
  }
})
test('publication status refresh does not overwrite unsaved body or undo history', async () => {
  const h = harness({ id: 7, article: { scheduledPublishAt: '2099-01-01 10:30:00' } })
  try {
    await settle()
    attachBody(h, '原文')
    await h.view.insertMarkdown('**', '**')
    await settle()
    h.state.load = async () => ({ data: { status: 1, scheduledPublishAt: null } })
    await h.view.refreshPublication()
    assert.equal(h.view.status.value, 1)
    assert.equal(h.view.form.content, '原文**文字**')
    assert.equal(h.view.canUndoBody.value, true)
  } finally {
    h.dispose()
  }
})
test('a delayed schedule response from an old session cannot change publication state', async () => {
  const wait = deferred(),
    h = harness({ schedule: () => wait.promise })
  try {
    h.fill()
    h.view.scheduleTime.value = '2099-01-01T10:30'
    const task = h.view.schedulePublication()
    await settle()
    h.state.token = 'other'
    wait.resolve({ data: '2099-01-01 10:30:00' })
    await task
    assert.equal(h.view.scheduledPublishAt.value, null)
    assert.equal(h.state.routes.length, 0)
  } finally {
    h.dispose()
  }
})
test('image upload inserts Markdown at the captured cursor and blocks save while uploading', async () => {
  const pending = deferred()
  const h = harness({ image: () => pending.promise })
  try {
    h.fill()
    h.view.form.content = 'before after'
    h.view.textarea.value = {
      selectionStart: 6,
      selectionEnd: 6,
      focus() {},
      setSelectionRange() {},
    }
    h.view.captureImagePosition()
    const uploading = h.view.uploadImages([imageFile])
    await h.view.save()
    assert.equal(await h.hooks.leave(), false)
    assert.deepEqual(
      h.state.calls.map((call) => call[0]),
      ['image'],
    )
    pending.resolve({ data: mediaAsset })
    await uploading
    assert.equal(h.view.form.content, `before\n\n![星光](/${mediaAsset.path})\n\n after`)
    assert.equal(h.view.uploadingImages.value, false)
  } finally {
    h.dispose()
  }
})
test('failed image uploads preserve the original text', async () => {
  const h = harness({
    image: async () => {
      throw new Error('network')
    },
  })
  try {
    h.view.form.content = 'original text'
    h.view.captureImagePosition()
    await h.view.uploadImages([imageFile])
    assert.equal(h.view.form.content, 'original text')
    assert.equal(h.view.uploadingImages.value, false)
  } finally {
    h.dispose()
  }
})
for (const reason of ['account', 'article', 'unmount']) {
  test(`image upload completion is ignored after ${reason} changes`, async () => {
    const pending = deferred()
    const h = harness({ image: () => pending.promise })
    try {
      h.view.form.content = 'original'
      const uploading = h.view.uploadImages([imageFile])
      if (reason === 'account') h.state.token = 'other'
      if (reason === 'article') {
        h.route.params.id = '99'
        await settle()
      }
      if (reason === 'unmount') h.dispose()
      const content = h.view.form.content
      pending.resolve({ data: mediaAsset })
      await uploading
      assert.equal(h.view.form.content, content)
      if (reason === 'article') assert.equal(h.view.uploadingImages.value, false)
    } finally {
      h.dispose()
    }
  })
}
test('invalid or oversized image batches do not start any uploads', async () => {
  const h = harness()
  try {
    await h.view.uploadImages([{ ...imageFile, type: 'image/svg+xml' }])
    await h.view.uploadImages([{ ...imageFile, size: 10 * 1024 * 1024 + 1 }])
    await h.view.uploadImages(Array(6).fill(imageFile))
    assert.equal(h.state.calls.length, 0)
  } finally {
    h.dispose()
  }
})
