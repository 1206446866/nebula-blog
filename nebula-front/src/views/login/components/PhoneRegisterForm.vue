<script setup lang="ts">
defineOptions({
  name: 'PhoneRegisterForm',
})

const emit = defineEmits<{
  back: []
}>()
</script>

<template>
  <div class="form-box">
    <h2>手机注册</h2>

    <el-input size="large" placeholder="手机号" />

    <el-input class="input-item" size="large" type="password" placeholder="密码" />

    <div class="captcha-box">
      <el-input size="large" placeholder="验证码" />

      <el-button> 发送验证码 </el-button>

    </div>

    <el-button class="submit-button" type="primary" size="large"> 注册 </el-button>

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
