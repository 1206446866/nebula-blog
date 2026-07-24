import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAvatarApi } from '@/api/user'
import { useAuthStore } from '../stores/auth'

export function useUploadAvatar() {
  const uploading = ref(false)

  const userStore = useAuthStore()

  const uploadAvatar = async (file: File) => {
    if (!file) {
      return
    }

    if (!file.type.startsWith('image/')) {
      ElMessage.error('只能上传图片')
      return
    }

    if (file.size > 10 * 1024 * 1024) {
      ElMessage.error('图片不能超过10MB')
      return
    }

    uploading.value = true

    try {
      const res = await uploadAvatarApi(file)

      if (res.code === 200) {
        // userStore.setUser({
        //   ...userStore.user,
        //   avatar: res.data,
        // })
        userStore.updateAvatar(res.data)

        ElMessage.success('头像上传成功')

        return res.data
      }

      ElMessage.error(res.message)
    } finally {
      uploading.value = false
    }
  }

  return {
    uploading,
    uploadAvatar,
  }
}
