import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { compileScript, parse } from '@vue/compiler-sfc'
import * as vue from 'vue'
import { clientFeatures } from '../src/constants/client-features.ts'

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}
function harness(file, props, api) {
  const auth = vue.reactive({
    token: 'session-one',
    getToken() {
      return this.token
    },
  })
  const events = [],
    unmount = []
  const source = readFileSync(
    new URL(`../src/components/community/${file}.vue`, import.meta.url),
    'utf8',
  )
  const code = ts.transpileModule(compileScript(parse(source).descriptor, { id: file }).content, {
    compilerOptions: { module: ts.ModuleKind.CommonJS },
  }).outputText
  const deps = {
    vue: {
      ...vue,
      onBeforeUnmount(fn) {
        unmount.push(fn)
      },
    },
    '@/api/community': api,
    '@/stores/auth': { useAuthStore: () => auth },
    '@/constants/client-features': { clientFeatures },
    '@/utils/resource': { avatarUrl: () => '' },
    './FollowButton.vue': {},
    './CommunityUserList.vue': {},
    '@/views/home/profile/ProfilePagination.vue': {},
  }
  const module = { exports: {} }
  vm.runInNewContext(code, {
    module,
    exports: module.exports,
    AbortController,
    require(name) {
      assert.ok(name in deps, name)
      return deps[name]
    },
  })
  const state = vue.reactive(props)
  const scope = vue.effectScope()
  const view = scope.run(() =>
    module.exports.default.setup(state, {
      expose() {},
      emit(...event) {
        events.push(event)
      },
    }),
  )
  return {
    auth,
    events,
    props: state,
    view,
    close() {
      unmount.forEach((fn) => fn())
      scope.stop()
    },
  }
}
const relationship = (extra = {}) => ({
  following: false,
  followedBy: false,
  self: false,
  canFollow: true,
  ...extra,
})
const page = (id) => ({ records: [{ id }], totalRow: 1, pageNumber: 1, pageSize: 12, totalPage: 1 })
const flush = async () => {
  await vue.nextTick()
  await Promise.resolve()
  await Promise.resolve()
}

test('double clicks submit one follow request and the server result drives the displayed state', async () => {
  const wait = deferred(),
    calls = []
  const h = harness(
    'FollowButton',
    { userId: 2, relationship: relationship() },
    {
      setFollowing(...args) {
        calls.push(args)
        return wait.promise
      },
    },
  )
  const first = h.view.toggle()
  await h.view.toggle()
  assert.equal(calls.length, 1)
  assert.deepEqual(calls[0], [2, true])
  wait.resolve({ data: relationship({ following: true }) })
  await first
  assert.equal(h.events[0][0], 'changed')
  assert.equal(h.events[0][1].following, true)
  h.close()
})

test('a late follow response cannot alter another profile or another login session', async () => {
  for (const change of ['user', 'session', 'unmount']) {
    const wait = deferred()
    const h = harness(
      'FollowButton',
      { userId: 2, relationship: relationship() },
      { setFollowing: () => wait.promise },
    )
    const task = h.view.toggle()
    if (change === 'user') h.props.userId = 3
    if (change === 'session') h.auth.token = 'session-two'
    if (change === 'unmount') h.close()
    await flush()
    wait.resolve({ data: relationship({ following: true }) })
    await task
    assert.equal(h.events.length, 0)
    if (change !== 'unmount') h.close()
  }
})

test('self and unavailable targets cannot be followed; existing disabled targets can still be unfollowed', async () => {
  const calls = []
  const h = harness(
    'FollowButton',
    { userId: 2, relationship: relationship({ self: true }) },
    {
      setFollowing(...args) {
        calls.push(args)
        return Promise.resolve({ data: relationship() })
      },
    },
  )
  await h.view.toggle()
  assert.equal(calls.length, 0)
  h.props.relationship = relationship({ canFollow: false })
  await h.view.toggle()
  assert.equal(calls.length, 0)
  h.props.relationship = relationship({ canFollow: false, following: true })
  await h.view.toggle()
  assert.deepEqual(calls[0], [2, false])
  h.close()
})

test('a failed follow releases the button and preserves the relationship for retry', async () => {
  const h = harness(
    'FollowButton',
    { userId: 2, relationship: relationship() },
    {
      setFollowing: () => Promise.reject(new Error('offline')),
    },
  )
  await h.view.toggle()
  assert.equal(h.view.busy.value, false)
  assert.equal(h.props.relationship.following, false)
  assert.equal(h.events.length, 0)
  h.close()
})

test('changing list mode aborts the previous request and ignores its late response', async () => {
  const calls = []
  const h = harness(
    'CommunityUserList',
    { mode: 'discover', userId: 1, keyword: '', refreshKey: 0 },
    {
      getCommunityUsers(...args) {
        const wait = deferred()
        calls.push({ args, ...wait })
        return wait.promise
      },
    },
  )
  h.props.mode = 'following'
  await flush()
  assert.equal(calls[0].args[4].aborted, true)
  calls[1].resolve({ data: page(20) })
  await flush()
  calls[0].resolve({ data: page(10) })
  await flush()
  assert.equal(h.view.result.value.records[0].id, 20)
  h.close()
})

test('a completed follow cancels stale profile counts and refreshes an expanded relationship list', async () => {
  const wait = deferred()
  const h = harness('ProfileConnections', { userId: 2 }, { getFollowSummary: () => wait.promise })
  h.view.changed({ ...relationship({ following: true }), followerCount: 4, followingCount: 2 })
  wait.resolve({ data: { ...relationship(), followerCount: 3, followingCount: 2 } })
  await flush()
  assert.equal(h.view.summary.value.following, true)
  assert.equal(h.view.summary.value.followerCount, 4)
  assert.equal(h.view.listRevision.value, 1)
  h.close()
})
