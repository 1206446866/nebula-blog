import type { CommunityMode } from '@/api/community'
import { useAuthStore } from '@/stores/auth'
import { computed, ref } from 'vue'

/** 管理社区发现与关注列表的状态与交互。 */
export function useCommunityPage() {
  const auth = useAuthStore()
  const userId = computed(() => auth.user?.id ?? 0)
  const mode = ref<CommunityMode>('discover')
  const search = ref('')
  const keyword = ref('')
  const tabs: { value: CommunityMode; label: string }[] = [
    { value: 'discover', label: '发现用户' },
    { value: 'following', label: '我的关注' },
    { value: 'followers', label: '我的粉丝' },
  ]

  return { userId, mode, search, keyword, tabs }
}
