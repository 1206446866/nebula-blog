import { onMounted, onScopeDispose, ref, watch, type Ref, type WatchSource } from 'vue'
import type { Page } from '@/types/page'
import type { Result } from '@/types/result'

/** 管理页提供业务条件，表格负责页码、每页数量及取消信号。 */
export interface TableQuery {
  current: number
  size: number
  signal: AbortSignal
}

export interface TableOptions {
  pageSize?: number
  watch?: WatchSource | WatchSource[]
  debounceMs?: number
  immediate?: boolean
}

/** 管理后台分页查询；只接收最新结果，统一处理搜索、重试及离页清理。 */
export function useTable<T extends object>(
  query: (page: TableQuery) => Promise<Result<Page<T>>>,
  options: TableOptions = {},
) {
  // 普通 ref 保留行数据的深层响应能力，行内状态修改仍能更新表格。
  const list = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const error = ref<string | null>(null)
  const current = ref(1)
  const size = ref(positiveInteger(options.pageSize, 10))
  const total = ref(0)
  let revision = 0
  let disposed = false
  let controller: AbortController | undefined
  let timer: ReturnType<typeof setTimeout> | undefined

  function clearTimer() {
    if (timer !== undefined) clearTimeout(timer)
    timer = undefined
  }

  /** 撤销未完成的查询，即使接口没有及时响应取消，也不能再写回旧结果。 */
  function invalidate() {
    revision++
    controller?.abort()
    controller = undefined
  }

  async function reload(): Promise<boolean> {
    if (disposed) return false
    clearTimer()
    invalidate()
    const run = revision
    const active = () => !disposed && run === revision
    const requestController = new AbortController()
    controller = requestController
    loading.value = true
    error.value = null
    let page = current.value
    const pageSize = size.value
    try {
      // 删除最后一页的数据或并发删除后，最多重新定位两次，避免无限重查。
      for (let attempt = 0; attempt < 3; attempt++) {
        const response = await query({
          current: page,
          size: pageSize,
          signal: requestController.signal,
        })
        if (!active()) return false
        const data = response.data
        if (
          !data ||
          !Array.isArray(data.records) ||
          !Number.isSafeInteger(data.totalRow) ||
          data.totalRow < 0 ||
          !Number.isSafeInteger(data.pageNumber) ||
          data.pageNumber < 1 ||
          !Number.isSafeInteger(data.pageSize) ||
          data.pageSize < 1
        )
          throw new Error('Invalid table page')
        const lastPage = Math.max(1, Math.ceil(data.totalRow / data.pageSize))
        if (data.totalRow > 0 && data.pageNumber > lastPage) {
          page = lastPage
          continue
        }
        list.value = data.records
        total.value = data.totalRow
        current.value = data.totalRow === 0 ? 1 : data.pageNumber
        size.value = data.pageSize
        return true
      }
      throw new Error('Table page changed repeatedly')
    } catch (cause) {
      if (active() && !requestController.signal.aborted && !isCancellation(cause)) {
        list.value = []
        total.value = 0
        error.value = '列表加载失败，请重试以获取当前条件的数据。'
      }
      return false
    } finally {
      if (active()) {
        loading.value = false
        controller = undefined
      }
    }
  }

  /** 主动搜索取消待执行的防抖任务，并从第一页查询。 */
  function search() {
    if (disposed) return Promise.resolve(false)
    current.value = 1
    return reload()
  }

  function changePage(page: number) {
    if (disposed || !Number.isSafeInteger(page) || page < 1 || page === current.value)
      return Promise.resolve(false)
    current.value = page
    return reload()
  }

  /** 切换每页数量只发起一次第一页查询，忽略分页控件紧接着发出的同页事件。 */
  function changeSize(pageSize: number) {
    if (disposed || !Number.isSafeInteger(pageSize) || pageSize < 1 || pageSize === size.value)
      return Promise.resolve(false)
    size.value = pageSize
    current.value = 1
    return reload()
  }

  function scheduleSearch() {
    if (disposed) return
    clearTimer()
    invalidate()
    current.value = 1
    loading.value = true
    error.value = null
    timer = setTimeout(
      () => {
        timer = undefined
        void reload()
      },
      Math.max(0, options.debounceMs ?? 300),
    )
  }

  if (options.watch) {
    const sources = Array.isArray(options.watch) ? options.watch : [options.watch]
    watch(sources, scheduleSearch, { flush: 'sync' })
  }
  if (options.immediate !== false)
    onMounted(() => {
      void reload()
    })
  onScopeDispose(() => {
    disposed = true
    clearTimer()
    invalidate()
    loading.value = false
  })

  return { list, loading, error, current, size, total, reload, search, changePage, changeSize }
}

function positiveInteger(value: number | undefined, fallback: number) {
  return value !== undefined && Number.isSafeInteger(value) && value > 0 ? value : fallback
}

function isCancellation(cause: unknown) {
  if (!cause || typeof cause !== 'object') return false
  return (
    ('code' in cause && cause.code === 'ERR_CANCELED') ||
    ('name' in cause && cause.name === 'AbortError')
  )
}
