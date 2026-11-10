const TOP_OFFSET = 112
const clamp = (value: number) => Math.max(0, Math.min(10000, Math.round(value)))

/** 记录章节内的位置，避免续读时退回长章节的开头；旧章节标识仍可兼容。 */
export function captureReadingPosition(content: HTMLElement, scrollY: number, viewport: number) {
  const rect = content.getBoundingClientRect()
  const progress = clamp(
    ((scrollY - (rect.top + scrollY - TOP_OFFSET)) / Math.max(1, rect.height - viewport + 160)) *
      10000,
  )
  const headings = [...content.querySelectorAll<HTMLElement>('h1[id],h2[id]')]
  let index = -1
  for (let cursor = headings.length - 1; cursor >= 0; cursor--) {
    if (headings[cursor]!.getBoundingClientRect().top <= 140) {
      index = cursor
      break
    }
  }
  let anchor = ''
  if (index >= 0) {
    const heading = headings[index]!,
      start = heading.getBoundingClientRect().top + scrollY - TOP_OFFSET
    const end =
      (headings[index + 1]?.getBoundingClientRect().top ?? rect.bottom) + scrollY - TOP_OFFSET
    const value = JSON.stringify({
      id: heading.id,
      offset: clamp(((scrollY - start) / Math.max(1, end - start)) * 10000),
    })
    if (value.length <= 160) anchor = value
  }
  return { progress, anchor }
}

/** 章节不存在或数据损坏时退回全文比例，定位范围始终限制在当前文章。 */
export function restoreReadingPosition(
  content: HTMLElement,
  progress: number,
  anchor: string,
  scrollY: number,
  viewport: number,
) {
  const rect = content.getBoundingClientRect(),
    top = rect.top + scrollY - TOP_OFFSET
  const fallback = top + (Math.max(1, rect.height - viewport + 160) * clamp(progress)) / 10000
  let id = anchor,
    offset = 0
  if (anchor.startsWith('{')) {
    try {
      const value = JSON.parse(anchor)
      if (
        typeof value.id !== 'string' ||
        !Number.isInteger(value.offset) ||
        value.offset < 0 ||
        value.offset > 10000
      )
        return fallback
      id = value.id
      offset = value.offset
    } catch {
      return fallback
    }
  }
  const headings = [...content.querySelectorAll<HTMLElement>('h1[id],h2[id]')]
  const index = headings.findIndex((node) => node.id === id)
  if (index < 0) return fallback
  const start = headings[index]!.getBoundingClientRect().top + scrollY - TOP_OFFSET
  const end =
    (headings[index + 1]?.getBoundingClientRect().top ?? rect.bottom) + scrollY - TOP_OFFSET
  return Math.max(0, start + (Math.max(1, end - start) * offset) / 10000)
}
