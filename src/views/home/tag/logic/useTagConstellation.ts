import { computed, onBeforeUnmount, onMounted, ref, watch, type CSSProperties, type Ref } from 'vue'
import type { TagVO } from '@/api/tag'
import { useTagSearch } from './useTagSearch'

/** 标签沿星环的远近轨道分布，轮换时保留每个真实标签的访问入口。 */
export function useTagConstellation(tags: Ref<TagVO[]>) {
  const page = ref(0)
  const arrangement = ref(0)
  const paused = ref(false)
  const activeId = ref<number>()
  const pointer = ref({ x: 0, y: 0 })
  const pageSize = ref(12)
  let compactScreen: MediaQueryList | undefined
  const pageCount = computed(() => Math.max(1, Math.ceil(tags.value.length / pageSize.value)))
  const pageTags = computed(() =>
    tags.value.slice(page.value * pageSize.value, (page.value + 1) * pageSize.value),
  )
  const search = useTagSearch(tags, pageTags, pageSize)
  const visibleTags = computed(() => [...pageTags.value, ...search.incomingTags.value])
  const activeTag = computed(
    () => visibleTags.value.find((tag) => tag.id === activeId.value) ?? visibleTags.value[0],
  )
  const activeNumber = computed(() =>
    String(tags.value.findIndex((tag) => tag.id === activeTag.value?.id) + 1).padStart(2, '0'),
  )
  const countLabel = computed(() => String(tags.value.length).padStart(3, '0'))
  const sceneStyle = computed(
    () =>
      ({
        '--pointer-x': `${pointer.value.x * 16}px`,
        '--pointer-y': `${pointer.value.y * 12}px`,
      }) as CSSProperties,
  )
  // 留出标题和底部观测信息的位置，节点以远近层次错落排布。
  const positions = [
    [55, 77, 1.1],
    [48, 55, 0.7],
    [79, 34, 0.65],
    [29, 83, 0.7],
    [84, 65, 1],
    [32, 69, 0.65],
    [91, 24, 0.45],
    [63, 44, 0.55],
    [69, 78, 1.05],
    [42, 37, 0.45],
    [91, 48, 0.65],
    [43, 67, 0.85],
  ] as const
  const compactPositions = [
    [22, 18],
    [72, 10],
    [46, 44],
    [18, 73],
    [72, 73],
    [79, 42],
  ] as const
  /** 在现有星尘之间寻找最大的留白，避开标题、寻星入口和底部观测区。 */
  const incomingPositions = computed(() => {
    const occupied: number[][] = pageTags.value.map((_, index) => [
      ...positions[(index + arrangement.value) % positions.length]!,
    ])
    const selected: number[][] = []
    for (let index = 0; index < search.incomingTags.value.length; index++) {
      let best = [62, 28, 1]
      let largestGap = -1
      for (let x = 40; x <= 88; x += 8) {
        for (let y = 25; y <= 81; y += 8) {
          if (x > 76 && y > 73) continue
          const gap = Math.min(
            ...occupied.map((point) => Math.hypot((x - point[0]!) * 1.4, y - point[1]!)),
          )
          if (gap > largestGap) {
            largestGap = gap
            best = [x, y, 1]
          }
        }
      }
      occupied.push(best)
      selected.push(best)
    }
    return selected
  })
  function nodeStyle(index: number, tagId: number): CSSProperties {
    const incomingIndex = search.incomingTags.value.findIndex((tag) => tag.id === tagId)
    const point = positions[(index + arrangement.value) % positions.length]!
    const compact = compactPositions[(index + arrangement.value) % compactPositions.length]!
    const destination = incomingPositions.value[incomingIndex]
    return {
      '--x': `${destination?.[0] ?? point[0]}%`,
      '--y': `${destination?.[1] ?? point[1]}%`,
      '--compact-x': `${incomingIndex >= 0 ? 47 : compact[0]}%`,
      '--compact-y': `${incomingIndex >= 0 ? 93 : compact[1]}%`,
      '--flight-x': `${index % 2 === 0 ? 320 : -280}px`,
      '--flight-y': '-190px',
      '--depth': point[2],
      '--delay': `${-index * 1.7}s`,
      '--duration': `${8 + (index % 5) * 1.3}s`,
      '--drift': `${7 + (index % 4) * 2}px`,
      '--node-size': `${16 + point[2] * 12}px`,
      '--node-color':
        index % 4 === 0 ? '230, 205, 157' : index % 3 === 0 ? '163, 197, 207' : '199, 195, 176',
    } as CSSProperties
  }
  function movePointer(event: PointerEvent) {
    if (event.pointerType === 'touch') return
    const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
    pointer.value = {
      x: (event.clientX - bounds.left) / bounds.width - 0.5,
      y: (event.clientY - bounds.top) / bounds.height - 0.5,
    }
  }
  function resetPointer() {
    pointer.value = { x: 0, y: 0 }
  }
  function turnPage(direction: number) {
    page.value = (page.value + direction + pageCount.value) % pageCount.value
    activeId.value = undefined
  }
  function rearrange() {
    arrangement.value = (arrangement.value + 5) % positions.length
  }
  function tagNumber(tagId: number) {
    return String(tags.value.findIndex((tag) => tag.id === tagId) + 1).padStart(2, '0')
  }
  watch([search.searchQuery, search.incomingTags], () => {
    activeId.value =
      search.incomingTags.value[0]?.id ??
      pageTags.value.find((tag) => search.matchedIds.value.has(tag.id))?.id
  })
  /** 窄屏减少单张星图的密度，调整尺寸后保留当前浏览位置。 */
  function adaptDensity() {
    const firstIndex = page.value * pageSize.value
    pageSize.value = compactScreen?.matches ? 6 : 12
    page.value = Math.min(Math.floor(firstIndex / pageSize.value), pageCount.value - 1)
  }
  onMounted(() => {
    compactScreen = window.matchMedia('(max-width: 650px)')
    adaptDensity()
    compactScreen.addEventListener('change', adaptDensity)
  })
  onBeforeUnmount(() => compactScreen?.removeEventListener('change', adaptDensity))
  watch(tags, () => {
    page.value = Math.min(page.value, pageCount.value - 1)
  })
  return {
    ...search,
    page,
    pageSize,
    pageCount,
    paused,
    activeId,
    activeTag,
    activeNumber,
    visibleTags,
    countLabel,
    pointer,
    sceneStyle,
    nodeStyle,
    tagNumber,
    movePointer,
    resetPointer,
    turnPage,
    rearrange,
  }
}
