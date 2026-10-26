import { getProfileApi, type UserProfileVO } from '@/api/profile'
import { useAuthStore } from '@/stores/auth.ts'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

export type TabType = 'article' | 'draft' | 'comment'

/** 管理个人主页加载的状态与交互。 */
export function useUserProfile() {
  const route = useRoute()
  const userStore = useAuthStore()
  const userId = computed(() => Number(route.params.userId))
  const profile = ref<UserProfileVO | null>(null)
  const isSelf = computed(() =>
    Boolean(profile.value && userStore.user?.id === profile.value.userProfileInfoVO.id),
  )
  const loading = ref(false)
  const errorMessage = ref('')
  const errorCode = ref<number | null>(null)
  const pages = ref({ article: 1, draft: 1, comment: 1 })
  let requestVersion = 0
  let controller: AbortController | undefined

  function handlePageChange(type: TabType, page: number) {
    if (loading.value || !Number.isSafeInteger(page) || page < 1) return
    pages.value[type] = page
    void loadProfile()
  }

  async function loadProfile() {
    const version = ++requestVersion
    controller?.abort()
    const currentController = new AbortController()
    controller = currentController
    errorMessage.value = ''
    errorCode.value = null
    if (!Number.isSafeInteger(userId.value) || userId.value < 1) {
      loading.value = false
      errorMessage.value = '用户地址无效，请检查链接。'
      return
    }
    loading.value = true
    try {
      const res = await getProfileApi(
        userId.value,
        {
          publicArticlePage: pages.value.article,
          publicArticleSize: 5,
          draftArticlePage: pages.value.draft,
          draftArticleSize: 5,
          commentPage: pages.value.comment,
          commentSize: 5,
        },
        currentController.signal,
      )
      if (version !== requestVersion) return
      profile.value = res.data
      pages.value = {
        article: res.data.publicArticles.pageNumber,
        draft: res.data.draftArticles?.pageNumber ?? 1,
        comment: res.data.comments.pageNumber,
      }
    } catch (error) {
      if (currentController.signal.aborted || version !== requestVersion) return
      const code = (error as { code?: unknown })?.code
      errorCode.value = typeof code === 'number' ? code : null
      errorMessage.value =
        code === 40101 ? '这个用户不存在，或已被删除。' : '加载失败，请检查服务连接后重试。'
    } finally {
      if (version === requestVersion) loading.value = false
    }
  }

  watch(
    userId,
    () => {
      profile.value = null
      pages.value = { article: 1, draft: 1, comment: 1 }
      void loadProfile()
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    requestVersion++
    controller?.abort()
  })

  return { profile, isSelf, loading, errorMessage, errorCode, handlePageChange, loadProfile }
}
