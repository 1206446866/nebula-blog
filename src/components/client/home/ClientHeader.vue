<template>
  <header class="client-header">
    <!-- 左侧 Logo -->
    <div class="left">
      <div class="logo" @click="goHome">Nebula</div>

      <nav class="nav">
        <router-link to="/" class="item">首页</router-link>
        <router-link to="/article" class="item">文章</router-link>
        <router-link to="/category" class="item">分类</router-link>
        <router-link to="/tag" class="item">标签</router-link>
      </nav>
    </div>

    <!-- 右侧用户区 -->
    <div class="right" ref="rightRef">
      <template v-if="!isLogin">
        <button class="btn" @click="goLogin">登录</button>
      </template>

      <template v-else>
        <div class="user" @click="toggleMenu">
          <img class="avatar" :src="avatar" alt="username" />
          <span>{{ username }}</span>
        </div>

        <div v-if="menuVisible" class="dropdown">
          <div class="item" @click="goProfile">个人主页</div>
          <div class="item" @click="logout">退出登录</div>
        </div>
      </template>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUserId } from '@/utils/auth'
import { useAuthStore } from '@/stores/auth.ts'

const router = useRouter()

const authStore = useAuthStore()
const isLogin = computed(() => !!authStore.getToken())
const username = computed(() => authStore.user.username)
const avatar = computed(() => authStore.getAvatar())
const rightRef = ref<HTMLElement>()
/** 菜单 */
const menuVisible = ref(false)

const toggleMenu = () => {
  menuVisible.value = !menuVisible.value
}

/** 跳转 */
const goHome = () => router.push('/')

const goLogin = () => router.push('/login')

const goProfile = () => {
  router.push(`/user/${getCurrentUserId()}`)
  menuVisible.value = false
}

const logout = () => {
  localStorage.clear()
  router.push('/login')
}

/** 点击外部关闭 */
const handleClickOutside = (e: MouseEvent) => {
  if (!rightRef.value?.contains(e.target as Node)) {
    menuVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.client-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 1000;
  padding: 0 24px;
  position: relative;
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(14px);

  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  color: #e5e7eb;
}

/* 左侧 */
.left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  font-size: 20px;
  font-weight: 800;
  color: #38bdf8;
  cursor: pointer;
}

.nav {
  display: flex;
  gap: 16px;
}

.item {
  color: #94a3b8;
  text-decoration: none;
  font-size: 14px;
  transition: 0.2s;
  cursor: pointer;
}

.item:hover {
  color: #38bdf8;
}

/* 右侧 */
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn {
  background: #38bdf8;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  color: #0f172a;
  cursor: pointer;
}

/* 用户 */
.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

/* 下拉 */
.dropdown {
  position: absolute;
  top: 64px;
  right: 24px;
  z-index: 9999;
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 8px;
  overflow: hidden;
}

.item {
  padding: 10px 14px;
  cursor: pointer;
}

.item:hover {
  background: rgba(56, 189, 248, 0.1);
}
</style>
