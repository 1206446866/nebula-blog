import type { UserProfileVO } from '@/api/profile.ts'
import { useAuthStore } from '@/stores/auth'
import type { EmitFn } from 'vue'
import { computed, ref, useId, watch } from 'vue'

export type TabType = 'article' | 'comment' | 'draft'

/** 管理个人动态与分页切换的状态与交互。 */
export function useProfileDetail(
  props: Readonly<{ profile: UserProfileVO | null; isSelf: boolean; loading: boolean }>,
  emit: EmitFn<{ 'page-change': [type: TabType, page: number] }>,
) {
  const activeTab = ref<TabType>('article')
  const auth = useAuthStore()
  const tabId = useId()
  const tabs = computed(() => [
    {
      id: 'article' as const,
      label: props.isSelf ? '我的文章' : 'TA的文章',
      count: props.profile?.publicArticles?.totalRow ?? 0,
    },
    {
      id: 'comment' as const,
      label: props.isSelf ? '我的评论' : 'TA的评论',
      count: props.profile?.comments?.totalRow ?? 0,
    },
    ...(props.isSelf
      ? [
          {
            id: 'draft' as const,
            label: '我的草稿',
            count: props.profile?.draftArticles?.totalRow ?? 0,
          },
        ]
      : []),
  ])
  const activePage = computed(() =>
    activeTab.value === 'article'
      ? props.profile?.publicArticles
      : activeTab.value === 'comment'
        ? props.profile?.comments
        : props.profile?.draftArticles,
  )
  const articles = computed(() => props.profile?.publicArticles?.records ?? [])
  const comments = computed(() => props.profile?.comments?.records ?? [])
  const drafts = computed(() => props.profile?.draftArticles?.records ?? [])
  const loginRecords = computed(() => (props.isSelf ? (props.profile?.loginLogs ?? []) : []))
  const emptyText = computed(
    () =>
      ({ article: '还没有发布文章', comment: '还没有公开评论', draft: '还没有保存草稿' })[
        activeTab.value
      ],
  )

  watch(
    () => props.profile?.userProfileInfoVO.id,
    () => {
      activeTab.value = 'article'
    },
  )

  watch(
    () => props.isSelf,
    (isSelf) => {
      if (!isSelf && activeTab.value === 'draft') activeTab.value = 'article'
    },
  )

  function changePage(page: number) {
    emit('page-change', activeTab.value, page)
  }

  function handleTabKey(event: KeyboardEvent, index: number) {
    let target: number
    if (event.key === 'ArrowRight') target = (index + 1) % tabs.value.length
    else if (event.key === 'ArrowLeft') target = (index - 1 + tabs.value.length) % tabs.value.length
    else if (event.key === 'Home') target = 0
    else if (event.key === 'End') target = tabs.value.length - 1
    else return
    event.preventDefault()
    activeTab.value = tabs.value[target]!.id
    const buttons = (
      event.currentTarget as HTMLElement
    ).parentElement?.querySelectorAll<HTMLButtonElement>('[role="tab"]')
    buttons?.[target]?.focus()
  }

  return {
    activeTab,
    auth,
    tabId,
    tabs,
    activePage,
    articles,
    comments,
    drafts,
    loginRecords,
    emptyText,
    changePage,
    handleTabKey,
  }
}
