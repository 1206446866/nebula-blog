<template>
  <el-dialog v-model="visible" title="权限分配" width="700px" class="perm-dialog">
    <!-- 角色信息 -->
    <el-card class="info-card">
      <div class="title">角色信息</div>
      <div class="row">角色：{{ role.name }}</div>
      <div class="row">描述：{{ role.description }}</div>
    </el-card>

    <!-- 模块切换 -->
    <div class="tabs">
      <el-button
        v-for="m in modules"
        :key="m.key"
        :type="currentModule === m.key ? 'primary' : 'default'"
        @click="currentModule = m.key"
      >
        {{ m.name }}
      </el-button>
    </div>

    <!-- 权限面板 -->
    <el-card class="perm-card">
      <div class="title">{{ currentModuleName }} 权限</div>
      <el-checkbox-group v-model="checkedMap[currentModule]" class="perm-group">
        <el-checkbox v-for="p in currentPermissions" :key="p.id" :value="p.id" class="perm-item">
          {{ p.description }}
        </el-checkbox>
      </el-checkbox-group>
    </el-card>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { type RoleVO } from '@/api/role.ts'
import {
  getPermissionsApi,
  getRolePermissionsApi,
  type PermissionGroupMap,
  type PermissionVO,
  saveRolePermissionsApi,
} from '@/api/permission.ts'
import { ElMessage } from 'element-plus'

const visible = ref(false)

const role = ref<RoleVO>({
  id: 0,
  name: '',
  description: '',
  createTime: '',
})

/** 后端数据 */
const grouped = ref<Record<string, PermissionVO[]>>({})

/** UI模块 */
const modules = [
  { key: 'user', name: '用户权限' },
  { key: 'article', name: '文章权限' },
  { key: 'category', name: '分类权限' },
  { key: 'tag', name: '标签权限' },
  { key: 'comment', name: '评论权限' },
  { key: 'other', name: '其它' },
]

const currentModule = ref('user')

const checkedMap = ref<Record<string, number[]>>({})

/** 当前权限 */
const currentPermissions = computed(() => {
  return grouped.value?.[currentModule.value] ?? []
})

const currentModuleName = computed(() => {
  return modules.find((m) => m.key === currentModule.value)?.name || 'user'
})

const open = async (roleData: RoleVO) => {
  role.value = roleData
  const [all, rolePerms] = await Promise.all([
    getPermissionsApi(),
    getRolePermissionsApi(roleData.id),
  ])
  grouped.value = all?.data ?? {}
  const baseMap = buildCheckedMap(rolePerms.data)
  checkedMap.value = Object.fromEntries(
    Object.keys(grouped.value).map((k) => [k, baseMap[k] || []]),
  )
  currentModule.value = grouped.value['user'] ? 'user' : Object.keys(grouped.value)[0] || 'other'
  visible.value = true
}
/** submit */
const submit = async () => {
  const all = [...new Set(Object.values(checkedMap.value).flat())]
  await saveRolePermissionsApi({
    roleId: role.value.id,
    permissionIds: all,
  })
  ElMessage.success('授权成功')
  visible.value = false
}

const buildCheckedMap = (grouped?: PermissionGroupMap) => {
  const map: Record<string, number[]> = {}
  if (!grouped) return map
  for (const key in grouped) {
    map[key] = (grouped[key] || []).map((item) => item.id)
  }
  return map
}

defineExpose({ open })
</script>

<style scoped>
.perm-dialog :deep(.el-dialog) {
  background: #0f172a;
  border-radius: 10px;
}

.info-card,
.perm-card {
  margin-bottom: 12px;
  background: #111827 !important;
  border: 1px solid #1f2937 !important;
  color: #e5e7eb;
}

.tabs {
  display: flex;
  gap: 10px;
  margin: 12px 0;
}

.perm-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.perm-item {
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 6px;
}

:deep(.el-checkbox__label) {
  color: #e5e7eb;
}

.title {
  font-weight: bold;
  margin-bottom: 10px;
}

.row {
  color: #9ca3af;
  font-size: 13px;
}
</style>
