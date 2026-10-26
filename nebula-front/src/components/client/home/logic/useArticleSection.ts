import { computed, ref } from 'vue'
import { useHomeSectionMotion } from './useHomeSectionMotion'

/** 首页文章板块的静态星域介绍，不查询或伪造文章数据。 */
const topics = [
  {
    id: 'backend',
    name: '后端与架构',
    star: '后端星域',
    english: 'BACKEND',
    description: '深入系统的运行轨迹，在接口、数据与架构之间，构建可靠的秩序。',
    tags: ['Java', 'Spring Boot', '系统设计'],
    color: '#8acbdd',
    rgb: '138, 203, 221',
    note: '在复杂之中，寻找清晰的结构。',
  },
  {
    id: 'frontend',
    name: '前端与体验',
    star: '前端星域',
    english: 'FRONTEND',
    description: '让想法拥有可见的形状，用交互、动效与细节，连接人与屏幕。',
    tags: ['Vue 3', 'TypeScript', '交互设计'],
    color: '#b5a2e0',
    rgb: '181, 162, 224',
    note: '让每一个想法，都有被看见的方式。',
  },
  {
    id: 'intelligence',
    name: 'AI 与新可能',
    star: '智能星域',
    english: 'INTELLIGENCE',
    description: '带着好奇心走向未知，探索智能工具如何改变创造与思考的方式。',
    tags: ['AI 应用', '智能体', '效率探索'],
    color: '#d9b68e',
    rgb: '217, 182, 142',
    note: '未知的边界，也是灵感的起点。',
  },
] as const

/** 管理星云观测主题与动效状态。 */
export function useArticleSection() {
  const section = ref<HTMLElement | null>(null)
  const activeIndex = ref(0)
  const activeTopic = computed(() => topics[activeIndex.value] ?? topics[0])
  return { section, activeIndex, activeTopic, topics, ...useHomeSectionMotion(section) }
}
