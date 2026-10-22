<template>
  <section class="user-profile-page" :aria-busy="loading">
    <template v-if="profile">
      <ProfileHeader :profile="profile" :is-self="isSelf" @refresh="loadProfile" />
      <ProfileInfo :profile="profile" :is-self="isSelf" />
      <div v-if="errorMessage" class="profile-alert" role="alert">
        {{ errorMessage }}<button type="button" @click="loadProfile">重新加载</button>
      </div>
      <ProfileDetail
        :profile="profile"
        :is-self="isSelf"
        :loading="loading"
        @page-change="handlePageChange"
      />
    </template>
    <div v-else class="profile-status" :role="loading ? 'status' : 'alert'">
      <template v-if="loading"
        ><span class="loading-orbit" />
        <h2>正在加载个人主页</h2>
        <p>资料和动态马上就来。</p></template
      >
      <template v-else
        ><h2>{{ errorCode === 40101 ? '用户不存在' : '暂时无法加载个人主页' }}</h2>
        <p>{{ errorMessage }}</p>
        <button type="button" @click="loadProfile">重新加载</button
        ><RouterLink to="/">返回首页</RouterLink></template
      >
    </div>
  </section>
</template>

<script setup lang="ts">
import ProfileHeader from './ProfileHeader.vue'
import ProfileInfo from './ProfileInfo.vue'
import ProfileDetail from './ProfileDetail.vue'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getProfileApi, type UserProfileVO } from '@/api/profile'
import { useAuthStore } from '@/stores/auth.ts'

type TabType = 'article' | 'draft' | 'comment'
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
</script>

<style scoped>
.user-profile-page {
  min-height: calc(100dvh - 64px);
  padding-bottom: 1px;
}
.profile-status {
  display: flex;
  min-height: 520px;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  flex-direction: column;
  gap: 16px;
  padding: 40px 24px;
  text-align: center;
  color: #cbd5e1;
}
.profile-status h2 {
  font-size: 22px;
}
.profile-status p {
  color: #94a3b8;
  font-size: 14px;
}
.profile-status button,
.profile-alert button {
  border: 1px solid #38bdf840;
  border-radius: 8px;
  padding: 8px 16px;
  background: #38bdf812;
  color: #7dd3fc;
  cursor: pointer;
}
.profile-status a {
  color: #94a3b8;
  font-size: 13px;
}
.profile-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 80%;
  margin: 20px auto 0;
  padding: 14px 20px;
  border: 1px solid #fbbf2430;
  border-radius: 12px;
  background: #fbbf2408;
  color: #d9c89d;
  font-size: 13px;
}
.loading-orbit {
  width: 28px;
  height: 28px;
  border: 2px solid #38bdf820;
  border-top-color: #7dd3fc;
  border-radius: 50%;
  animation: loading-spin 0.8s linear infinite;
}
@keyframes loading-spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 760px) {
  .profile-alert {
    width: calc(100% - 32px);
  }
}
@media (prefers-reduced-motion: reduce) {
  .loading-orbit {
    animation: none;
  }
}
</style>
