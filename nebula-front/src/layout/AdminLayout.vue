<template>
  <div class="layout">
    <!-- 星空背景 -->
    <StarBackground />

    <!-- 左侧菜单 -->
    <AdminSidebar />

    <!-- 主区域 -->
    <div class="main">
      <!-- 顶部 -->
      <AdminHeader />

      <!-- 页面 -->
      <div class="content">
<!--        <GlobalLoading/>-->
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AdminSidebar from '../components/admin/AdminSidebar.vue'
import AdminHeader from '../components/admin/AdminHeader.vue'
import StarBackground from '../components/StarBackground.vue'
// import GlobalLoading from '@/layout/GlobalLoading.vue'


</script>

<style scoped>
.layout {
  display: flex;

  min-height: 100vh;

  background: #020617;

  position: relative;

  overflow: hidden;
}

.main {
  flex: 1;

  display: flex;
  flex-direction: column;

  position: relative;
  z-index: 2;
}

.content {
  flex: 1;

  padding: 24px;

  overflow-y: auto;
}
</style>
