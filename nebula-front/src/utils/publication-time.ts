/** 日期控件中的时间始终解释为上海时间，不依赖浏览器所在时区。 */
export function publicationTime(value: string, now = Date.now()): string | null {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) return null
  const iso = `${value}:00+08:00`
  const date = new Date(iso)
  if (!Number.isFinite(date.getTime()) || date.getTime() <= now) return null
  // 拒绝日期溢出，例如2月30日被Date自动归一化。
  const shanghai = new Date(date.getTime() + 8 * 60 * 60 * 1000).toISOString().slice(0, 16)
  return shanghai === value ? iso : null
}
