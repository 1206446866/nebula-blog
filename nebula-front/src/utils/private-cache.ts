/** 编辑备份只清理项目自己的键，不影响同域下的其他数据。 */
export const WRITING_CACHE_PREFIX = 'nebula-writing-v1:'
const MAX_AGE = 30 * 86400000
type CacheStorage = Pick<Storage, 'length' | 'key' | 'getItem' | 'removeItem'>

function writingKeys(storage: CacheStorage, userId?: number): string[] {
  const prefix = userId === undefined ? WRITING_CACHE_PREFIX : `${WRITING_CACHE_PREFIX}${userId}:`
  const keys: string[] = []
  for (let i = 0; i < storage.length; i++) {
    const key = storage.key(i)
    if (key?.startsWith(prefix)) keys.push(key)
  }
  return keys
}

/** 启动时扫描所有账号和文章的过期备份，损坏的项目备份也一并清除。 */
export function pruneWritingCache(storage: CacheStorage, now = Date.now()) {
  try {
    for (const key of writingKeys(storage)) {
      let time: unknown
      try {
        time = JSON.parse(storage.getItem(key) || 'null')?.time
      } catch {
        /* 损坏的备份没有可恢复的保存时间。 */
      }
      if (
        typeof time !== 'number' ||
        !Number.isFinite(time) ||
        time > now + 60000 ||
        now - time >= MAX_AGE
      )
        storage.removeItem(key)
    }
  } catch {
    /* 浏览器禁止存储时，仍允许使用内存会话和云端备份。 */
  }
}

/** 只清理离开账号的备份，其他账号的数据不受影响。 */
export function clearWritingCache(storage: CacheStorage, userId: number | undefined) {
  if (!Number.isSafeInteger(userId) || !userId || userId < 1) return
  try {
    writingKeys(storage, userId).forEach((key) => storage.removeItem(key))
  } catch {
    /* 禁用存储时不阻止服务端会话撤销。 */
  }
}

export function hasWritingCache(storage: CacheStorage, userId: number | undefined) {
  if (!userId) return false
  try {
    return writingKeys(storage, userId).length > 0
  } catch {
    return false
  }
}

/** 每次备份前核对共享存储，其他标签页尚未收到退出事件时也不能继续写入。 */
export function storedSessionMatches(storage: Pick<Storage, 'getItem'>, token: string) {
  try {
    return !!token && storage.getItem('token') === token
  } catch {
    // 禁用本机存储时仍允许使用云端；本机写入会自行报告不可用。
    return true
  }
}
