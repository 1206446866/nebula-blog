import type { EmitFn } from 'vue'
import { computed, ref, watch } from 'vue'

/** 管理个人页分页与跳页的状态与交互。 */
export function useProfilePagination(
  props: Readonly<
    Required<{
      currentPage: number
      pageSize: number
      total: number
      loading?: boolean
    }>
  >,
  emit: EmitFn<{ 'update:currentPage': [page: number] }>,
) {
  const totalPages = computed(() =>
    Math.max(1, Math.ceil(Math.max(0, props.total) / Math.max(1, props.pageSize))),
  )
  const current = computed(() => Math.min(totalPages.value, Math.max(1, props.currentPage)))
  const jumpPage = ref('1')

  watch(
    current,
    (value) => {
      jumpPage.value = String(value)
    },
    { immediate: true },
  )
  const pages = computed(() => {
    if (totalPages.value <= 5)
      return Array.from({ length: totalPages.value }, (_, index) => index + 1)
    const visible = new Set([
      1,
      totalPages.value,
      current.value - 1,
      current.value,
      current.value + 1,
    ])
    if (current.value <= 3) [2, 3, 4].forEach((page) => visible.add(page))
    if (current.value >= totalPages.value - 2)
      [totalPages.value - 3, totalPages.value - 2, totalPages.value - 1].forEach((page) =>
        visible.add(page),
      )
    const sorted = [...visible]
      .filter((page) => page >= 1 && page <= totalPages.value)
      .sort((a, b) => a - b)
    const result: (number | string)[] = []
    sorted.forEach((page, index) => {
      if (index > 0 && page - sorted[index - 1]! > 1) result.push(`gap-${page}`)
      result.push(page)
    })
    return result
  })

  function changePage(page: number) {
    if (props.loading || props.total === 0 || !Number.isSafeInteger(page)) return
    const target = Math.min(totalPages.value, Math.max(1, page))
    jumpPage.value = String(target)
    if (target !== current.value) emit('update:currentPage', target)
  }

  function jump() {
    if (!/^\d+$/.test(jumpPage.value)) {
      jumpPage.value = String(current.value)
      return
    }
    const target = Number(jumpPage.value)
    if (Number.isSafeInteger(target)) changePage(target)
    else jumpPage.value = String(current.value)
  }

  return { totalPages, current, jumpPage, pages, changePage, jump }
}
