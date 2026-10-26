<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { getFollowSummary, type FollowSummary } from '@/api/community'
import { useAuthStore } from '@/stores/auth'
import FollowButton from './FollowButton.vue'
import CommunityUserList from './CommunityUserList.vue'
import { clientFeatures } from '@/constants/client-features'

const props = defineProps<{ userId: number }>()
const auth = useAuthStore()
const summary = ref<FollowSummary | null>(null)
const expanded = ref<'following' | 'followers' | null>(null)
const failed = ref(false)
const listRevision = ref(0)
let revision = 0
let controller: AbortController | undefined
async function load() {
  const version = ++revision
  controller?.abort()
  controller = new AbortController()
  failed.value = false
  try {
    const response = await getFollowSummary(props.userId, controller.signal)
    if (version === revision) summary.value = response.data
  } catch {
    if (version === revision && !controller?.signal.aborted) failed.value = true
  }
}
function changed(value: FollowSummary) {
  // 取消可能仍在进行的旧统计请求，避免覆盖操作结果。
  revision++
  controller?.abort()
  summary.value = value
  failed.value = false
  listRevision.value++
}
watch(
  () => [props.userId, auth.getToken()],
  () => {
    summary.value = null
    expanded.value = null
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
  <section class="connections">
    <div v-if="summary" class="connection-bar">
      <div class="counts">
        <button
          type="button"
          :class="{ active: expanded === 'following' }"
          :aria-expanded="expanded === 'following'"
          @click="expanded = expanded === 'following' ? null : 'following'"
        >
          <strong>{{ summary.followingCount }}</strong> 关注
        </button>
        <button
          type="button"
          :class="{ active: expanded === 'followers' }"
          :aria-expanded="expanded === 'followers'"
          @click="expanded = expanded === 'followers' ? null : 'followers'"
        >
          <strong>{{ summary.followerCount }}</strong> 粉丝
        </button>
        <span v-if="summary.followedBy && !summary.self" class="relationship-hint">{{
          summary.following ? '互相关注' : '关注了你'
        }}</span>
      </div>
      <FollowButton
        v-if="!summary.self"
        :user-id="userId"
        :relationship="summary"
        @changed="changed"
      />
      <RouterLink v-else-if="clientFeatures.community" to="/community">发现社区伙伴 →</RouterLink>
    </div>
    <div v-if="failed" class="connection-error" role="alert">
      关注信息加载失败 <button type="button" @click="load">重试</button>
    </div>
    <p v-else-if="!summary" class="connection-error" role="status">正在加载关注信息…</p>
    <div v-if="expanded" class="connection-list">
      <h3>{{ expanded === 'following' ? '关注的人' : '粉丝' }}</h3>
      <CommunityUserList
        :key="expanded"
        :mode="expanded"
        :user-id="userId"
        :refresh-key="listRevision"
        @changed="load"
      />
    </div>
  </section>
</template>

<style scoped>
.connections {
  width: 80%;
  margin: 18px auto 0;
  border: 1px solid #b6c6eb1c;
  border-radius: 16px;
  background: #0f172a88;
  padding: 20px 24px;
}
.connection-bar,
.counts {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
}
.connection-bar {
  justify-content: space-between;
  gap: 16px;
}
.counts button {
  font: inherit;
  font-size: 13px;
  color: #94a3b8;
  border: 0;
  background: none;
  padding: 4px 0;
  cursor: pointer;
}
.counts strong {
  color: #e2e8f0;
  font-size: 21px;
  margin-right: 5px;
}
.counts button.active,
.counts button:hover {
  color: #a5deff;
}
.relationship-hint {
  font-size: 12px;
  color: #c4b5fd;
}
.connection-bar a {
  font-size: 13px;
  color: #a5deff;
  text-decoration: none;
}
.connection-list {
  padding-top: 22px;
  border-top: 1px solid #b6c6eb1c;
  margin-top: 20px;
}
.connection-list h3 {
  color: #dbe5f5;
  font-size: 16px;
  margin: 0 0 16px;
}
.connection-error {
  color: #94a3b8;
  font-size: 13px;
}
.connection-error button {
  margin-left: 10px;
  color: #a5deff;
  background: none;
  border: 0;
  cursor: pointer;
}
@media (max-width: 1050px) {
  .connections {
    width: 90%;
  }
}
@media (max-width: 760px) {
  .connections {
    width: calc(100% - 32px);
    padding: 18px;
  }
}
</style>
