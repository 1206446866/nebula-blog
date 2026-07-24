<template>
  <div class="error-page">
    <div class="error-content">
      <h1>404</h1>
      <p>该页面不存在哦</p>

      <div class="btn-group">
        <el-button @click="goBack">返回上级</el-button>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'NotFound404',
})
import { useRouter } from 'vue-router'

const router = useRouter()

// 回首页
const goHome = () => {
  router.push('/')
}

// 回上一级
const goBack = () => {
  router.back()
}
</script>

<style scoped>
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: radial-gradient(circle at top, #1e293b 0%, #020617 60%);
  color: #fff;
  text-align: center;
}

.error-content h1 {
  font-size: 120px;
  font-weight: bold;
  margin-bottom: 20px;
  background: linear-gradient(90deg, #38bdf8, #818cf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.error-content p {
  font-size: 20px;
  margin-bottom: 24px;
}

.btn-group {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
