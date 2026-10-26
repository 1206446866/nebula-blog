<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import {
  getCommunityUsers,
  type CommunityMode,
  type CommunityUser,
  type FollowSummary,
} from '@/api/community'
import type { Page } from '@/types/page'
import { avatarUrl } from '@/utils/resource'
import { useAuthStore } from '@/stores/auth'
import FollowButton from './FollowButton.vue'
import ProfilePagination from '@/views/home/profile/ProfilePagination.vue'
import { clientFeatures } from '@/constants/client-features'

const props = withDefaults(
  defineProps<{
    mode: CommunityMode
    userId: number
    keyword?: string
    refreshKey?: number
  }>(),
  { keyword: '', refreshKey: 0 },
)
const emit = defineEmits<{ changed: [] }>()
const auth = useAuthStore()
const page = ref(1)
const result = ref<Page<CommunityUser> | null>(null)
const loading = ref(false)
const failed = ref(false)
let revision = 0
let controller: AbortController | undefined
async function load() {
  const version = ++revision
  controller?.abort()
  controller = new AbortController()
  const signal = controller.signal
  loading.value = true
  failed.value = false
  try {
    const response = await getCommunityUsers(
      props.mode,
      props.userId,
      page.value,
      props.keyword,
      signal,
    )
    if (version !== revision) return
    result.value = response.data
    page.value = response.data.pageNumber
  } catch {
    if (version === revision && !signal.aborted) failed.value = true
  } finally {
    if (version === revision) loading.value = false
  }
}
function changePage(value: number) {
  page.value = value
  void load()
}
function changed(user: CommunityUser, summary: FollowSummary) {
  Object.assign(user, {
    following: summary.following,
    followedBy: summary.followedBy,
    canFollow: summary.canFollow,
  })
  emit('changed')
  void load()
}
watch(
  () => props.refreshKey,
  () => {
    void load()
  },
)
watch(
  () => [props.mode, props.userId, props.keyword, auth.getToken()],
  () => {
    result.value = null
    page.value = 1
    void load()
  },
  { immediate: true },
)
onBeforeUnmount(() => {
  revision++
  controller?.abort()
})
</script>

<template>
  <section class="community-users" :aria-busy="loading">
    <div v-if="failed" class="list-status" role="alert">
      <p>暂时无法加载用户列表。</p>
      <button type="button" @click="load">重新加载</button>
    </div>
    <p v-else-if="loading && !result" class="list-status" role="status">正在寻找社区伙伴…</p>
    <template v-else-if="result">
      <div v-if="result.records.length" class="user-grid">
        <article v-for="user in result.records" :key="user.id" class="user-card">
          <RouterLink :to="`/user/${user.id}`" class="user-link">
            <el-avatar :size="46" :src="avatarUrl(user.avatar)">{{
              user.username?.charAt(0) || 'U'
            }}</el-avatar>
            <div class="user-info">
              <strong>{{ user.username }}</strong
              ><span>@{{ user.nid }}</span>
              <small v-if="user.followedBy && !user.self">{{
                user.following ? '互相关注' : '关注了你'
              }}</small>
            </div>
          </RouterLink>
          <FollowButton
            :user-id="user.id"
            :relationship="user"
            @changed="(value) => changed(user, value)"
          />
        </article>
      </div>
      <div v-else class="list-status">
        <strong>{{
          mode === 'discover'
            ? '暂时没有找到用户'
            : mode === 'following'
              ? '还没有关注的人'
              : '还没有粉丝'
        }}</strong>
        <p>
          {{
            mode === 'discover'
              ? '试试其他昵称或 NID。'
              : '从一篇文章、一次交流开始，认识更多伙伴。'
          }}
        </p>
        <RouterLink v-if="mode !== 'discover' && clientFeatures.community" to="/community"
          >去社区看看</RouterLink
        >
      </div>
      <ProfilePagination
        :current-page="page"
        :page-size="12"
        :total="result.totalRow"
        :loading="loading"
        @update:current-page="changePage"
      />
    </template>
  </section>
</template>

<style scoped>
.community-users {
  min-width: 0;
}
.user-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.user-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  border: 1px solid #b6c6eb1c;
  border-radius: 14px;
  background: #18233966;
}
.user-link {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 13px;
  color: #e2e8f0;
  text-decoration: none;
}
.user-link :deep(.el-avatar) {
  flex-shrink: 0;
  background: #354366;
  color: #d9e5ff;
}
.user-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.user-info strong {
  font-size: 15px;
  overflow-wrap: anywhere;
}
.user-info span {
  font-size: 12px;
  color: #91a0bb;
  overflow-wrap: anywhere;
}
.user-info small {
  font-size: 11px;
  color: #c4b5fd;
}
.list-status {
  padding: 42px 20px;
  text-align: center;
  color: #a4b2c9;
  line-height: 1.8;
}
.list-status p {
  font-size: 13px;
}
.list-status a,
.list-status button {
  color: #a5deff;
}
.list-status button {
  background: #38bdf818;
  border: 1px solid #7dd3fc35;
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
}
@media (max-width: 850px) {
  .user-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 420px) {
  .user-card {
    padding: 15px 12px;
    gap: 8px;
  }
  .user-link {
    gap: 9px;
  }
}
</style>
