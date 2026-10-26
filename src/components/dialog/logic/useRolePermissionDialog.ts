import {
  getPermissionsApi,
  getRolePermissionsApi,
  type PermissionGroupMap,
  type PermissionVO,
  saveRolePermissionsApi,
} from '@/api/permission.ts'
import { type RoleVO } from '@/api/role.ts'
import { Role } from '@/constants/role'
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

/** 管理角色权限分配的状态与业务交互。 */
export function useRolePermissionDialog() {
  const visible = ref(false)
  const submitting = ref(false)
  const isSuperAdminRole = computed(() => role.value.name === Role.SUPER_ADMIN)
  const role = ref<RoleVO>({
    id: 0,
    name: '',
    description: '',
    createTime: '',
  })

  /** 后端数据 */
  const grouped = ref<Record<string, PermissionVO[]>>({})

  /** 界面模块 */
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
    grouped.value = normalizeGroups(all?.data)
    const baseMap = buildCheckedMap(
      isSuperAdminRole.value ? grouped.value : normalizeGroups(rolePerms.data),
    )
    checkedMap.value = Object.fromEntries(
      Object.keys(grouped.value).map((k) => [k, baseMap[k] || []]),
    )
    currentModule.value =
      modules.find((module) => grouped.value[module.key]?.length)?.key || 'other'
    visible.value = true
  }

  /** 提交权限分配 */
  const submit = async () => {
    if (isSuperAdminRole.value || submitting.value) return
    submitting.value = true
    try {
      const all = [...new Set(Object.values(checkedMap.value).flat())]
      await saveRolePermissionsApi({
        roleId: role.value.id,
        permissionIds: all,
      })
      ElMessage.success('授权成功')
      visible.value = false
    } finally {
      submitting.value = false
    }
  }
  const buildCheckedMap = (grouped?: PermissionGroupMap) => {
    const map: Record<string, number[]> = {}
    if (!grouped) return map
    for (const key in grouped) {
      map[key] = (grouped[key] || []).map((item) => item.id)
    }
    return map
  }

  /** 未识别的后端分组归入“其它”，只调整展示位置，保留原始权限标识和ID。 */
  const normalizeGroups = (source?: PermissionGroupMap): PermissionGroupMap => {
    const result: PermissionGroupMap = Object.fromEntries(modules.map((module) => [module.key, []]))
    const knownGroups = new Set(modules.map((module) => module.key))
    const seen = new Set<number>()
    for (const [key, permissions] of Object.entries(source ?? {})) {
      const target = knownGroups.has(key) ? key : 'other'
      for (const permission of permissions ?? []) {
        if (seen.has(permission.id)) continue
        seen.add(permission.id)
        result[target]!.push(permission)
      }
    }
    return result
  }

  return {
    visible,
    submitting,
    isSuperAdminRole,
    role,
    modules,
    currentModule,
    checkedMap,
    currentPermissions,
    currentModuleName,
    open,
    submit,
  }
}
