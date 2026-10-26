import { computed, onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/** 首页装饰动效仅在板块可见、页面前台且用户允许时播放，离页清理监听。 */
export function useHomeSectionMotion(section: Ref<HTMLElement | null>) {
  const motionEnabled = ref(true)
  const visible = ref(true)
  const foreground = ref(true)
  const reducedMotion = ref(false)
  const isAnimated = computed(
    () => motionEnabled.value && visible.value && foreground.value && !reducedMotion.value,
  )
  let observer: IntersectionObserver | undefined
  let removeListeners = () => {}

  onMounted(() => {
    const media = window.matchMedia('(prefers-reduced-motion: reduce)')
    const updateVisibility = () => {
      foreground.value = !document.hidden
    }
    const updatePreference = () => {
      reducedMotion.value = media.matches
    }
    updateVisibility()
    updatePreference()
    if (section.value && 'IntersectionObserver' in window) {
      observer = new IntersectionObserver(
        ([entry]) => {
          visible.value = entry?.isIntersecting ?? false
        },
        { threshold: 0.03 },
      )
      observer.observe(section.value)
    }
    document.addEventListener('visibilitychange', updateVisibility)
    media.addEventListener('change', updatePreference)
    removeListeners = () => {
      document.removeEventListener('visibilitychange', updateVisibility)
      media.removeEventListener('change', updatePreference)
    }
  })
  onBeforeUnmount(() => {
    observer?.disconnect()
    removeListeners()
  })

  return { motionEnabled, reducedMotion, isAnimated }
}
