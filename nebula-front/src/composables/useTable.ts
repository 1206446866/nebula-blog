import { ref } from 'vue'

export function useTable<T, P extends any[]>(
  api: (...args: P) => Promise<any>,
  defaultPageSize = 10,
) {
  const list = ref<T[]>([])
  const loading = ref(false)
  const current = ref(1)
  const size = ref(defaultPageSize)
  const total = ref(0)

  const fetch = async (...args: P) => {
    loading.value = true
    try {
      const res = await api(...args)
      list.value = res.data.records
      total.value = res.data.totalRow
      current.value = res.data.pageNumber
      size.value = res.data.pageSize
    } finally {
      loading.value = false
    }
  }

  const resetPage = () => {
    current.value = 1
  }

  return {
    list,
    loading,
    current,
    size,
    total,
    fetch,
    resetPage,
  }
}
