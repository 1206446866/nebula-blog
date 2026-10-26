import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createTextHistory } from '../src/utils/text-history.ts'

const state = (content, start = content.length, end = start) => ({
  content,
  selectionStart: start,
  selectionEnd: end,
})

test('continuous typing merges while commands and pauses create separate undo steps', () => {
  const history = createTextHistory()
  history.reset(state(''))
  history.record(state(''), state('a'), 'insertText', 100)
  history.record(state('a'), state('ab'), 'insertText', 200)
  history.record(state('ab'), state('abc'), 'insertText', 1000)
  history.record(state('abc'), state('**abc**', 2, 5))
  assert.equal(history.undo(state('**abc**')).content, 'abc')
  assert.equal(history.undo(state('abc')).content, 'ab')
  assert.equal(history.undo(state('ab')).content, '')
  assert.equal(history.undo(state('')), null)
})
test('cursor movement splits a typing group and backwards selections survive undo', () => {
  const history = createTextHistory()
  history.reset(state('a'))
  history.record(state('a'), state('ab'), 'insertText', 100)
  history.selection(state('ab', 0))
  history.record(state('ab', 0), state('cab', 1), 'insertText', 200)
  assert.equal(history.undo(state('cab')).content, 'ab')
  const before = { ...state('ab', 0, 2), selectionDirection: 'backward' }
  history.record(before, state('**ab**', 2, 4))
  assert.deepEqual(history.undo(state('**ab**')), before)
})
test('a new edit clears the undone branch while a no-op command preserves redo', () => {
  const history = createTextHistory()
  history.reset(state('a'))
  history.record(state('a'), state('ab'))
  history.undo(state('ab'))
  history.record(state('a'), state('a'))
  assert.equal(history.canRedo, true)
  history.record(state('a'), state('c'))
  assert.equal(history.canRedo, false)
  assert.equal(history.redo(state('c')), null)
})
test('history boundaries reset per document and enforce step and text memory limits', () => {
  for (const history of [createTextHistory(2), createTextHistory(100, 12)]) {
    history.reset(state('aaaa'))
    history.record(state('aaaa'), state('bbbb'))
    history.record(state('bbbb'), state('cccc'))
    history.record(state('cccc'), state('dddd'))
    history.undo(state('dddd'))
    history.undo(state('cccc'))
    assert.equal(history.canUndo, false)
    history.reset(state('other article'))
    assert.equal(history.canUndo, false)
    assert.equal(history.canRedo, false)
  }
})
