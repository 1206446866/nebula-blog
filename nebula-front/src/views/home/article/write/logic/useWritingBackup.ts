import {
  getWritingBackupApi,
  saveWritingBackupApi,
  type WritingDraft,
} from '@/api/article-workspace'
import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import { storedSessionMatches } from '@/utils/private-cache'
import { registerSessionWriter } from '@/utils/session-exit'
import {
  readWritingBackups,
  writingContent,
  writingPrefix,
  type LocalWritingBackup,
} from './writing-cache'

/** 本地备份即时兜底，云端备份使用版本检查；两者均不更新正式文章。 */
export function useWritingBackup(context: {
  userId: number | undefined
  sessionToken: string
  id: Ref<number | undefined>
  version: Ref<number>
  ready: Ref<boolean>
  dirty: Ref<boolean>
  busy: Ref<boolean>
  current: () => boolean
  draft: () => WritingDraft
  restore: (draft: WritingDraft) => Promise<void>
}) {
  const backupState = ref('正在检查编辑备份…')
  const recovery = ref<{ draft: WritingDraft; label: string; slot?: string }>()
  const revision = ref(0)
  const initialized = ref(false)
  const conflict = ref(false)
  const localSaved = ref(false)
  const document = computed(() => (context.id.value ? `article-${context.id.value}` : 'new'))
  const client = crypto.randomUUID()
  let activeKey = '',
    activeSlot = '',
    disposed = false,
    generation = 0
  let localTimer: ReturnType<typeof setTimeout> | undefined
  let cloudTimer: ReturnType<typeof setTimeout> | undefined
  let flight: Promise<void> | undefined
  let lastCloud = ''
  let recoveredSlot: string | undefined
  let controller: AbortController | undefined
  const current = (run = generation) =>
    !disposed &&
    run === generation &&
    context.current() &&
    storedSessionMatches(localStorage, context.sessionToken)
  const canWrite = () => current() && context.ready.value && context.dirty.value && !!activeSlot

  function cache() {
    if (!canWrite()) return
    try {
      const draft = context.draft()
      localStorage.setItem(activeSlot, JSON.stringify({ time: Date.now(), draft }))
      if (!current()) {
        localStorage.removeItem(activeSlot)
        return
      }
      localSaved.value = true
      if (!initialized.value) backupState.value = '已保存在本机；云端暂未连接'
    } catch {
      localSaved.value = false
      backupState.value = '本机备份不可用，请及时手动保存'
    }
  }

  async function open() {
    if (!context.ready.value || !context.userId || !current()) return
    const run = ++generation
    controller?.abort()
    controller = new AbortController()
    initialized.value = false
    conflict.value = false
    recovery.value = undefined
    recoveredSlot = undefined
    localSaved.value = false
    activeKey = document.value
    activeSlot = writingPrefix(context.userId, activeKey) + client
    lastCloud = ''
    const same = writingContent(context.draft())
    let local: LocalWritingBackup | undefined
    try {
      local = readWritingBackups(localStorage, writingPrefix(context.userId, activeKey)).find(
        (item) => writingContent(item.draft) !== same,
      )
      if (local)
        recovery.value = { draft: local.draft, label: '发现本机未完成的编辑', slot: local.slot }
    } catch {
      /* 禁用浏览器存储时，仍可尝试云端备份。 */
    }
    try {
      const result = await getWritingBackupApi(activeKey, controller.signal)
      if (!current(run)) return
      revision.value = result.data.revision
      initialized.value = true
      const remote = result.data.draft
      if (remote) {
        lastCloud = writingContent(remote)
        const cloudTime = Date.parse((result.data.updatedAt || '').replace(' ', 'T') + '+08:00')
        if (lastCloud !== same && (!local || cloudTime > local.time))
          recovery.value = { draft: remote, label: '发现云端未完成的编辑' }
      }
      backupState.value = recovery.value ? '请选择是否恢复备份' : '编辑时自动备份，不改变线上文章'
    } catch {
      if (current(run)) backupState.value = '云端暂未连接，本机备份仍可使用'
    }
  }

  async function sync() {
    cache()
    if (
      !canWrite() ||
      !initialized.value ||
      conflict.value ||
      recovery.value ||
      context.busy.value ||
      flight
    )
      return
    const draft = context.draft(),
      key = activeKey,
      run = generation
    if (writingContent(draft) === lastCloud) return
    backupState.value = '正在备份到云端…'
    flight = (async () => {
      try {
        const result = await saveWritingBackupApi(key, revision.value, draft)
        if (!current(run)) return
        revision.value = result.data.revision
        lastCloud = writingContent(draft)
        backupState.value = '已自动备份到云端（私人）'
      } catch (error) {
        if (!current(run)) return
        const status = (error as { response?: { status?: number } }).response?.status
        conflict.value = status === 409
        backupState.value = conflict.value
          ? '另一处已修改云端备份，已暂停同步；本机内容保留'
          : '云端备份失败，稍后重试；请留意本机备份状态'
      }
    })().finally(() => {
      flight = undefined
    })
    await flight
  }

  async function recover() {
    const run = generation
    const candidate = recovery.value
    if (!candidate || !context.ready.value || context.busy.value || !current()) return
    await context.restore(candidate.draft)
    if (!current(run)) return
    recoveredSlot = candidate.slot
    recovery.value = undefined
    cache()
    backupState.value =
      candidate.draft.sourceVersion !== context.version.value
        ? '已恢复旧版本上的编辑，请对照当前文章后手动保存'
        : '已恢复编辑，手动保存后才更新正式草稿或文章'
    schedule()
  }

  async function discard() {
    if (!context.ready.value || context.busy.value || !current()) return
    // 仅丢弃明确选中的本机槽位，其他标签页的缓存继续保留。
    const selected = recovery.value?.slot
    try {
      if (selected) localStorage.removeItem(selected)
    } catch {
      /* 浏览器存储可能禁用。 */
    }
    recovery.value = undefined
    if (!initialized.value || conflict.value) return
    const run = generation
    try {
      const result = await saveWritingBackupApi(activeKey, revision.value, null)
      if (current(run)) {
        revision.value = result.data.revision
        lastCloud = ''
        backupState.value = '已放弃选中的恢复备份'
      }
    } catch {
      if (current(run)) {
        conflict.value = true
        backupState.value = '备份状态已变化，请重新检查后选择'
      }
    }
  }

  function schedule() {
    clearTimeout(localTimer)
    clearTimeout(cloudTimer)
    if (!canWrite()) return
    localTimer = setTimeout(cache, 600)
    cloudTimer = setTimeout(() => void sync(), 5000)
  }

  async function beforeSave() {
    cache()
    await flight
  }

  async function afterSave() {
    const run = generation,
      key = activeKey
    clearTimeout(localTimer)
    clearTimeout(cloudTimer)
    try {
      localStorage.removeItem(activeSlot)
      if (recoveredSlot) localStorage.removeItem(recoveredSlot)
    } catch {
      /* 手动保存已经成功，缓存清理失败不能改变保存结果。 */
    }
    if (initialized.value && !conflict.value) {
      try {
        const result = await saveWritingBackupApi(key, revision.value, null)
        if (current(run)) {
          revision.value = result.data.revision
          lastCloud = ''
          recovery.value = undefined
          backupState.value = '正式内容已保存，自动备份已清除'
        }
      } catch {
        if (current(run)) backupState.value = '正式内容已保存，另一处备份仍保留'
      }
    }
  }

  const stopOpen = watch(
    [context.ready, document],
    ([ready]) => {
      if (ready) void open()
    },
    { immediate: true },
  )
  const stopContent = watch(() => writingContent(context.draft()), schedule)
  const retryTimer = setInterval(() => {
    if (!context.ready.value || !context.current()) return
    if (!initialized.value) void open()
    else void sync()
  }, 20000)
  window.addEventListener('pagehide', cache)
  const unregisterWriter = registerSessionWriter(context.sessionToken, sync)
  onBeforeUnmount(() => {
    cache()
    disposed = true
    unregisterWriter()
    generation++
    controller?.abort()
    stopOpen()
    stopContent()
    clearInterval(retryTimer)
    clearTimeout(localTimer)
    clearTimeout(cloudTimer)
    window.removeEventListener('pagehide', cache)
  })
  return {
    backupState,
    recovery,
    conflict,
    localSaved,
    recover,
    discard,
    recheck: open,
    backupNow: sync,
    beforeSave,
    afterSave,
    cache,
  }
}
