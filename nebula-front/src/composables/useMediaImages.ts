import { nextTick, onBeforeUnmount, watch, type Ref } from 'vue'
import { mediaImageApi } from '@/api/media'
import { useAuthStore } from '@/stores/auth'
import { mediaPath } from '@/utils/resource'

/** 按当前页面实例和会话缓存图片，避免每次编辑正文都重新下载。 */
export function useMediaImages(root: Ref<HTMLElement | null>, html: Ref<string>) {
  const auth = useAuthStore()
  type Entry = { controller: AbortController; promise: Promise<string>; url?: string }
  const cache = new Map<string, Entry>()
  let generation = 0
  let session = auth.getToken()
  function release(entry: Entry) {
    entry.controller.abort()
    if (entry.url) URL.revokeObjectURL(entry.url)
  }
  function clear() {
    cache.forEach(release)
    cache.clear()
  }
  watch(
    [html, root, () => auth.getToken()],
    async () => {
      const run = ++generation
      const token = auth.getToken()
      if (token !== session) {
        clear()
        root.value
          ?.querySelectorAll('img[data-media-path]')
          .forEach((image) => image.removeAttribute('src'))
        session = token
      }
      await nextTick()
      if (run !== generation) return
      const groups = new Map<string, HTMLImageElement[]>()
      root.value?.querySelectorAll<HTMLImageElement>('img[data-media-path]').forEach((image) => {
        const path = mediaPath(image.dataset.mediaPath || '')
        if (path) groups.set(path, [...(groups.get(path) || []), image])
      })
      for (const [path, entry] of cache) {
        if (!groups.has(path)) {
          release(entry)
          cache.delete(path)
        }
      }
      if (!token) return
      await Promise.allSettled(
        Array.from(groups, async ([path, images]) => {
          let entry = cache.get(path)
          if (!entry) {
            const controller = new AbortController()
            const created: Entry = { controller, promise: Promise.resolve('') }
            created.promise = mediaImageApi(path, controller.signal)
              .then((blob) => {
                if (
                  controller.signal.aborted ||
                  cache.get(path) !== created ||
                  auth.getToken() !== token
                )
                  throw new Error('Session or view changed')
                if (!blob.type.startsWith('image/')) throw new Error('图片内容无效')
                created.url = URL.createObjectURL(blob)
                return created.url
              })
              .catch((error) => {
                if (cache.get(path) === created) cache.delete(path)
                throw error
              })
            cache.set(path, created)
            entry = created
          }
          try {
            const url = await entry.promise
            if (run !== generation || auth.getToken() !== token) return
            images.forEach((image) => {
              if (image.isConnected) image.src = url
            })
          } catch {
            if (run === generation && auth.getToken() === token)
              images.forEach((image) => {
                image.title = '图片暂时无法加载，请重新打开页面重试'
              })
          }
        }),
      )
    },
    { immediate: true, flush: 'post' },
  )
  onBeforeUnmount(() => {
    generation++
    clear()
  })
}
