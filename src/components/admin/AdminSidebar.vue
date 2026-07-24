<template>
  <div class="sidebar">
    <div class="logo">Nebula</div>

    <div class="menu">
      <router-link to="/admin" class="item" exact-active-class="active" active-class="">
        <ion-icon name="home-outline"></ion-icon>
        <span>首页</span>
      </router-link>

      <router-link to="/admin/user" class="item">
        <ion-icon name="people-outline"></ion-icon>
        <span>用户管理</span>
      </router-link>

      <router-link to="/admin/role" class="item">
        <ion-icon name="shield-outline"></ion-icon>
        <span>角色管理</span>
      </router-link>

      <router-link to="/admin/permission" class="item">
        <ion-icon name="lock-closed-outline"></ion-icon>
        <span>权限管理</span>
      </router-link>

      <router-link to="/admin/article" class="item">
        <ion-icon name="document-text-outline"></ion-icon>
        <span>文章管理</span>
      </router-link>

      <router-link to="/admin/comment" class="item">
        <ion-icon name="chatbubble-ellipses-outline"></ion-icon>
        <span>评论管理</span>
      </router-link>

      <router-link to="/admin/category" class="item">
        <ion-icon name="folder-outline"></ion-icon>
        <span>分类管理</span>
      </router-link>

      <router-link to="/admin/tag" class="item">
        <ion-icon name="pricetag-outline"></ion-icon>
        <span>标签管理</span>
      </router-link>
    </div>
  </div>
</template>
<script setup lang="ts">
defineOptions({
  name: 'AdminSidebar',
})
</script>
<style scoped>
.sidebar {
  width: 240px;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
  z-index: 2;
}

.logo {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 800;
  color: white;
}

.menu {
  padding: 16px;
}

.item {
  height: 48px;

  padding: 0 16px;

  display: flex;
  align-items: center;

  gap: 12px;

  border-radius: 12px;

  color: #cbd5e1;

  text-decoration: none;

  transition: 0.3s;
}

.item:hover {
  background: rgba(56, 189, 248, 0.1);
  color: #38bdf8;
}

.item.router-link-active:not(.router-link-exact-active) {
  background: rgba(56, 189, 248, 0.08);
  color: #cbd5e1;
}

.item.router-link-exact-active {
  background: rgba(56, 189, 248, 0.18);
  color: #38bdf8;
}
</style>
