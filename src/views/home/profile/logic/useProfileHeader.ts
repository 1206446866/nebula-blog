import type { UserProfileVO } from '@/api/profile.ts'
import { updateSelfNameApi } from '@/api/user.ts'
import { useUploadAvatar } from '@/composables/useUploadAvatar'
import { UserStatus } from '@/enum/userStatus.js'
import { useAuthStore } from '@/stores/auth.ts'
import { avatarUrl as resourceAvatarUrl } from '@/utils/resource'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'

/** 管理个人头像与昵称的状态与交互。 */
export function useProfileHeader(
  props: Readonly<{
    profile: UserProfileVO | null
    isSelf: boolean
  }>,
  emit: {
    (e: 'refresh'): void
  },
) {
  const username = computed(() => props.profile?.userProfileInfoVO.username ?? '')
  const avatarText = computed(() => username.value?.charAt(0) ?? 'U')
  const roles = computed(() => props.profile?.userProfileInfoVO.roles ?? [])
  const statusText = computed(() => {
    return props.profile?.userProfileInfoVO.status === UserStatus.NORMAL ? 'Active' : 'Disabled'
  })
  const statusColor = computed(() => {
    return props.profile?.userProfileInfoVO.status === UserStatus.NORMAL
      ? '#34d399' // 绿
      : '#ef4444' // 红
  })
  const avatarUrl = computed(() => {
    const avatar = props.profile?.userProfileInfoVO.avatar
    if (!avatar) return ''
    return resourceAvatarUrl(avatar)
  })
  const { uploadAvatar, uploading } = useUploadAvatar()
  const userStore = useAuthStore()
  const fileInput = ref<HTMLInputElement>()
  const editing = ref(false)
  const tempUsername = ref('')
  const nameLoading = ref(false)

  watch(
    () => props.profile?.userProfileInfoVO.id,
    () => {
      cancelEdit()
    },
  )
  const updateAvatar = () => {
    fileInput.value?.click()
  }
  const handleFileChange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return
    try {
      const avatar = await uploadAvatar(file)
      if (avatar) {
        emit('refresh')
      }
    } catch {
      ElMessage.error('头像上传失败，请重试')
    } finally {
      // 上传失败后也清空文件选择器，以便再次选择同一文件。
      ;(e.target as HTMLInputElement).value = ''
    }
  }
  const startEdit = () => {
    tempUsername.value = username.value
    editing.value = true
  }
  const cancelEdit = () => {
    editing.value = false
    tempUsername.value = ''
  }
  const changeName = async () => {
    if (nameLoading.value) return
    const nextName = tempUsername.value.trim()
    if (nextName.length < 3 || nextName.length > 20) {
      ElMessage.warning('昵称长度应为3到20个字符')
      return
    }
    nameLoading.value = true
    const token = userStore.getToken()
    try {
      const res = await updateSelfNameApi(nextName)
      if (userStore.getToken() !== token) return

      if (res.code === 200 && res.data) {
        ElMessage.success('修改成功')

        const currentUser = userStore.user
        if (currentUser && currentUser.id === props.profile?.userProfileInfoVO.id) {
          userStore.setUser({ ...currentUser, username: nextName })
        }
        editing.value = false
        emit('refresh')
      }
    } catch {
      // 业务错误已由请求拦截器统一显示。
    } finally {
      nameLoading.value = false
    }
  }

  return {
    username,
    avatarText,
    roles,
    statusText,
    statusColor,
    avatarUrl,
    uploading,
    fileInput,
    editing,
    tempUsername,
    nameLoading,
    updateAvatar,
    handleFileChange,
    startEdit,
    cancelEdit,
    changeName,
  }
}
