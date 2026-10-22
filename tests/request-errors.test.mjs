import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'
import { belongsToCurrentSession } from '../src/utils/auth-session.ts'
import { getHttpErrorMessage, getRetryAfterSeconds } from '../src/utils/http-error.ts'

function loadTs(relative, dependencies) {
  const source = readFileSync(new URL(relative, import.meta.url), 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
  })
  const module = { exports: {} }
  vm.runInNewContext(outputText, {
    module,
    exports: module.exports,
    require(name) {
      assert.ok(name in dependencies, `Unexpected dependency: ${name}`)
      return dependencies[name]
    },
  })
  return module.exports
}

function client() {
  const state = { token: 'current-session', clears: 0, messages: [], routes: [] }
  let success
  let failure
  const service = {
    interceptors: {
      request: { use() {} },
      response: {
        use(onSuccess, onFailure) {
          success = onSuccess
          failure = onFailure
        },
      },
    },
  }
  loadTs('../src/utils/request.ts', {
    axios: { create: () => service },
    'element-plus': { ElMessage: { error: (message) => state.messages.push(message) } },
    '@/router': { replace: async (path) => state.routes.push(path) },
    '../stores/auth.ts': {
      useAuthStore: () => ({
        getToken: () => state.token,
        clearAuth: () => {
          state.clears++
          state.token = ''
        },
      }),
    },
    './auth-session': { belongsToCurrentSession },
    './http-error': { getHttpErrorMessage },
  })
  return { state, success, failure }
}

function error(status, message, config = {}) {
  return {
    config: { headers: { Authorization: 'Bearer current-session' }, ...config },
    response: { status, data: { code: status, message } },
  }
}

test('the login API opts out of expiring an existing session on credential failure', () => {
  let observed
  const api = loadTs('../src/api/auth.ts', {
    '@/utils/request': { post: (...args) => (observed = args) },
  })
  api.loginApi({ account: 'fixture', password: 'not-a-real-password' })
  assert.equal(observed[0], '/auth/login')
  assert.equal(observed[2].skipAuthRedirect, true)
})

test('a failed login displays its public message without clearing or redirecting the active session', async () => {
  const { state, failure } = client()
  await assert.rejects(failure(error(401, '密码错误', { skipAuthRedirect: true })))
  assert.deepEqual(state.messages, ['密码错误'])
  assert.equal(state.token, 'current-session')
  assert.equal(state.clears, 0)
  assert.deepEqual(state.routes, [])
})

test('an expired protected request clears only the matching session', async () => {
  const { state, failure } = client()
  await assert.rejects(failure(error(401, 'ignored')))
  assert.equal(state.clears, 1)
  assert.equal(state.token, '')
  assert.deepEqual(state.routes, ['/login'])
})

test('a late unauthorized response cannot clear a newer session or show a misleading expiry', async () => {
  const { state, failure } = client()
  await assert.rejects(
    failure(error(401, 'expired', { headers: { Authorization: 'Bearer old-session' } })),
  )
  assert.equal(state.clears, 0)
  assert.equal(state.token, 'current-session')
  assert.deepEqual(state.routes, [])
  assert.deepEqual(state.messages, [])
})

for (const [status, message] of [
  [400, '旧密码错误'],
  [403, '权限不足'],
  [404, '文章不存在'],
  [409, '该邮箱已注册，请直接登录'],
  [413, '上传文件过大'],
  [429, '操作过于频繁，请稍后重试'],
  [503, '注册服务暂时不可用，请稍后重试'],
]) {
  test(`HTTP ${status} shows the public API message and keeps authentication`, async () => {
    const { state, failure } = client()
    await assert.rejects(failure(error(status, message)))
    assert.deepEqual(state.messages, [message])
    assert.equal(state.token, 'current-session')
    assert.equal(state.clears, 0)
  })
}

test('unexpected server failures and network failures have safe useful messages', async () => {
  const { state, failure } = client()
  await assert.rejects(failure(error(500, 'SELECT password FROM private_table')))
  await assert.rejects(failure({ config: {} }))
  assert.deepEqual(state.messages, ['服务器异常，请稍后重试', '连接失败，请检查网络后重试'])
  assert.equal(state.clears, 0)
})

test('legacy HTTP 200 business failures still reject and show feedback', async () => {
  const { state, success } = client()
  await assert.rejects(
    success({ data: { code: 40010, message: '验证码错误或已失效' }, config: {} }),
  )
  assert.deepEqual(state.messages, ['验证码错误或已失效'])
  assert.equal(state.clears, 0)
})

test('malformed error payloads use fallback messages', () => {
  for (const payload of [undefined, '<html>error</html>', { message: {} }, { message: ' ' }]) {
    assert.equal(getHttpErrorMessage(403, payload), '权限不足')
    assert.equal(getHttpErrorMessage(503, payload), '服务暂时不可用，请稍后重试')
  }
})

test('retry delays from rate-limit responses are bounded and ignore invalid values', () => {
  assert.equal(getRetryAfterSeconds('60'), 60)
  assert.equal(getRetryAfterSeconds(0.5), 1)
  assert.equal(getRetryAfterSeconds('99999999'), 86400)
  for (const value of [undefined, null, -1, 0, 'NaN', {}, 'Wed, bad date']) {
    assert.equal(getRetryAfterSeconds(value), 0)
  }
  assert.equal(getHttpErrorMessage(429, undefined), '操作过于频繁，请稍后重试')
})

test('password reset sends the explicit target and proof without changing the session contract', () => {
  let observed
  const api = loadTs('../src/api/auth.ts', {
    '@/utils/request': { post: (...args) => (observed = args) },
  })
  const dto = {
    operatorPassword: 'operator-fixture',
    newPassword: 'new-fixture',
    confirmPassword: 'new-fixture',
  }
  api.resetUserPasswordApi(42, dto)
  assert.equal(observed[0], '/auth/reset-password/42')
  assert.equal(observed[1], dto)
})
