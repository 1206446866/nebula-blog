import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

/**
 * 标准时间格式
 */
export const formatDateTime = (time?: string | Date) => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 仅日期
 */
export const formatDate = (time?: string | Date) => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD')
}

/**
 * 七天内时间显示
 *
 * 今天
 * 昨天
 * 前天
 * 3天前
 * 超过7天显示完整日期
 */
export function formatRelativeDay(dateTime: string | Date): string {
  const date = new Date(dateTime)

  const now = new Date()

  // 清除时分秒，只比较日期
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

  const target = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  const diffTime = today.getTime() - target.getTime()

  const diffDay = Math.floor(diffTime / (1000 * 60 * 60 * 24))

  switch (diffDay) {
    case 0:
      return '今天'

    case 1:
      return '昨天'

    case 2:
      return '前天'

    case 3:
    case 4:
    case 5:
    case 6:
      return `${diffDay}天前`

    default:
      return formatDateTime(dateTime)
  }
}
