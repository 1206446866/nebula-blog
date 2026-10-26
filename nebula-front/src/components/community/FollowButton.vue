<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { setFollowing, type FollowSummary, type Relationship } from '@/api/community'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ userId: number; relationship: Relationship }>()
const emit = defineEmits<{ changed: [summary: FollowSummary] }>()
const auth = useAuthStore()
const busy = ref(false)
let revision = 0
const label = computed(() => {
  if (props.relationship.self) return '你自己'
  if (busy.value) return '处理中…'
  if (props.relationship.following) return props.relationship.followedBy ? '互相关注' : '已关注'
  if (!props.relationship.canFollow) return '暂不可关注'
  return props.relationship.followedBy ? '回关' : '+ 关注'
})
watch(
  () => [props.userId, auth.getToken()],
  () => {
    revision++
    busy.value = false
  },
)
onBeforeUnmount(() => {
  revision++
})
async function toggle() {
  const relation = props.relationship
  if (busy.value || relation.self || (!relation.canFollow && !relation.following)) return
  const version = ++revision
  const id = props.userId
  const token = auth.getToken()
  busy.value = true
  try {
    const result = await setFollowing(id, !relation.following)
    if (version === revision && id === props.userId && token === auth.getToken())
      emit('changed', result.data)
  } catch {
    // 请求拦截器统一显示错误，失败时保留原关系供重试。
  } finally {
    if (version === revision) busy.value = false
  }
}
</script>

<template>
  <button
    type="button"
    class="follow-button"
    :class="{ following: relationship.following }"
    :disabled="busy || relationship.self || (!relationship.canFollow && !relationship.following)"
    :aria-pressed="relationship.following"
    :aria-busy="busy"
    :aria-label="relationship.following ? `${label}，点击取消关注` : label"
    @click="toggle"
  >
    {{ label }}
  </button>
</template>

<style scoped>
.follow-button {
  flex-shrink: 0;
  min-width: 94px;
  padding: 9px 15px;
  border: 1px solid #7dd3fc55;
  border-radius: 9px;
  background: #38bdf81c;
  color: #a5e4ff;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}
.follow-button:hover:not(:disabled) {
  background: #38bdf832;
}
.follow-button.following {
  border-color: #c4b5fd35;
  background: #a78bfa10;
  color: #c4b5fd;
}
.follow-button:disabled {
  opacity: 0.55;
  cursor: default;
}
.follow-button:focus-visible {
  outline: 2px solid #7dd3fc;
  outline-offset: 3px;
}
</style>
