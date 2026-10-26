<template>
  <section class="user-profile-page" :aria-busy="loading">
    <template v-if="profile">
      <ProfileHeader :profile="profile" :is-self="isSelf" @refresh="loadProfile" />
      <ProfileConnections :user-id="profile.userProfileInfoVO.id" />
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
import ProfileConnections from '@/components/community/ProfileConnections.vue'
import ProfileInfo from './ProfileInfo.vue'
import ProfileDetail from './ProfileDetail.vue'
import { RouterLink } from 'vue-router'

import { useUserProfile } from './logic/useUserProfile'

const { profile, isSelf, loading, errorMessage, errorCode, handlePageChange, loadProfile } =
  useUserProfile()
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
