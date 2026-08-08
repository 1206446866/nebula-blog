<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import LoginForm from './components/LoginForm.vue'
import RegisterSelect from './components/RegisterSelect.vue'
import EmailRegisterForm from './components/EmailRegisterForm.vue'
import PhoneRegisterForm from './components/PhoneRegisterForm.vue'
import StarBackground from '@/components/StarBackground.vue'

defineOptions({
  name: 'AuthIndex',
})

type Mode = 'login' | 'register-select' | 'email-register' | 'phone-register'
const emailCaptchaExpireTime = ref(0)
const mode = ref<Mode>('login')
const direction = ref<'next' | 'prev'>('next')
const componentMap = {
  login: LoginForm,

  'register-select': RegisterSelect,

  'email-register': EmailRegisterForm,

  'phone-register': PhoneRegisterForm,
}
const registerForm = reactive({
  email: '',
  password: '',
  code: '',
})
const registerProps = computed(() => {
  if (mode.value !== 'email-register') {
    return {}
  }

  return {
    registerForm,
    emailCaptchaExpireTime,
  }
})
const updateRegisterForm = (value: Partial<typeof registerForm>) => {
  Object.assign(registerForm, value)
}
const currentComponent = computed(() => {
  return componentMap[mode.value]
})

const changeMode = (value: Mode) => {
  const order: Mode[] = ['login', 'register-select', 'email-register', 'phone-register']

  const currentIndex = order.indexOf(mode.value)

  const targetIndex = order.indexOf(value)

  direction.value = targetIndex > currentIndex ? 'next' : 'prev'

  mode.value = value
}
</script>

<template>
  <div class="auth-container">
    <StarBackground />
    <div class="glass-card">
      <Transition :name="direction === 'next' ? 'slide-next' : 'slide-prev'" mode="out-in">
        <component
          :is="currentComponent"
          v-bind="registerProps"
          :register-form="registerForm"
          @update:register-form="updateRegisterForm"
          :email-captcha-expire-time="emailCaptchaExpireTime"
          @update:email-captcha-expire-time="emailCaptchaExpireTime = $event"
          @register="changeMode('register-select')"
          @email-register="changeMode('email-register')"
          @phone-register="changeMode('phone-register')"
          @login="changeMode('login')"
          @back="changeMode('register-select')"
        />
      </Transition>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  min-height: 100vh;

  display: flex;

  justify-content: center;

  align-items: center;

  overflow: hidden;

  position: relative;

  background: radial-gradient(circle at top, #1e293b 0%, #020617 60%);
}

.glass-card {
  width: 420px;

  padding: 45px;

  background: transparent;

  border: 2px solid rgba(255, 255, 255, 0.35);

  border-radius: 20px;

  backdrop-filter: blur(20px);

  -webkit-backdrop-filter: blur(20px);

  box-shadow: 0 0 30px rgba(0, 0, 0, 0.5);

  overflow: hidden;

  transition:
    width 0.2s ease,
    height 0.2s ease;
}

.slide-next-enter-active,
.slide-next-leave-active {
  transition: 0.1s ease;
}

.slide-next-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.slide-next-leave-to {
  opacity: 0;
  transform: translateX(-40px);
}

.slide-prev-enter-active,
.slide-prev-leave-active {
  transition: 0.1s ease;
}

.slide-prev-enter-from {
  opacity: 0;
  transform: translateX(-40px);
}

.slide-prev-leave-to {
  opacity: 0;
  transform: translateX(40px);
}
</style>
