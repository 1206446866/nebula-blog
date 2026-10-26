import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

/** 首页收尾仅展示现有且有访问权限的入口，不提供未开放板块占位。 */
export function useHomeClosing() {
  const auth = useAuthStore()
  const canRead = computed(() => auth.hasPermission('article:list'))
  const links = computed(() =>
    [
      { label: '文章', to: '/article', permission: 'article:list' },
      { label: '标签', to: '/tag', permission: 'tag:list' },
      { label: '分类', to: '/category', permission: 'category:list' },
    ].filter((link) => auth.hasPermission(link.permission)),
  )

  /** 回到首页起点并移动键盘焦点；减少动态效果时直接定位。 */
  const goTop = () => {
    document.getElementById('home-start')?.focus({ preventScroll: true })
    window.scrollTo({
      top: 0,
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    })
  }

  return { canRead, links, goTop, year: new Date().getFullYear() }
}
