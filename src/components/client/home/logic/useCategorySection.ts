import { computed, ref, useId } from 'vue'
import { useHomeSectionMotion } from './useHomeSectionMotion'

/** 分类航图使用固定示意星座，不对应真实分类编号或文章数量。 */
const regions = [
  {
    id: 'architecture',
    name: '系统与架构',
    english: 'ARCHITECTURE',
    code: 'I',
    color: '#a2ced9',
    rgb: '162, 206, 217',
    description: '从数据的流动，到系统的秩序。沿着实践的航线，走进技术背后的结构。',
    tags: ['后端开发', '系统设计', '数据存储'],
    note: '在复杂之中，寻找清晰。',
    points: [
      [114, 180],
      [190, 116],
      [248, 178],
      [213, 248],
      [132, 270],
    ],
    path: 'M114 180 190 116 248 178 213 248 132 270 114 180M114 180 213 248',
    x: 26.4,
    y: 20.7,
  },
  {
    id: 'experience',
    name: '界面与体验',
    english: 'EXPERIENCE',
    code: 'II',
    color: '#c4b3e0',
    rgb: '196, 179, 224',
    description: '让想法拥有形状，让每次交互都自然发生。在细节之间，发现设计与代码的共鸣。',
    tags: ['前端开发', '交互设计', '视觉表达'],
    note: '把想象，变成可触及的世界。',
    points: [
      [456, 105],
      [543, 149],
      [590, 230],
      [510, 267],
      [453, 203],
    ],
    path: 'M456 105 543 149 590 230 510 267 453 203 456 105M453 203 543 149',
    x: 75.4,
    y: 26.6,
  },
  {
    id: 'intelligence',
    name: '智能与探索',
    english: 'INTELLIGENCE',
    code: 'III',
    color: '#dbbe90',
    rgb: '219, 190, 144',
    description: '带着好奇心，试着往未知多走一步。记录工具、智能与新思路带来的可能。',
    tags: ['AI 应用', '智能工具', '灵感实验'],
    note: '未知的远方，也在回应好奇。',
    points: [
      [118, 389],
      [204, 356],
      [280, 408],
      [219, 471],
      [142, 449],
    ],
    path: 'M118 389 204 356 280 408 219 471 142 449 118 389M204 356 219 471',
    x: 28.3,
    y: 63.6,
  },
  {
    id: 'engineering',
    name: '工程与实践',
    english: 'ENGINEERING',
    code: 'IV',
    color: '#a6cabb',
    rgb: '166, 202, 187',
    description: '从本地的第一步，到稳定运行的每一天。把零散经验，积累成可复用的路径。',
    tags: ['云与部署', '开发工具', '工程经验'],
    note: '让每一次实践，都成为新的起点。',
    points: [
      [444, 355],
      [532, 362],
      [593, 432],
      [498, 471],
      [423, 422],
    ],
    path: 'M444 355 532 362 593 432 498 471 423 422 444 355M444 355 498 471',
    x: 73.9,
    y: 64.6,
  },
] as const

/** 管理分类星座的点亮、介绍及装饰动画状态。 */
export function useCategorySection() {
  const section = ref<HTMLElement | null>(null)
  const activeIndex = ref(0)
  const activeRegion = computed(() => regions[activeIndex.value] ?? regions[0])
  const uid = `category-atlas-${useId()}`
  const paint = (name: string) => `url(#${uid}-${name})`
  const stars = Array.from({ length: 125 }, (_, i) => ({
    x: (i * 137.508 + 17) % 720,
    y: (i * 87.73 + 31) % 560,
    r: i % 13 === 0 ? 1.4 : 0.6,
    opacity: 0.13 + (i % 5) * 0.09,
  }))
  return {
    section,
    activeIndex,
    activeRegion,
    regions,
    uid,
    paint,
    stars,
    ...useHomeSectionMotion(section),
  }
}
