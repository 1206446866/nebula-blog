<template>
  <el-dialog :model-value="true" title="登录设备" width="min(540px, 92vw)" @close="emit('close')">
    <p v-if="loading" role="status">正在加载登录设备…</p>
    <div v-else-if="failed" role="alert">
      登录设备加载失败。
      <el-button link type="primary" @click="load">重新加载</el-button>
    </div>
    <p v-else-if="!sessions.length">暂无设备记录。升级前的登录不会出现在此列表。</p>
    <ul v-else class="device-list">
      <li v-for="session in sessions" :key="session.id" class="device-row">
        <div>
          <strong>{{ session.browser }} · {{ session.os }}</strong>
          <div class="device-time">登录于 {{ formatDateTime(session.signedInAt) }}</div>
        </div>
        <span v-if="session.current">当前会话</span>
        <el-button v-else size="small" :disabled="!!revoking" @click="revoke(session)">
          {{ revoking === session.id ? '正在撤销…' : '退出此设备' }}
        </el-button>
      </li>
    </ul>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDeviceSessionsApi, revokeDeviceSessionApi, type DeviceSessionVO } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/date'

const emit = defineEmits<{ close: [] }>()
const auth = useAuthStore()
const openingToken = auth.getToken()
const controller = new AbortController()
const sessions = ref<DeviceSessionVO[]>([])
const loading = ref(false)
const failed = ref(false)
const revoking = ref('')

function stillCurrent() {
  return !controller.signal.aborted && auth.getToken() === openingToken
}

async function load() {
  if (loading.value || !stillCurrent()) return
  loading.value = true
  failed.value = false
  try {
    const result = await listDeviceSessionsApi(controller.signal)
    if (stillCurrent()) sessions.value = result.data
  } catch {
    if (stillCurrent()) failed.value = true
  } finally {
    loading.value = false
  }
}

async function revoke(session: DeviceSessionVO) {
  if (revoking.value || !stillCurrent()) return
  revoking.value = session.id
  try {
    await ElMessageBox.confirm('此设备需要重新登录。', '退出此设备', {
      confirmButtonText: '退出此设备',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (!stillCurrent()) return
    const result = await revokeDeviceSessionApi(session.id)
    if (!stillCurrent()) return
    sessions.value = sessions.value.filter((item) => item.id !== session.id)
    ElMessage.success(result.data ? '设备已退出' : '该会话已失效')
  } catch (error) {
    if (stillCurrent() && error !== 'cancel' && error !== 'close') ElMessage.error('撤销失败，请重试')
  } finally {
    revoking.value = ''
  }
}

watch(() => auth.getToken(), () => emit('close'))
onMounted(load)
onUnmounted(() => controller.abort())
</script>

<style scoped>
.device-list { margin: 0; padding: 0; list-style: none; }
.device-row { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; padding: 12px 0; }
.device-row + .device-row { border-top: 1px solid var(--el-border-color-lighter); }
.device-time { margin-top: 4px; color: var(--el-text-color-secondary); }
</style>
