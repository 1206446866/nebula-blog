<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { registerApi } from '@/api/auth.js'
import type { Result } from '@/types/result.ts'

const route = useRoute()
const router = useRouter()

type Status = 'loading' | 'success' | 'error'

const status = ref<Status>('loading')

const message = ref('正在确认注册信息...')

const handleRegister = async () => {
  const token = route.query.token as string

  if (!token) {
    status.value = 'error'
    message.value = '注册链接无效'
    return
  }

  try {
    await registerApi({
      token,
    })

    status.value = 'success'
    message.value = '账号注册成功'
  } catch (err: unknown) {
    const error = err as Result<boolean>
    status.value = 'error'
    message.value = error.message || '注册失败'
  }
}

const goLogin = () => {
  router.push('/login')
}

onMounted(() => {
  handleRegister()
})
</script>

<template>
  <div class="confirm-page">
    <div class="confirm-card">
      <template v-if="status === 'loading'">
        <h2>正在验证</h2>
        <p>{{ message }}</p>
      </template>

      <template v-else-if="status === 'success'">
        <h2>注册成功</h2>
        <p>{{ message }}</p>

        <el-button type="primary" @click="goLogin"> 去登录 </el-button>
      </template>

      <template v-else>
        <h2>注册失败</h2>
        <p>{{ message }}</p>

        <el-button type="primary" @click="goLogin"> 返回注册 </el-button>
      </template>
    </div>
  </div>
</template>
<style scoped>
.confirm-page {
  min-height: 100vh;

  display: flex;

  justify-content: center;

  align-items: center;

  background: radial-gradient(circle at top, #1e293b 0%, #020617 60%);
}

.confirm-card {
  width: 420px;

  padding: 45px;

  text-align: center;

  background: transparent;

  border: 2px solid rgba(255, 255, 255, 0.35);

  border-radius: 20px;

  backdrop-filter: blur(20px);

  -webkit-backdrop-filter: blur(20px);

  box-shadow: 0 0 30px rgba(0, 0, 0, 0.5);

  color: white;

  animation: fade-in 0.4s ease;
}

h2 {
  margin-bottom: 20px;

  font-size: 28px;

  letter-spacing: 2px;
}

p {
  margin-bottom: 30px;

  color: rgba(255, 255, 255, 0.75);

  font-size: 15px;
}

.el-button {
  width: 100%;

  height: 42px;

  border-radius: 10px;
}

/**
 * 状态动画
 */
.status-icon {
  width: 70px;

  height: 70px;

  margin: 0 auto 25px;

  display: flex;

  justify-content: center;

  align-items: center;

  border-radius: 50%;

  font-size: 36px;

  background: rgba(255, 255, 255, 0.1);
}

.loading {
  animation: rotate 1.5s linear infinite;
}

@keyframes fade-in {
  from {
    opacity: 0;

    transform: translateY(20px);
  }

  to {
    opacity: 1;

    transform: translateY(0);
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
