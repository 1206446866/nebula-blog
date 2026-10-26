const localImage =
  /^upload\/(avatar|article)\/[A-Za-z0-9_-]{1,64}\/\d{8}\/[a-f0-9]{32}\.(jpg|png|gif|webp)$/

export function mediaPath(value: string): string | undefined {
  const path = value.replace(/^\/(?:api\/)?/, '')
  return localImage.test(path) ? path : undefined
}

export function avatarUrl(value?: string | null): string {
  if (!value) return ''
  const path = mediaPath(value)
  if (path?.startsWith('upload/avatar/')) return `/api/${path}`
  return /^[A-Za-z0-9_-]+\.(jpg|jpeg|png|gif|webp)$/i.test(value)
    ? `/api/upload/avatar/${encodeURIComponent(value)}`
    : ''
}

export function imageMarkdown(path: string, name: string): string {
  const safe = mediaPath(path)
  if (!safe || !safe.startsWith('upload/article/')) throw new Error('图片路径无效')
  const alt =
    name
      .replace(/\.[^.]+$/, '')
      .replace(/[\\\[\]`*_<>\r\n]/g, ' ')
      .trim() || '图片'
  return `![${alt}](/${safe})`
}
