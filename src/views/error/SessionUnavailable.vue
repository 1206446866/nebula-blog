<template>
  <main class="session-unavailable">
    <span aria-hidden="true">✧</span>
    <h1>暂时无法确认登录状态</h1>
    <p>服务连接暂时不可用，原有登录信息已保留。稍后可以继续访问。</p>
    <div>
      <el-button type="primary" @click="retry">重新连接</el-button
      ><el-button @click="goLogin">返回登录</el-button>
    </div>
  </main>
</template>
<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { safeRedirect, loginLocation } from '@/router/policy'
const route = useRoute()
const router = useRouter()
const retry = () => router.replace(safeRedirect(route.query.redirect) || '/')
const goLogin = () => router.replace(loginLocation(safeRedirect(route.query.redirect) || '/'))
</script>
<style scoped>
.session-unavailable {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 24px;
  text-align: center;
  background: radial-gradient(ellipse at top, #1e293b, #020617 65%);
  color: #cbd5e1;
}
.session-unavailable > span {
  font-size: 60px;
  color: #a5b4fc;
}
.session-unavailable h1 {
  font-size: 24px;
  margin: 0;
}
.session-unavailable p {
  font-size: 14px;
  line-height: 1.8;
  color: #94a3b8;
  margin: 0 0 12px;
}
</style>
