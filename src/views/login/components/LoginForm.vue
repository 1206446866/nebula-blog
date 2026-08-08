<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.ts'
import { reactive, ref } from 'vue'
import { loginApi } from '@/api/auth.ts'
import { ElMessage } from 'element-plus'
import { Role } from '@/constants/role.ts'

defineOptions({
  name: 'LoginForm',
})

const emit = defineEmits<{
  register: []
}>()

const router = useRouter()
const userStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  account: '',
  password: '',
})

const handleLogin = async () => {
  if (!validateForm()) {
    return
  }
  const res = await loginApi(form)
  if (res.code !== 200 || !res.data) {
    ElMessage.error(res.message || '登录失败')
    return
  }
  userStore.setAuth(res.data)
  // TODO print
  console.log(res.data)
  if (userStore.hasRole(Role.ADMIN)) {
    router.push('/admin')
  } else {
    router.push('/')
  }
}

const validateForm = () => {
  if (!form.account.trim()) {
    ElMessage.warning('请输入账号或邮箱')
    return false
  }

  if (!form.password.trim()) {
    ElMessage.warning('请输入密码')
    return false
  }

  return true
}
</script>

<template>
  <div class="form-box">
    <h2>登录</h2>

    <el-input size="large" placeholder="NID / 邮箱" v-model="form.account" />

    <el-input
      class="input-item"
      size="large"
      type="password"
      placeholder="密码"
      v-model="form.password"
    />

    <div class="remember">
      <label>
        <input type="checkbox" />
        记住我
      </label>

      <a> 忘记密码? </a>
    </div>

    <el-button
      class="submit-button"
      type="primary"
      size="large"
      @click="handleLogin()"
      :loading="loading"
    >
      登录
    </el-button>

    <p class="switch">
      没有账号?
      <a @click="emit('register')"> 去注册 </a>
    </p>
  </div>
</template>

<style scoped>
.form-box {
  width: 100%;

  text-align: center;
}

h2 {
  margin-bottom: 30px;
}

.input-item {
  margin-top: 20px;
}

.submit-button {
  width: 100%;

  margin-top: 25px;
}

.remember {
  display: flex;

  justify-content: space-between;

  margin-top: 20px;

  font-size: 14px;
}

a {
  cursor: pointer;

  color: #38bdf8;
}

.switch {
  margin-top: 25px;
}
</style>
