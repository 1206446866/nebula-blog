<script setup lang="ts">
defineOptions({
  name: 'RegisterSelect',
})

const emit = defineEmits<{
  'email-register': []
  'phone-register': []
  login: []
}>()
</script>

<template>
  <div class="form-box">
    <h2>注册</h2>

    <div class="register-options">
      <div class="register-item" @click="emit('email-register')">
        <div class="icon">✉</div>

        <span> Email </span>
      </div>

      <div class="register-item" @click="emit('phone-register')">
        <div class="icon">☎</div>

        <span> Phone </span>
      </div>
    </div>

    <p class="switch">
      已有账号?

      <a @click="emit('login')"> 登录 </a>
    </p>
  </div>
</template>

<style scoped>
.form-box {
  width: 100%;

  text-align: center;
}

h2 {
  margin-bottom: 35px;
}

.register-options {
  display: flex;

  justify-content: center;

  gap: 30px;
}

.register-item {
  width: 200px;

  height: 130px;

  display: flex;

  flex-direction: column;

  justify-content: center;

  align-items: center;

  border-radius: 16px;

  cursor: pointer;

  background: rgba(255, 255, 255, 0.08);

  border: 1px solid rgba(255, 255, 255, 0.1);

  transition: 0.3s;
}

.register-item:hover {
  transform: translateY(-5px);

  background: rgba(255, 255, 255, 0.15);
}

.icon {
  font-size: 36px;

  margin-bottom: 15px;
}

.switch {
  margin-top: 30px;
}

a {
  color: #38bdf8;

  cursor: pointer;
}
</style>
