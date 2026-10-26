<template>
  <header class="client-header" :class="{ 'is-reading': isReading, 'is-stardust': isStardust }">
    <!-- 左侧标识 -->
    <div class="left">
      <div class="logo" @click="goHome">Nebula</div>

      <nav class="nav">
        <router-link to="/" class="item">首页</router-link>
        <router-link v-if="clientFeatures.community" to="/community" class="item">社区</router-link>
        <router-link v-if="authStore.hasPermission('article:list')" to="/article" class="item"
          >文章</router-link
        >
        <router-link v-if="authStore.hasPermission('category:list')" to="/category" class="item"
          >分类</router-link
        >
        <router-link v-if="authStore.hasPermission('tag:list')" to="/tag" class="item"
          >标签</router-link
        >
      </nav>
    </div>

    <HeaderSearch />

    <!-- 右侧用户区 -->
    <div class="right" ref="rightRef">
      <template v-if="!isLogin">
        <button class="btn" @click="goLogin">登录</button>
      </template>

      <template v-else>
        <router-link
          v-if="authStore.hasPermission('article:create')"
          to="/write"
          class="write-link"
          aria-label="写文章"
          title="写文章"
        >
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="m14 5 5 5M4 20l5-1L20 8a2.1 2.1 0 0 0-5-5L4 14Z" />
          </svg>
          <span>写文章</span>
        </router-link>
        <div class="user" @click="toggleMenu">
          <img
            v-if="avatar && !avatarFailed"
            class="avatar"
            :src="avatar"
            :alt="username"
            @error="avatarFailed = true"
          />
          <span v-else class="avatar avatar-fallback" aria-hidden="true">{{
            username.charAt(0)
          }}</span>
          <span>{{ username }}</span>
        </div>

        <div v-if="menuVisible" class="dropdown">
          <div class="item" @click="goProfile">个人主页</div>
          <router-link to="/media" class="item" @click="menuVisible = false">媒体库</router-link>
          <router-link
            v-if="authStore.hasPermission('article:create')"
            to="/write"
            class="item menu-write"
            @click="menuVisible = false"
            >写文章</router-link
          >
          <div class="item" @click="logout">
            {{ authStore.loggingOut ? '正在退出…' : '退出登录' }}
          </div>
        </div>
      </template>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUserId } from '@/utils/auth'
import { useAuthStore } from '@/stores/auth.ts'
import HeaderSearch from './HeaderSearch.vue'
import { clientFeatures } from '@/constants/client-features'

const router = useRouter()
const isReading = computed(() => router.currentRoute.value.name === 'ArticleDetail')
const isStardust = computed(() => router.currentRoute.value.path === '/tag')

const authStore = useAuthStore()
const isLogin = computed(() => !!authStore.getToken())
const username = computed(() => authStore.user?.username || '用户')
const avatar = computed(() => authStore.getAvatar())
const avatarFailed = ref(false)
watch(avatar, () => {
  avatarFailed.value = false
})
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

const logout = async () => {
  if (await authStore.logout()) await router.replace('/login')
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
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.client-header {
  height: 64px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(200px, 320px) minmax(0, 1fr);
  column-gap: 24px;
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

/* 标签星环页使用透明导航，让深空背景延伸到页面顶部。 */
.client-header.is-stardust {
  position: absolute;
  inset: 0 0 auto;
  height: 88px;
  padding-inline: 6%;
  background: transparent;
  border-bottom-color: #bda6741a;
  backdrop-filter: none;
}
.is-stardust .logo {
  color: #e6ddc9;
  font-weight: 500;
  letter-spacing: 0.06em;
}
.is-stardust .nav {
  gap: 8px;
}
.is-stardust .nav .item {
  color: #9b978c;
  font-size: 12px;
}
.is-stardust .nav .item.router-link-active {
  color: #e4d4ae;
}
.is-stardust .btn {
  background: #bda67418;
  border: 1px solid #bda67440;
  color: #d6c5a4;
  border-radius: 3px;
  font-size: 11px;
}

/* 左侧 */
.left {
  flex: 1;
  min-width: 0;
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
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
  white-space: nowrap;
  display: flex;
  gap: 16px;
}
.nav::-webkit-scrollbar {
  display: none;
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
  justify-self: end;
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

.write-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 13px;
  border: 1px solid rgba(125, 211, 252, 0.24);
  border-radius: 9px;
  color: #bae6fd;
  background: rgba(56, 189, 248, 0.08);
  font-size: 13px;
  text-decoration: none;
  white-space: nowrap;
}

.write-link:hover {
  background: rgba(56, 189, 248, 0.16);
}

.write-link svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.write-link:focus-visible {
  outline: 2px solid #7dd3fc;
  outline-offset: 3px;
}

.menu-write {
  display: block;
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

.user .avatar-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #40466b, #243a50);
  color: #c5d3ef;
  font-size: 13px;
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

@media (max-width: 650px) {
  .client-header {
    padding: 0 16px;
    gap: 12px;
  }
  .left {
    min-width: 0;
    gap: 14px;
  }
  .logo {
    font-size: 18px;
  }
  .nav {
    gap: 2px;
  }
  .nav .item {
    padding: 8px;
    white-space: nowrap;
  }
  .right {
    flex-shrink: 0;
  }
  .user span {
    display: none;
  }
  .write-link {
    padding: 8px;
  }
  .write-link span {
    display: none;
  }
  .btn {
    padding: 6px 8px;
    white-space: nowrap;
  }
}

@media (max-width: 400px) {
  .client-header {
    padding: 0 10px;
    gap: 8px;
  }
  .left {
    gap: 8px;
  }
  .nav .item {
    padding: 8px 5px;
  }
  .right {
    gap: 6px;
  }
}

.client-header.is-reading {
  height: 74px;
  padding: 0 max(32px, calc((100vw - 1280px) / 2));
  background: #0a0d19ed;
  border-bottom-color: #c6afd51a;
  color: #d5c9df;
}
.is-reading .logo {
  color: #d2c4e1;
  font:
    22px Georgia,
    serif;
  letter-spacing: 0.04em;
}
.is-reading .logo::after {
  content: ' / journal';
  color: #766981;
  font:
    9px Consolas,
    monospace;
  letter-spacing: 0.04em;
  margin-left: 9px;
}
.is-reading .left {
  gap: 38px;
}
.is-reading .item {
  color: #93879f;
  font-size: 12px;
}
.is-reading .item:hover {
  color: #d3bee5;
  background: #a488bf0a;
}
.is-reading .write-link {
  color: #baa9cd;
  background: #9c82b00a;
  border-color: #b4a0c529;
  font-size: 11px;
}
.is-reading .user {
  color: #ad9dbb;
  font-size: 11px;
}
.is-reading .avatar {
  width: 29px;
  height: 29px;
}
.is-reading .avatar-fallback {
  background: linear-gradient(145deg, #65506d, #30454d);
  color: #dbcae8;
  font-size: 12px;
}
.is-reading .dropdown {
  top: 74px;
  right: max(32px, calc((100vw - 1280px) / 2));
  background: #11101f;
  border-color: #b4a0c529;
}
@media (max-width: 650px) {
  .client-header.is-reading {
    height: 64px;
    padding: 0 12px;
    gap: 10px;
  }
  .is-reading .logo {
    font-size: 19px;
  }
  .is-reading .logo::after {
    display: none;
  }
  .is-reading .left {
    gap: 12px;
  }
  .is-reading .nav .item {
    font-size: 11px;
    padding: 7px 5px;
  }
  .is-reading .right {
    gap: 8px;
  }
  .is-reading .write-link {
    padding: 6px;
  }
  .is-reading .dropdown {
    top: 64px;
    right: 12px;
  }
}
@media (max-width: 1200px) {
  .client-header,
  .client-header.is-reading {
    grid-template-columns: minmax(0, 1fr) auto;
    height: auto;
    min-height: 64px;
    padding-top: 10px;
    padding-bottom: 10px;
    row-gap: 10px;
  }
  .left {
    grid-column: 1;
    grid-row: 1;
  }
  .right {
    grid-column: 2;
    grid-row: 1;
  }
  .header-search {
    grid-column: 1 / -1;
    grid-row: 2;
    width: min(100%, 560px);
    justify-self: center;
    box-sizing: border-box;
  }
  .dropdown,
  .is-reading .dropdown {
    top: 54px;
  }
}
</style>
