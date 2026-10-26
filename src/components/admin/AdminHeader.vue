<template>
  <div class="header">
    <div class="left">
      <h1>Nebula</h1>
    </div>

    <div class="right">
      <div class="user">
        <ion-icon name="person-circle-outline"></ion-icon>

        <button type="button" class="profile-link" @click="profile">
          {{ userStore.hasRole(Role.SUPER_ADMIN) ? '超级管理员' : '管理员' }}
        </button>
      </div>

      <el-button type="danger" size="small" :loading="userStore.loggingOut" @click="logout">
        退出登录
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Role } from '@/constants/role'

import { useAdminHeader } from './logic/useAdminHeader'

defineOptions({
  name: 'AdminHeader',
})

const { userStore, logout, profile } = useAdminHeader()
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

.profile-link {
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
}
</style>
