import { changePasswordApi } from '@/api/auth.ts'
import type { UserProfileVO } from '@/api/profile.ts'
import { cancelSelfAccountApi } from '@/api/user.ts'
import router from '@/router'
import { useAuthStore } from '@/stores/auth.ts'
import { formatDateTime } from '@/utils/date.ts'
import { isValidNewPassword, newPasswordMessage } from '@/utils/password-policy'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'

/** 管理个人资料与账号操作的状态与交互。 */
export function useProfileInfo(
  props: Readonly<{
    profile: UserProfileVO | null
    isSelf: boolean
  }>,
) {
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
    if (!isValidNewPassword(pwdForm.value.newPassword)) {
      ElMessage.warning(newPasswordMessage)
      return
    }
    passwordSubmitting.value = true
    const token = authStore.getToken()
    try {
      const res = await changePasswordApi({
        oldPassword: pwdForm.value.oldPassword,
        newPassword: pwdForm.value.newPassword,
        confirmPassword: pwdForm.value.confirmPassword,
      })
      if (authStore.getToken() !== token) return
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
      // 业务错误已由请求拦截器统一显示。
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
    const token = authStore.getToken()
    try {
      const result = await cancelSelfAccountApi({ ...cancellationForm.value })
      if (authStore.getToken() !== token) return
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
      // 服务端返回的校验或权限错误由请求拦截器统一显示。
    } finally {
      cancelling.value = false
    }
  }

  return {
    authStore,
    passwordDialogVisible,
    passwordSubmitting,
    cancellationDialogVisible,
    cancelling,
    cancellationForm,
    lastLogin,
    pwdForm,
    resetForm,
    openPasswordDialog,
    submitChangePassword,
    logout,
    resetCancellationForm,
    openCancellationDialog,
    submitCancellation,
  }
}
