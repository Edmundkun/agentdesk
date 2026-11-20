import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/** 后端时间格式 `YYYY-MM-DD HH:mm:ss`，统一展示到分钟 */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm') : value
}

/** 会话列表使用的相对时间，超过 7 天回落为日期 */
export function formatRelative(value?: string | null): string {
  if (!value) return '—'
  const date = dayjs(value)
  if (!date.isValid()) return value
  return dayjs().diff(date, 'day') >= 7 ? date.format('MM-DD') : date.fromNow()
}

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }
  return `${value >= 10 || unitIndex === 0 ? Math.round(value) : value.toFixed(1)} ${units[unitIndex]}`
}

export function formatNumber(value: number): string {
  return Number.isFinite(value) ? value.toLocaleString('zh-CN') : '0'
}
