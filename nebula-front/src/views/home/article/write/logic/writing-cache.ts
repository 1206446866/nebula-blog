import type { WritingDraft } from '@/api/article-workspace'
import { WRITING_CACHE_PREFIX } from '../../../../../utils/private-cache.ts'

export interface LocalWritingBackup {
  slot: string
  time: number
  draft: WritingDraft
}
const MAX_AGE = 30 * 86400000

/** 校验浏览器缓存，损坏或旧结构不能覆盖当前编辑内容。 */
export function validWritingDraft(value: unknown): value is WritingDraft {
  if (!value || typeof value !== 'object') return false
  const draft = value as WritingDraft
  return (
    typeof draft.title === 'string' &&
    draft.title.length <= 255 &&
    typeof draft.content === 'string' &&
    draft.content.length <= 200000 &&
    (draft.categoryId == null ||
      (Number.isSafeInteger(draft.categoryId) && draft.categoryId > 0)) &&
    Array.isArray(draft.tagIds) &&
    draft.tagIds.length <= 100 &&
    draft.tagIds.every((id) => Number.isSafeInteger(id) && id > 0) &&
    Number.isSafeInteger(draft.sourceVersion) &&
    draft.sourceVersion >= 0
  )
}

export function writingPrefix(userId: number, document: string) {
  return `${WRITING_CACHE_PREFIX}${userId}:${document}:`
}

/** 每个编辑页面使用独立槽位，避免多个标签页互相覆盖本地备份。 */
export function readWritingBackups(
  storage: Storage,
  prefix: string,
  now = Date.now(),
): LocalWritingBackup[] {
  const values: LocalWritingBackup[] = []
  const expired: string[] = []
  for (let i = 0; i < storage.length; i++) {
    const slot = storage.key(i)
    if (!slot?.startsWith(prefix)) continue
    try {
      const value = JSON.parse(storage.getItem(slot) || 'null')
      if (value && Number.isFinite(value.time) && now - value.time >= MAX_AGE) expired.push(slot)
      if (
        value &&
        Number.isFinite(value.time) &&
        value.time <= now + 60000 &&
        now - value.time < MAX_AGE &&
        validWritingDraft(value.draft)
      )
        values.push({ ...value, slot })
    } catch {
      /* 忽略损坏的缓存，保留当前正文。 */
    }
  }
  expired.forEach((slot) => storage.removeItem(slot))
  return values.sort((a, b) => b.time - a.time)
}

/** 比较正文和目录，不把保存时间及基线版本当成内容差异。 */
export function writingContent(draft: WritingDraft) {
  return JSON.stringify([
    draft.title,
    draft.content,
    draft.categoryId ?? null,
    [...draft.tagIds].sort((a, b) => a - b),
  ])
}
