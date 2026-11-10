import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  pruneWritingCache,
  clearWritingCache,
  storedSessionMatches,
} from '../src/utils/private-cache.ts'
import { readAuthCache, persistAuth } from '../src/utils/session-cache.ts'
import { registerSessionWriter, flushSessionWriters } from '../src/utils/session-exit.ts'

function storage(initial = {}) {
  const values = new Map(Object.entries(initial))
  return {
    values,
    get length() {
      return values.size
    },
    key: (index) => [...values.keys()][index] ?? null,
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  }
}
const key = (user, document = 'new') => `nebula-writing-v1:${user}:${document}:tab`
const token = (id, exp = Date.now() / 1000 + 3600) =>
  `header.${Buffer.from(JSON.stringify({ sub: String(id), exp })).toString('base64url')}.signature`
const backup = (time = Date.now()) => JSON.stringify({ time, draft: { title: '本机私人内容' } })

test('启动时清理所有账号和文章的过期或损坏备份，仅保留有效项目备份', () => {
  const now = Date.now()
  const cache = storage({
    [key(1)]: backup(now - 31 * 86400000),
    [key(2, 'article-9')]: backup(now - 30 * 86400000),
    [key(3)]: '{损坏',
    [key(4)]: backup(now + 120000),
    [key(5)]: backup(now),
    preference: '保留其他设置',
  })
  pruneWritingCache(cache, now)
  assert.deepEqual([...cache.values.keys()].sort(), [key(5), 'preference'].sort())
})

test('清理账号1不影响账号11或其他账号，也不会删除同域其他设置', () => {
  const cache = storage({
    [key(1)]: backup(),
    [key(1, 'article-8')]: backup(),
    [key(11)]: backup(),
    theme: 'dark',
  })
  clearWritingCache(cache, 1)
  assert.deepEqual([...cache.values.keys()].sort(), [key(11), 'theme'].sort())
})

test('过期Token及其用户权限缓存被物理移除，未同步稿件保留供重新登录恢复', () => {
  const cache = storage({
    token: token(1, 1),
    user: JSON.stringify({ id: 1, username: '本人' }),
    roles: '["USER"]',
    permissions: '[]',
    [key(1)]: backup(),
    [key(2)]: backup(),
  })
  assert.equal(readAuthCache(cache).token, '')
  for (const item of ['token', 'user', 'roles', 'permissions'])
    assert.equal(cache.getItem(item), null)
  assert.ok(cache.getItem(key(2)))
  assert.ok(cache.getItem(key(1)))
})

test('损坏Token或身份不匹配时清除登录状态，不静默删除未同步稿件', () => {
  for (const value of ['损坏Token', token(2)]) {
    const cache = storage({
      token: value,
      user: JSON.stringify({ id: 1, username: '错误身份' }),
      roles: '["ADMIN"]',
      permissions: '[]',
      [key(1)]: backup(),
      [key(2)]: backup(),
    })
    assert.equal(readAuthCache(cache).token, '')
    assert.equal(cache.getItem('token'), null)
    assert.ok(cache.getItem(key(1)))
    assert.ok(cache.getItem(key(2)))
  }
})

test('重新打开浏览器且会话仍有效时保留备份，便于恢复意外关闭的编辑', () => {
  const jwt = token(1)
  const cache = storage({
    token: jwt,
    user: JSON.stringify({ id: 1, username: '本人' }),
    roles: '["USER"]',
    permissions: '[]',
    [key(1)]: backup(),
  })
  assert.equal(readAuthCache(cache).token, jwt)
  assert.ok(cache.getItem(key(1)))
})

test('发布新会话时Token最后写入，退出时Token最先移除', () => {
  const calls = []
  const cache = { setItem: (key) => calls.push(key), removeItem: (key) => calls.push(key) }
  persistAuth(cache, { token: token(1), user: { id: 1 }, roles: [], permissions: [] })
  assert.equal(calls.at(-1), 'token')
  calls.length = 0
  persistAuth(cache, { token: '', user: null, roles: [], permissions: [] })
  assert.equal(calls[0], 'token')
})

test('其他标签页移除或替换Token后，旧编辑页在收到通知前即不能继续备份', () => {
  const cache = storage({ token: '旧会话' })
  assert.equal(storedSessionMatches(cache, '旧会话'), true)
  cache.removeItem('token')
  assert.equal(storedSessionMatches(cache, '旧会话'), false)
  cache.setItem('token', '新会话')
  assert.equal(storedSessionMatches(cache, '旧会话'), false)
})

test('退出同步只处理当前会话，失败不会阻止其他编辑器处理，离页后取消注册', async () => {
  const calls = []
  const removeFirst = registerSessionWriter('当前', async () => {
    calls.push('当前')
    throw new Error('离线')
  })
  const removeSecond = registerSessionWriter('其他', async () => {
    calls.push('其他')
  })
  try {
    await flushSessionWriters('当前')
    assert.deepEqual(calls, ['当前'])
    removeFirst()
    await flushSessionWriters('当前')
    assert.deepEqual(calls, ['当前'])
  } finally {
    removeFirst()
    removeSecond()
  }
})

test('跨标签页只读同步不会清除另一个页面正在写入的新会话', () => {
  const current = token(2)
  const cache = storage({
    user: JSON.stringify({ id: 2, username: '新账号' }),
    roles: '["USER"]',
    permissions: '[]',
  })
  assert.equal(readAuthCache(cache, false).token, '')
  assert.ok(cache.getItem('user'))
  cache.setItem('token', current)
  assert.equal(readAuthCache(cache, false).token, current)
  cache.setItem('user', '{正在更新')
  assert.equal(readAuthCache(cache, false).token, '')
  assert.equal(cache.getItem('token'), current)
})
