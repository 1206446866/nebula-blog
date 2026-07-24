<template>
  <div class="header">
    <div class="left">
      <h1>Nebula</h1>
    </div>

    <div class="right">
      <div class="user">
        <ion-icon name="person-circle-outline"></ion-icon>

        <tag @click="profile">管理员</tag>
      </div>

      <el-button type="danger" size="small" @click="logout"> 退出登录 </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'AdminHeader',
})
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const userStore = useAuthStore()
const logout = () => {
  userStore.logout()
  router.replace('/login')
}

const profile = () => {
  router.push('/user/'+userStore.user.id)
}
</script>

<style scoped>
.header {
  height: 70px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.left h1 {
  font-size: 28px;
  font-weight: 800;

  background: linear-gradient(90deg, #38bdf8, #818cf8);

  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.right {
  display: flex;
  align-items: center;

  gap: 16px;

  color: white;
}

.user {
  display: flex;
  align-items: center;

  gap: 6px;

  font-size: 16px;
}
</style>
