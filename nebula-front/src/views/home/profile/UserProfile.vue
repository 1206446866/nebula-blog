<template>
  <ProfileHeader :profile="profile" :is-self="isSelf" @refresh="loadProfile" />
  <ProfileInfo :profile="profile" :is-self="isSelf" />
  <ProfileDetail :profile="profile" :is-self="isSelf" @page-change="handlePageChange" />
</template>

<script setup lang="ts">
import ProfileHeader from '@/views/home/profile/ProfileHeader.vue'
import ProfileInfo from '@/views/home/profile/ProfileInfo.vue'
import ProfileDetail from '@/views/home/profile/ProfileDetail.vue'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ref, onMounted } from 'vue'
import { getProfileApi, type UserProfileVO } from '@/api/profile'
import { useAuthStore } from '@/stores/auth.ts'

const route = useRoute()
const userStore = useAuthStore()
const userId = computed(() => {
  const routeId = route.params.userId
  return routeId ? Number(routeId) : userStore.user?.id
})
const isSelf = computed(() => userStore.user.id === profile.value?.userProfileInfoVO.id)
const profile = ref<UserProfileVO | null>(null)
const loading = ref(false)

const publicArticlePage = ref(1)
const draftArticlePage = ref(1)
const commentPage = ref(1)

function handlePageChange(type: string, page: number) {
  switch (type) {
    case 'article':
      publicArticlePage.value = page
      break

    case 'draft':
      draftArticlePage.value = page
      break

    case 'comment':
      commentPage.value = page
      break
  }
  loadProfile()
}

const loadProfile = async () => {
  loading.value = true
  try {
    const res = await getProfileApi(userId.value, {
      publicArticlePage: publicArticlePage.value,
      publicArticleSize: 5,
      draftArticlePage: draftArticlePage.value,
      draftArticleSize: 5,
      commentPage: commentPage.value,
      commentSize: 5,
    })
    profile.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>
