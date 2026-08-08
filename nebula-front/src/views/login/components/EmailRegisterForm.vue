<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { sendEmailCaptchaApi, verifyEmailCaptchaApi } from '@/api/auth.ts'
import { ElMessage } from 'element-plus'

defineOptions({
  name: 'EmailRegisterForm',
})

const props = defineProps<{
  registerForm: {
    email: string
    password: string
    code: string
  }

  emailCaptchaExpireTime: number
}>()

const emit = defineEmits<{
  back: []
  'update:register-form': [value: Partial<typeof props.registerForm>]
  'update:email-captcha-expire-time': [value: number]
}>()

const form = reactive({
  email: props.registerForm.email,
  password: props.registerForm.password,
  code: props.registerForm.code,
})
const countdown = ref(0)
let timer: number | undefined
const sending = ref(false)
const sendCaptcha = async () => {
  if (!form.email) {
    ElMessage.warning('请输入邮箱')
    return
  }

  if (sending.value || countdown.value > 0) {
    return
  }

  sending.value = true

  try {
    const res = await sendEmailCaptchaApi({
      email: form.email,
    })
    if (res.data <= 0) {
      ElMessage.info(res.message || '验证码发送失败')
      return
    }
    ElMessage.success(res.message||'再等'+res.data+'s才可发送下一次验证码')

    const expireTime = Date.now() + res.data * 1000

    emit('update:email-captcha-expire-time', expireTime)

    startCountdown(expireTime)
  } finally {
    sending.value = false
  }
}

const register = async () => {
  if (!form.email) {
    ElMessage.warning('请输入邮箱')
    return
  }

  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }

  if (!form.code) {
    ElMessage.warning('请输入验证码')
    return
  }

  try {
    const res = await verifyEmailCaptchaApi({
      email: form.email,
      password: form.password,
      code: form.code,
    })

    ElMessage.success(res.message || '验证成功，请查看邮箱确认邮件')
  } catch (e) {
    console.error(e)
  }
}

const startCountdown = (expireTime: number) => {
  if (timer) {
    clearInterval(timer)
  }

  const update = () => {
    const remain = Math.floor((expireTime - Date.now()) / 1000)

    if (remain <= 0) {
      countdown.value = 0

      if (timer) {
        clearInterval(timer)
        timer = undefined
      }

      return
    }

    countdown.value = remain
  }

  update()

  timer = window.setInterval(update, 1000)
}

onMounted(() => {
  if (props.emailCaptchaExpireTime) {
    startCountdown(props.emailCaptchaExpireTime)
  }
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})

watch(
  form,
  (value) => {
    emit('update:register-form', {
      email: value.email,
      password: value.password,
      code: value.code,
    })
  },
  {
    deep: true,
  },
)
</script>

<template>
  <div class="form-box">
    <h2>邮箱注册</h2>

    <el-input size="large" placeholder="电子邮箱" v-model="form.email" />

    <el-input
      class="input-item"
      size="large"
      type="password"
      placeholder="密码"
      v-model="form.password"
    />

    <div class="captcha-box">
      <el-input size="large" placeholder="验证码" v-model="form.code" />

      <el-button :disabled="sending || countdown > 0" @click="sendCaptcha">
        {{ sending ? '发送中...' : countdown > 0 ? `${countdown}s后重新发送` : '发送验证码' }}
      </el-button>
    </div>

    <el-button class="submit-button" type="primary" size="large" @click="register">
      注册
    </el-button>

    <p class="switch">
      <a @click="emit('back')"> 返回选择方式 </a>
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

.captcha-box {
  display: flex;

  gap: 12px;

  margin-top: 20px;
}

.captcha-box .el-input {
  flex: 1;
}

.submit-button {
  width: 100%;

  margin-top: 25px;
}

.switch {
  margin-top: 25px;
}

a {
  color: #38bdf8;

  cursor: pointer;
}
</style>
