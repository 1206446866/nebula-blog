import {
  createRole,
  type CreateRoleDto,
  getRolePageApi,
  removeRole,
  type RoleVO,
  updateRole,
  type UpdateRoleDto,
} from '@/api/role'
import type { TableColumn } from '@/components/basic/table'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/date'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reactive, ref } from 'vue'

/** 处理角色管理页面的状态与业务交互。 */
export function useRoleManager() {
  /** 筛选条件 */
  const descriptionFilter = ref('')

  /** 编辑弹窗 */
  const dialogVisible = ref(false)

  /** 编辑表单 */
  const form = reactive<Partial<RoleVO>>({
    id: undefined,
    name: '',
    description: '',
  })

  /** 用户管理风格的表格列配置 */
  const columns: TableColumn<RoleVO>[] = [
    { label: 'ID', prop: 'id', width: 80 },
    { label: '角色名称', prop: 'name' },
    { label: '描述', prop: 'description' },
    {
      label: '创建时间',
      prop: 'createTime',
      width: 180,
      formatter: (row: RoleVO) => formatDateTime(row.createTime) || '—',
    },
    { label: '操作', slot: 'actions', width: 300 },
  ]
  const {
    list: roles,
    loading,
    error: tableError,
    current,
    size,
    total,
    reload: reloadTable,
    search: onSearch,
    changePage: onPageChange,
    changeSize: onSizeChange,
  } = useTable<RoleVO>(
    ({ current, size, signal }) =>
      getRolePageApi({ description: descriptionFilter.value, current, size }, signal),
    { watch: [descriptionFilter] },
  )
  const permDialogRef = ref<{ open: (role: RoleVO) => Promise<void> }>()

  /** 新增 */
  const openCreate = () => {
    Object.assign(form, { id: undefined, name: '', description: '' })
    dialogVisible.value = true
  }

  /** 编辑 */
  const openEdit = (row: RoleVO) => {
    Object.assign(form, row)
    dialogVisible.value = true
  }

  /** 提交 */
  const submitForm = async () => {
    if (!form.name) {
      ElMessage.warning('请输入角色名称')
      return
    }
    if (form.id) {
      await updateRole(<UpdateRoleDto>form)
      ElMessage.success('更新成功')
    } else {
      await createRole(<CreateRoleDto>form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await reloadTable()
  }

  /** 删除角色 */
  const deleteRole = async (row: RoleVO) => {
    await ElMessageBox.confirm(`确定删除角色【${row.name}】吗？`, '提示', {
      type: 'warning',
    })
    await removeRole(row.id)
    ElMessage.success('删除成功')
    await reloadTable()
  }
  const openPermission = async (row: RoleVO) => {
    await permDialogRef.value?.open(row)
  }

  return {
    descriptionFilter,
    dialogVisible,
    form,
    columns,
    roles,
    loading,
    tableError,
    current,
    size,
    total,
    reloadTable,
    onSearch,
    onPageChange,
    onSizeChange,
    permDialogRef,
    openCreate,
    openEdit,
    submitForm,
    deleteRole,
    openPermission,
  }
}
