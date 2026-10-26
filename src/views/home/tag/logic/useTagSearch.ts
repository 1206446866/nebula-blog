import { computed, ref, watch, type Ref } from 'vue'
import type { TagVO } from '@/api/tag'

/** 只按标签名称寻星；原星图保持原位，异页命中分束召入，避免堆满画面。 */
export function useTagSearch(tags: Ref<TagVO[]>, pageTags: Ref<TagVO[]>, pageSize: Ref<number>) {
  const searchDraft = ref('')
  const searchQuery = ref('')
  const searchBatch = ref(0)
  const isSearching = computed(() => !!searchQuery.value)
  const batchSize = computed(() => (pageSize.value <= 6 ? 1 : 3))
  const normalized = (text: string) => text.normalize('NFKC').trim().toLocaleLowerCase('zh-CN')
  const searchMatches = computed(() => {
    const query = normalized(searchQuery.value)
    if (!query) return []
    // 完全相同的名称优先，其次是前缀和包含匹配；不检索简介和文章。
    const rank = (tag: TagVO) => {
      const name = normalized(tag.name)
      return name === query ? 0 : name.startsWith(query) ? 1 : 2
    }
    return tags.value
      .filter((tag) => normalized(tag.name).includes(query))
      .sort((a, b) => rank(a) - rank(b))
  })
  const matchedIds = computed(() => new Set(searchMatches.value.map((tag) => tag.id)))
  const distantMatches = computed(() => {
    const currentIds = new Set(pageTags.value.map((tag) => tag.id))
    return searchMatches.value.filter((tag) => !currentIds.has(tag.id))
  })
  const searchBatchCount = computed(() =>
    Math.max(1, Math.ceil(distantMatches.value.length / batchSize.value)),
  )
  const incomingTags = computed(() =>
    distantMatches.value.slice(
      searchBatch.value * batchSize.value,
      (searchBatch.value + 1) * batchSize.value,
    ),
  )
  const incomingIds = computed(() => new Set(incomingTags.value.map((tag) => tag.id)))
  const searchStatus = computed(() => {
    if (!isSearching.value) return '输入标签名，让微光回应你'
    if (!searchMatches.value.length) return '尚未发现这枚星尘，换个名字试试'
    return `找到 ${searchMatches.value.length} 枚星尘${incomingTags.value.length ? ` · ${incomingTags.value.length} 枚从远处飞来` : ' · 已在星图中点亮'}`
  })
  function searchTags() {
    searchQuery.value = searchDraft.value.normalize('NFKC').trim().slice(0, 50)
    searchBatch.value = 0
  }
  function clearSearch() {
    searchDraft.value = ''
    searchQuery.value = ''
    searchBatch.value = 0
  }
  function turnSearchBatch(direction: number) {
    searchBatch.value =
      (searchBatch.value + direction + searchBatchCount.value) % searchBatchCount.value
  }
  // 清空输入立即恢复星图；切换原星图、尺寸或数据后重新整理远处命中。
  watch(searchDraft, (value) => {
    if (!value.trim()) clearSearch()
  })
  watch([pageTags, pageSize], () => {
    searchBatch.value = 0
  })
  watch(searchBatchCount, (count) => {
    searchBatch.value = Math.min(searchBatch.value, count - 1)
  })
  return {
    searchDraft,
    searchQuery,
    isSearching,
    searchMatches,
    matchedIds,
    incomingTags,
    incomingIds,
    searchBatch,
    searchBatchCount,
    searchStatus,
    searchTags,
    clearSearch,
    turnSearchBatch,
  }
}
