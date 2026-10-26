import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { createMediaQueue } from '../src/utils/media-queue.ts'

test('media work queued in an old session cannot run under a new account', async () => {
  let session = 'first-account'
  const pending = [],
    calls = []
  const request = {
    get(url, config) {
      calls.push({ url, config })
      return new Promise((resolve) => pending.push(resolve))
    },
  }
  const module = { exports: {} }
  const code = ts.transpileModule(
    readFileSync(new URL('../src/api/media.ts', import.meta.url), 'utf8'),
    {
      compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
    },
  ).outputText
  const deps = {
    '@/utils/request': request,
    '@/utils/media-queue': { createMediaQueue },
    '@/stores/auth': { useAuthStore: () => ({ getToken: () => session }) },
    axios: { CanceledError: class CanceledError extends Error {} },
  }
  vm.runInNewContext(code, {
    module,
    exports: module.exports,
    FormData,
    require(name) {
      assert.ok(name in deps, name)
      return deps[name]
    },
  })
  const signal = new AbortController().signal
  const api = module.exports
  const first = api.mediaFileApi(1, signal),
    second = api.mediaFileApi(2, signal)
  const oldQueued = api.mediaFileApi(3, signal)
  const rejected = assert.rejects(oldQueued, /登录状态已变化/)
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(calls.length, 2)
  assert.equal(calls[0].config.headers.Authorization, 'Bearer first-account')
  session = 'second-account'
  pending.shift()(new Blob())
  await first
  await rejected
  pending.shift()(new Blob())
  await second
  assert.equal(calls.length, 2)
  const current = api.mediaFileApi(4, signal)
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(calls[2].config.headers.Authorization, 'Bearer second-account')
  pending.shift()(new Blob())
  await current
})
