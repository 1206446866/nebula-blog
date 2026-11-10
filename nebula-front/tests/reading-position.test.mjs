import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  captureReadingPosition,
  restoreReadingPosition,
} from '../src/views/home/article/detail/logic/reading-position.ts'

function article(scroll = 0) {
  const headings = [
    { id: 'chapter-1', getBoundingClientRect: () => ({ top: 300 - scroll }) },
    { id: 'chapter-2', getBoundingClientRect: () => ({ top: 1300 - scroll }) },
  ]
  return {
    getBoundingClientRect: () => ({ top: 200 - scroll, bottom: 2400 - scroll, height: 2200 }),
    querySelectorAll: () => headings,
  }
}
test('续读保留长章节内的位置，不退回章节开头', () => {
  const position = captureReadingPosition(article(800), 800, 720)
  const restored = restoreReadingPosition(article(), position.progress, position.anchor, 0, 720)
  assert.ok(Math.abs(restored - 800) < 1)
})
test('删除章节或损坏记录时按全文比例恢复，旧章节标识可兼容', () => {
  assert.equal(restoreReadingPosition(article(), 5000, 'chapter-2', 0, 720), 1188)
  const expected = 88 + 1640 / 2
  assert.equal(restoreReadingPosition(article(), 5000, 'removed', 0, 720), expected)
  assert.equal(
    restoreReadingPosition(article(), 5000, '{"id":"chapter-1","offset":-1}', 0, 720),
    expected,
  )
  assert.equal(restoreReadingPosition(article(), 5000, '{bad', 0, 720), expected)
})
