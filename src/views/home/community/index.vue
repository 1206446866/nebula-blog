<script setup lang="ts">
defineOptions({ name: 'CommunityPage' })

import CommunityUserList from '@/components/community/CommunityUserList.vue'

import { useCommunityPage } from './logic/useCommunityPage'

const { userId, mode, search, keyword, tabs } = useCommunityPage()
</script>

<template>
  <div class="community-page">
    <header class="community-intro">
      <div>
        <span class="eyebrow">NEBULA COMMUNITY</span>
        <h1>在这里，遇见同频的人</h1>
        <p>关注感兴趣的作者，让阅读成为交流的开始。</p>
      </div>
      <RouterLink :to="`/user/${userId}`">我的主页 ↗</RouterLink>
    </header>
    <section class="community-panel">
      <div class="community-toolbar">
        <div class="community-tabs" role="group" aria-label="社区用户列表">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            :class="{ active: mode === tab.value }"
            :aria-pressed="mode === tab.value"
            @click="mode = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
        <form
          v-if="mode === 'discover'"
          class="community-search"
          @submit.prevent="keyword = search.trim()"
        >
          <input
            v-model="search"
            maxlength="50"
            aria-label="搜索昵称或NID"
            placeholder="搜索昵称或 NID"
            type="search"
          />
          <button type="submit">搜索</button>
        </form>
      </div>
      <CommunityUserList :mode="mode" :user-id="userId" :keyword="keyword" />
    </section>
  </div>
</template>

<style scoped>
.community-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 48px 24px 60px;
  min-height: calc(100dvh - 64px);
  color: #e2e8f0;
}
.community-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 30px;
}
.eyebrow {
  font-size: 11px;
  color: #a5b4fc;
  letter-spacing: 0.16em;
}
h1 {
  font-size: clamp(23px, 4vw, 32px);
  font-weight: 600;
  margin: 12px 0;
}
.community-intro p {
  color: #94a3b8;
  font-size: 14px;
  line-height: 1.7;
}
.community-intro a {
  color: #a5deff;
  font-size: 13px;
  text-decoration: none;
  white-space: nowrap;
}
.community-panel {
  border-radius: 20px;
  border: 1px solid #b6c6eb20;
  padding: 24px;
  background: linear-gradient(130deg, #18233b99, #231b3480 70%, #162c3380);
}
.community-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}
.community-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.community-tabs button {
  font: inherit;
  font-size: 14px;
  border: 1px solid transparent;
  padding: 10px 14px;
  border-radius: 9px;
  background: none;
  color: #94a3b8;
  cursor: pointer;
}
.community-tabs .active {
  background: #a78bfa18;
  color: #d2c5ff;
  border-color: #a78bfa38;
}
.community-search {
  display: flex;
  gap: 8px;
  min-width: 0;
}
.community-search input {
  min-width: 0;
  width: 180px;
  border: 1px solid #94a3b838;
  border-radius: 8px;
  background: #0f172a88;
  padding: 10px 12px;
  color: #e2e8f0;
  font: inherit;
  font-size: 13px;
}
.community-search button {
  border: 1px solid #7dd3fc35;
  border-radius: 8px;
  background: #38bdf818;
  color: #a5deff;
  padding: 9px 14px;
  cursor: pointer;
}
@media (max-width: 600px) {
  .community-page {
    padding: 28px 16px;
  }
  .community-intro {
    align-items: flex-start;
    flex-direction: column;
  }
  .community-panel {
    padding: 16px;
  }
  .community-search,
  .community-search input {
    flex: 1;
  }
}
</style>
