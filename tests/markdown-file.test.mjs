import assert from 'node:assert/strict'
import { test } from 'node:test'
import { exportMarkdown, importMarkdown, markdownFilename } from '../src/utils/markdown-file.ts'
import {
  validWritingDraft,
  readWritingBackups,
  writingPrefix,
} from '../src/views/home/article/write/logic/writing-cache.ts'

test('Markdown导出再导入保留标题正文和原始代码', () => {
  const content = '## 标题\n\n```html\n<script>alert(1)</script>\n```\n'
  assert.deepEqual(importMarkdown(exportMarkdown('原始文章', content), 'a.md'), {
    title: '原始文章',
    content,
  })
  assert.deepEqual(importMarkdown('\uFEFF# 中文标题\r\n\r\n正文', 'a.md'), {
    title: '中文标题',
    content: '正文',
  })
  assert.deepEqual(importMarkdown('普通正文', '本地笔记.markdown'), {
    title: '本地笔记',
    content: '普通正文',
  })
})
test('Markdown导入拒绝超量和二进制内容，名称无法穿越目录', () => {
  assert.throws(() => importMarkdown('字'.repeat(200001), 'a.md'))
  assert.throws(() => importMarkdown('正文\0二进制', 'a.md'))
  assert.equal(markdownFilename('../a\\b\r\n:*?'), '.._a_b_____.md')
  assert.equal(markdownFilename('CON'), 'nebula-CON.md')
  assert.equal(markdownFilename('文章', 'exe'), '文章.md')
})
test('本机编辑备份按账号和文章隔离，损坏和过期内容不可恢复', () => {
  const map = new Map(),
    storage = {
      get length() {
        return map.size
      },
      key: (i) => [...map.keys()][i],
      getItem: (key) => map.get(key) ?? null,
      removeItem: (key) => map.delete(key),
    }
  const draft = { title: '草稿', content: '正文', tagIds: [], sourceVersion: 0 },
    now = Date.now()
  const prefix = writingPrefix(1, 'article-12')
  map.set(prefix + 'a', JSON.stringify({ time: now - 20, draft }))
  map.set(
    prefix + 'b',
    JSON.stringify({ time: now - 10, draft: { ...draft, title: '另一标签页' } }),
  )
  map.set(
    writingPrefix(2, 'article-12') + 'a',
    JSON.stringify({ time: now, draft: { ...draft, title: '别人' } }),
  )
  map.set(prefix + 'old', JSON.stringify({ time: now - 31 * 86400000, draft }))
  map.set(prefix + 'bad', 'broken')
  assert.equal(readWritingBackups(storage, prefix, now).length, 2)
  assert.equal(readWritingBackups(storage, prefix, now)[0].draft.title, '另一标签页')
  assert.equal(map.has(prefix + 'old'), false)
  assert.equal(validWritingDraft({ ...draft, tagIds: [-1] }), false)
  assert.equal(validWritingDraft({ ...draft, sourceVersion: -1 }), false)
})
