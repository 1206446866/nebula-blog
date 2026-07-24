<template>
  <div class="profile-info">
    <!-- 顶部 -->
    <div class="top-section">
      <!-- 基本信息 -->
      <div class="glass-card info-card">
        <h3 class="card-title">基本信息</h3>

        <div class="info-list">
          <div class="info-item">
            <span class="label">邮箱</span>
            <span class="value">{{ props.profile?.userProfileInfoVO.email }}</span>
          </div>

          <div class="info-item">
            <span class="label">手机号</span>
            <span class="value">{{ props.profile?.userProfileInfoVO.phone }}</span>
          </div>

          <div class="info-item">
            <span class="label">注册时间</span>
            <span class="value">{{ props.profile?.userProfileInfoVO.createTime }}</span>
          </div>

          <div class="info-item">
            <span class="label">最后登录</span>
            <span class="value">2026-06-15 22:30</span>
          </div>
        </div>
      </div>

      <!-- 安全设置 -->
      <div class="glass-card security-card" v-if="isSelf">
        <h3 class="card-title">安全设置</h3>

        <div class="security-grid">
          <div class="action-card" @click="openPasswordDialog">
            <ion-icon name="lock-closed-outline" />
            <span>修改密码</span>
          </div>

          <div class="action-card" @click="logout">
            <ion-icon name="log-out-outline" />
            <span>退出登录</span>
          </div>

          <div class="action-card danger" @click="deleteAccount">
            <ion-icon name="trash-outline" />
            <span>注销账号</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据概览 -->
    <div class="glass-card overview-card">
      <h3 class="card-title">数据概览</h3>
      <div class="overview-grid">
        <div class="stat-card">
          <ion-icon name="document-text-outline" />
          <div class="value">{{ props.profile?.statisticsVO.articleCount }}</div>
          <div class="label">文章</div>
        </div>

        <div class="stat-card">
          <ion-icon name="chatbubble-outline" />
          <div class="value">{{ props.profile?.statisticsVO.commentCount }}</div>
          <div class="label">评论</div>
        </div>

        <div class="stat-card">
          <ion-icon name="eye-outline" />
          <div class="value">{{ props.profile?.statisticsVO.totalViewCount }}</div>
          <div class="label">浏览</div>
        </div>

        <div class="stat-card">
          <ion-icon name="heart-outline" />
          <div class="value">{{ props.profile?.statisticsVO.likeCount }}</div>
          <div class="label">点赞</div>
        </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px" class="glass-dialog">
    <el-form :model="pwdForm">
      <el-form-item label="旧密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>

      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>

      <el-form-item label="确认密码">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitChangePassword">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth.ts'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePasswordApi } from '@/api/auth.ts'
defineOptions({
  name: 'ProfileInfo',
})
const props = defineProps<{
  profile: UserProfileVO | null
  isSelf: boolean
}>()

const authStore = useAuthStore()
const passwordDialogVisible = ref(false)

const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const resetForm = () => {
  pwdForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  }
}
const openPasswordDialog = () => {
  passwordDialogVisible.value = true
}
const submitChangePassword = async () => {
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  const res = await changePasswordApi({
    oldPassword: pwdForm.value.oldPassword,
    newPassword: pwdForm.value.newPassword,
    confirmPassword: pwdForm.value.confirmPassword,
  })
  if (res.code === 200) {
    resetForm()
    passwordDialogVisible.value = false
    ElMessage.success('修改成功，请重新登录')
  } else {
    ElMessage.error(res.message)
  }
}
import router from '@/router'
import type { UserProfileVO } from '@/api/profile.ts'

const logout = () => {
  router.replace('/login')
  authStore.logout()
}
const deleteAccount = () => {
  ElMessage.warning('功能开发中')
}
</script>

<style scoped>
.profile-info {
  width: 80%;
  margin: 24px auto 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ========================= */
/* 通用玻璃卡 */
/* ========================= */

.glass-card {
  background: rgba(15, 23, 42, 0.55);

  backdrop-filter: blur(20px);

  border: 1px solid rgba(255, 255, 255, 0.08);

  border-radius: 20px;

  padding: 24px;
}

.card-title {
  margin: 0 0 20px;

  font-size: 18px;
  font-weight: 600;

  color: #ffffff;
}

/* ========================= */
/* 顶部区域 */
/* ========================= */

.top-section {
  display: grid;
  //grid-template-columns: 1fr 1fr;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.info-card,
.security-card {
  min-height: 260px;
}

/* ========================= */
/* 基本信息 */
/* ========================= */

.info-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.label {
  color: #94a3b8;
  font-size: 14px;
}

.value {
  color: #ffffff;
  font-weight: 500;
}

/* ========================= */
/* 安全设置 */
/* ========================= */

.security-grid {
  display: grid;

  grid-template-columns: repeat(2, 1fr);

  gap: 14px;
}

.action-card {
  height: 90px;

  display: flex;
  flex-direction: column;

  justify-content: center;
  align-items: center;

  gap: 10px;

  border-radius: 16px;

  cursor: pointer;

  background: rgba(255, 255, 255, 0.04);

  transition: all 0.25s ease;
}

.action-card:hover {
  transform: translateY(-3px);

  background: rgba(56, 189, 248, 0.12);
}

.action-card ion-icon {
  font-size: 26px;
}

.action-card span {
  font-size: 14px;
}

.danger:hover {
  background: rgba(239, 68, 68, 0.15);

  color: #ef4444;
}

/* 注销账号占整行 */
.security-grid .danger {
  grid-column: span 2;
}

/* ========================= */
/* 数据概览 */
/* ========================= */

.overview-grid {
  display: grid;

  grid-template-columns: repeat(4, 1fr);

  gap: 16px;
}

.stat-card {
  height: 140px;

  display: flex;
  flex-direction: column;

  justify-content: center;
  align-items: center;

  border-radius: 18px;

  background: rgba(255, 255, 255, 0.03);

  transition: all 0.25s ease;
}

.stat-card:hover {
  transform: translateY(-4px);

  background: rgba(56, 189, 248, 0.08);
}

.stat-card ion-icon {
  font-size: 28px;
  margin-bottom: 10px;
  color: #38bdf8;
}

.stat-card .value {
  font-size: 28px;

  font-weight: 700;

  color: #ffffff;
}

.stat-card .label {
  margin-top: 6px;

  color: #94a3b8;
}
/* =========================
   Profile Page Layout
========================= */

.profile-info {
  width: 80%;
  margin: 24px auto 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* =========================
   Glass Card
========================= */

.glass-card {
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 24px;
}

/* =========================
   Dialog 终极修复版（100%生效）
========================= */

/* 遮罩层 */
.el-overlay {
  background: rgba(0, 0, 0, 0.65) !important;
  backdrop-filter: blur(6px);
}

/* dialog 主体（重点：不能依赖 scoped） */
:global(.el-dialog.glass-dialog) {
  background: rgba(15, 23, 42, 0.85) !important;
  backdrop-filter: blur(30px);

  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 22px;

  overflow: hidden;

  box-shadow:
    0 25px 80px rgba(0, 0, 0, 0.65),
    0 0 0 1px rgba(56, 189, 248, 0.08);
}

/* header */
:global(.el-dialog.glass-dialog .el-dialog__header) {
  padding: 18px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

/* title */
:global(.el-dialog.glass-dialog .el-dialog__title) {
  color: #ffffff;
  font-weight: 600;
}

/* body */
:global(.el-dialog.glass-dialog .el-dialog__body) {
  padding: 24px;
}

/* footer */
:global(.el-dialog.glass-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
</style>
