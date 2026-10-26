import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createMediaQueue } from '../src/utils/media-queue.ts'

const tick = () => new Promise((resolve) => setImmediate(resolve))
test('a full media page runs at most two image requests at a time', async () => {
  const queue = createMediaQueue(2)
  let active = 0,
    peak = 0
  const results = await Promise.all(
    Array.from({ length: 24 }, (_, id) =>
      queue(async () => {
        active++
        peak = Math.max(peak, active)
        await tick()
        active--
        return id
      }),
    ),
  )
  assert.equal(peak, 2)
  assert.deepEqual(
    results,
    Array.from({ length: 24 }, (_, id) => id),
  )
})
test('aborting queued media removes it without sending a request', async () => {
  const queue = createMediaQueue(1)
  let release,
    called = false
  const first = queue(
    () =>
      new Promise((resolve) => {
        release = resolve
      }),
  )
  await tick()
  const controller = new AbortController()
  const pending = queue(async () => {
    called = true
  }, controller.signal)
  const rejected = assert.rejects(pending, { name: 'AbortError' })
  controller.abort()
  await rejected
  release()
  await first
  await queue(async () => 'recovered')
  assert.equal(called, false)
})
test('failed work releases its slot and the pending queue remains bounded', async () => {
  const queue = createMediaQueue(1, 1)
  let release
  const first = queue(
    () =>
      new Promise((resolve) => {
        release = resolve
      }),
  )
  await tick()
  const second = queue(async () => {
    throw new Error('fixture failure')
  })
  const rejected = assert.rejects(second, /fixture failure/)
  await assert.rejects(
    queue(async () => assert.fail('queue overflow ran')),
    /图片请求较多/,
  )
  release()
  await first
  await rejected
  assert.equal(await queue(async () => 'ok'), 'ok')
})
test('already cancelled work never starts', async () => {
  const controller = new AbortController()
  controller.abort()
  await assert.rejects(
    createMediaQueue()(async () => assert.fail('cancelled work ran'), controller.signal),
    { name: 'AbortError' },
  )
})
