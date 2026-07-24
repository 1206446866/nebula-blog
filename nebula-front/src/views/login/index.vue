<script setup lang="ts">
import { ElMessage } from 'element-plus'

defineOptions({
  name: 'LoginPage',
})

import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import NebulaCard from '@/components/NebulaCard.vue'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { getCurrentRoleList } from '@/utils/auth.ts'

const router = useRouter()

const userStore = useAuthStore()

const loading = ref(false)

const form = reactive({
  nid: '',
  password: '',
})

const handleLogin = async () => {
  const res = await loginApi(form)
  if (res.code !== 200 || !res.data) {
    ElMessage.error(res.message || '登录失败')
    return
  }
  userStore.setAuth(res.data)
  // TODO print
  console.log(res.data)
  if (getCurrentRoleList().includes('ADMIN')) {
    router.push('/admin')
  } else {
    router.push('/')
  }
}
</script>

<template>
  <div class="login-container">
    <div class="background-glow glow-1"></div>
    <div class="background-glow glow-2"></div>

    <NebulaCard class="login-card">
      <div class="login-header">
        <h1>Nebula</h1>

        <p>Next Generation Tech Platform</p>
      </div>

      <el-form :model="form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.nid" size="large" placeholder="账号（NID）" />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="Password"
            show-password
          />
        </el-form-item>

        <el-button
          class="login-button"
          type="primary"
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
    </NebulaCard>
  </div>
</template>

<style scoped>
.login-container {
  position: relative;

  min-height: 100vh;

  display: flex;
  justify-content: center;
  align-items: center;

  overflow: hidden;

  background: radial-gradient(circle at top, #1e293b 0%, #020617 60%);
}

.login-card {
  width: 420px;

  padding: 48px;

  position: relative;
  z-index: 2;
}

.login-header {
  text-align: center;

  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 56px;
  font-weight: 800;

  margin-bottom: 12px;

  background: var(--nebula-gradient);

  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.login-header p {
  color: var(--nebula-text-secondary);
}

.login-button {
  width: 100%;

  margin-top: 12px;
}

.background-glow {
  position: absolute;

  width: 500px;
  height: 500px;

  border-radius: 50%;

  filter: blur(120px);

  opacity: 0.25;
}

.glow-1 {
  background: #38bdf8;

  top: -120px;
  left: -120px;
}

.glow-2 {
  background: #818cf8;

  bottom: -120px;
  right: -120px;
}
</style>
