<template>
  <div class="profile-info">
    <!-- 顶部 -->
    <div class="top-section">
      <!-- 基本信息 -->
      <div class="glass-card info-card">
        <h3 class="card-title">基本信息</h3>

        <div class="info-list">
          <div v-if="isSelf" class="info-item">
            <span class="label">邮箱</span>
            <span class="value">{{ props.profile?.userProfileInfoVO.email || '未绑定' }}</span>
          </div>

          <div v-if="isSelf" class="info-item">
            <span class="label">手机号</span>
            <span class="value">{{ props.profile?.userProfileInfoVO.phone || '未绑定' }}</span>
          </div>

          <div class="info-item">
            <span class="label">注册时间</span>
            <span class="value">{{
              formatDateTime(props.profile?.userProfileInfoVO.createTime) || '—'
            }}</span>
          </div>

          <div v-if="isSelf" class="info-item">
            <span class="label">最后登录</span>
            <span class="value">{{ lastLogin || '暂无记录' }}</span>
          </div>
        </div>
      </div>

      <!-- 安全设置 -->
      <div class="glass-card security-card" v-if="isSelf">
        <h3 class="card-title">安全设置</h3>

        <div class="security-grid">
          <button type="button" class="action-card" @click="openPasswordDialog">
            <ion-icon name="lock-closed-outline" />
            <span>修改密码</span>
          </button>

          <button
            type="button"
            class="action-card"
            :disabled="authStore.loggingOut"
            @click="logout"
          >
            <ion-icon name="log-out-outline" />
            <span>{{ authStore.loggingOut ? '正在退出…' : '退出登录' }}</span>
          </button>

          <button type="button" class="action-card danger" @click="openCancellationDialog">
            <ion-icon name="trash-outline" />
            <span>注销账号</span>
          </button>
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

  <el-dialog
    v-model="passwordDialogVisible"
    title="修改密码"
    width="min(420px, calc(100vw - 32px))"
    class="glass-dialog"
    @closed="resetForm"
  >
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
      <el-button type="primary" :loading="passwordSubmitting" @click="submitChangePassword"
        >确定</el-button
      >
    </template>
  </el-dialog>

  <el-dialog
    v-model="cancellationDialogVisible"
    title="注销账号"
    width="min(460px, calc(100vw - 32px))"
    class="glass-dialog cancellation-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="!cancelling"
    :show-close="!cancelling"
    @closed="resetCancellationForm"
  >
    <div class="cancellation-notice">
      <strong>确认离开 Nebula？</strong>
      <p>注销后，你将无法再登录此账号，个人主页也将关闭。此操作无法在页面中撤回。</p>
      <p>已发布的文章与评论会保留。</p>
    </div>
    <el-form :model="cancellationForm" label-position="top" @submit.prevent="submitCancellation">
      <el-form-item label="当前密码">
        <el-input
          v-model="cancellationForm.password"
          type="password"
          autocomplete="current-password"
          placeholder="输入当前密码以验证身份"
          show-password
          :disabled="cancelling"
        />
      </el-form-item>
      <el-checkbox
        v-model="cancellationForm.confirmed"
        :disabled="cancelling"
        class="cancellation-confirm"
      >
        我已了解上述影响，确认注销当前账号
      </el-checkbox>
    </el-form>
    <template #footer>
      <el-button :disabled="cancelling" @click="cancellationDialogVisible = false"
        >保留账号</el-button
      >
      <el-button
        type="danger"
        :loading="cancelling"
        :disabled="!cancellationForm.confirmed"
        @click="submitCancellation"
      >
        确认注销
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth.ts'
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { changePasswordApi } from '@/api/auth.ts'
import { cancelSelfAccountApi } from '@/api/user.ts'
import router from '@/router'
import type { UserProfileVO } from '@/api/profile.ts'
import { formatDateTime } from '@/utils/date.ts'

defineOptions({
  name: 'ProfileInfo',
})
const props = defineProps<{
  profile: UserProfileVO | null
  isSelf: boolean
}>()

const authStore = useAuthStore()
const passwordDialogVisible = ref(false)
const passwordSubmitting = ref(false)
const cancellationDialogVisible = ref(false)
const cancelling = ref(false)
const cancellationForm = ref({ password: '', confirmed: false })
const lastLogin = computed(() =>
  formatDateTime(props.profile?.loginLogs?.find((record) => record.status === 1)?.createTime),
)
watch(
  () => props.profile?.userProfileInfoVO.id,
  () => {
    passwordDialogVisible.value = false
    resetForm()
    cancellationDialogVisible.value = false
    resetCancellationForm()
  },
)

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
  resetForm()
  passwordDialogVisible.value = true
}
const submitChangePassword = async () => {
  if (passwordSubmitting.value) return
  if (
    !pwdForm.value.oldPassword.trim() ||
    !pwdForm.value.newPassword.trim() ||
    !pwdForm.value.confirmPassword.trim()
  ) {
    ElMessage.warning('请填写旧密码、新密码和确认密码')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  passwordSubmitting.value = true
  try {
    const res = await changePasswordApi({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword,
      confirmPassword: pwdForm.value.confirmPassword,
    })
    if (res.code === 200 && res.data) {
      resetForm()
      passwordDialogVisible.value = false
      ElMessage.success('修改成功，请重新登录')
      authStore.clearAuth()
      await router.replace('/login')
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    // Business errors are already displayed by the request interceptor.
  } finally {
    passwordSubmitting.value = false
  }
}

const logout = async () => {
  if (await authStore.logout()) await router.replace('/login')
}
const resetCancellationForm = () => {
  cancellationForm.value = { password: '', confirmed: false }
}
const openCancellationDialog = () => {
  resetCancellationForm()
  cancellationDialogVisible.value = true
}
const submitCancellation = async () => {
  if (cancelling.value || !props.isSelf) return
  if (!cancellationForm.value.password.trim()) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!cancellationForm.value.confirmed) {
    ElMessage.warning('请先确认已了解注销影响')
    return
  }
  cancelling.value = true
  try {
    const result = await cancelSelfAccountApi({ ...cancellationForm.value })
    if (result.code === 200 && result.data) {
      resetCancellationForm()
      cancellationDialogVisible.value = false
      authStore.clearAuth()
      await router.replace('/login')
      ElMessage.success('账号已注销')
    } else {
      ElMessage.error(result.message || '注销失败，请重试')
    }
  } catch {
    // The request interceptor displays the server's validation or permission error.
  } finally {
    cancelling.value = false
  }
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
  border: 0;
  color: inherit;
  font: inherit;
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

.action-card:focus-visible {
  outline: 2px solid #7dd3fc;
  outline-offset: 3px;
}
.cancellation-notice {
  margin-bottom: 22px;
  padding: 16px;
  border: 1px solid #f8717133;
  border-radius: 12px;
  background: #f871710a;
  color: #cbd5e1;
  line-height: 1.7;
  font-size: 13px;
}
.cancellation-notice strong {
  color: #fca5a5;
  font-size: 15px;
}
.cancellation-notice p {
  margin: 8px 0 0;
}
.cancellation-confirm {
  height: auto;
  align-items: flex-start;
  white-space: normal;
}
.cancellation-confirm :deep(.el-checkbox__label) {
  white-space: normal;
  line-height: 1.5;
  color: #b7c6db;
}
:global(.cancellation-dialog .el-form-item__label) {
  color: #cbd5e1;
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
@media (max-width: 1050px) {
  .profile-info {
    width: 90%;
  }
}
@media (max-width: 760px) {
  .profile-info {
    width: calc(100% - 32px);
    gap: 16px;
  }
  .top-section {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }
  .glass-card {
    padding: 20px;
    border-radius: 16px;
  }
  .info-card,
  .security-card {
    min-height: 0;
  }
  .info-item {
    gap: 18px;
    align-items: flex-start;
  }
  .info-item .value {
    text-align: right;
    overflow-wrap: anywhere;
    min-width: 0;
  }
  .info-item .label {
    flex-shrink: 0;
  }
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
