import assert from 'node:assert/strict'
import { test } from 'node:test'
import { revokeCurrentSession, belongsToCurrentSession } from '../src/utils/auth-session.ts'

function session(revoke) {
  const state = { token: 'old-session', cleared: 0 }
  const logout = () =>
    revokeCurrentSession({
      token: state.token,
      revoke,
      currentToken: () => state.token,
      clear: () => {
        state.token = ''
        state.cleared++
      },
    })
  return { state, logout }
}

test('logout waits for server confirmation before clearing the captured session', async () => {
  let confirm
  const { state, logout } = session((token) => {
    assert.equal(token, 'old-session')
    return new Promise((resolve) => {
      confirm = resolve
    })
  })
  const pending = logout()
  assert.equal(state.token, 'old-session')
  assert.equal(state.cleared, 0)
  confirm()
  assert.equal(await pending, true)
  assert.equal(state.token, '')
  assert.equal(state.cleared, 1)
})

for (const failure of [{ response: { status: 401 } }, { code: 401 }]) {
  test(`an already invalid session may clear locally (${JSON.stringify(failure)})`, async () => {
    const { state, logout } = session(async () => {
      throw failure
    })
    assert.equal(await logout(), true)
    assert.equal(state.token, '')
  })
}

for (const failure of [new Error('offline'), { response: { status: 503 } }]) {
  test(`failed revocation retains the session for retry (${failure.message || '503'})`, async () => {
    const { state, logout } = session(async () => {
      throw failure
    })
    await assert.rejects(logout())
    assert.equal(state.token, 'old-session')
    assert.equal(state.cleared, 0)
  })
}

test('late logout completion cannot erase a newly established session', async () => {
  let confirm
  const { state, logout } = session(
    () =>
      new Promise((resolve) => {
        confirm = resolve
      }),
  )
  const pending = logout()
  state.token = 'new-session'
  confirm()
  assert.equal(await pending, false)
  assert.equal(state.token, 'new-session')
  assert.equal(state.cleared, 0)
})

test('logging out an empty session does not call the server', async () => {
  const { state, logout } = session(() => assert.fail('unexpected server request'))
  state.token = ''
  assert.equal(await logout(), true)
})

test('a stale 401 must not clear a different or newer session', () => {
  assert.equal(belongsToCurrentSession('Bearer old-session', 'new-session'), false)
  assert.equal(belongsToCurrentSession('Bearer new-session', 'new-session'), true)
  assert.equal(belongsToCurrentSession(undefined, 'new-session'), false)
  assert.equal(belongsToCurrentSession('Bearer ', ''), false)
})
