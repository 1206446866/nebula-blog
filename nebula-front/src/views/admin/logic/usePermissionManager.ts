import {
  createPermissionApi,
  type CreatePermissionDto,
  getPermissionPage,
  type PermissionVO,
  removePermissionApi,
  updatePermissionApi,
  type UpdatePermissionDto,
} from '@/api/permission.ts'
import type { TableColumn } from '@/components/basic/table'
import { useTable } from '@/composables/useTable.ts'
import { formatDateTime } from '@/utils/date'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reactive, ref } from 'vue'

/** 处理权限管理页面的状态与业务交互。 */
export function usePermissionManager() {
  const nameFilter = ref('')
  const dialogVisible = ref(false)
  const form = reactive<Partial<PermissionVO>>({
    id: undefined,
    name: '',
    description: '',
  })
  const columns: TableColumn<PermissionVO>[] = [
    { prop: 'id', label: 'ID' },
    { prop: 'name', label: '权限编码' },
    { prop: 'description', label: '描述' },
    {
      prop: 'createTime',
      label: '创建时间',
      width: 180,
      formatter: (row: PermissionVO) => formatDateTime(row.createTime) || '—',
    },
    { slot: 'actions', label: '操作', width: 160 },
  ]
  const {
    list: permissions,
    loading,
    error: tableError,
    current,
    size,
    total,
    reload: reloadTable,
    search: onSearch,
    changePage: onPageChange,
    changeSize: onSizeChange,
  } = useTable<PermissionVO>(
    ({ current, size, signal }) =>
      getPermissionPage({ name: nameFilter.value, current, size }, signal),
    { watch: [nameFilter] },
  )
  const openCreate = () => {
    Object.assign(form, { id: undefined, name: '', description: '' })
    dialogVisible.value = true
  }
  const openEdit = (row: PermissionVO) => {
    Object.assign(form, row)
    dialogVisible.value = true
  }
  const remove = async (row: PermissionVO) => {
    await ElMessageBox.confirm(`确定删除权限【${row.name}】吗？`, '提示', {
      type: 'warning',
    })
    await removePermissionApi(row.id)
    ElMessage.success('删除成功')
    await reloadTable()
  }
  const submitForm = async () => {
    if (!form.name) {
      ElMessage.warning('请输入权限代号')
      return
    }
    if (form.id) {
      await updatePermissionApi(<UpdatePermissionDto>form)
      ElMessage.success('更新成功')
    } else {
      await createPermissionApi(<CreatePermissionDto>form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await reloadTable()
  }

  return {
    nameFilter,
    dialogVisible,
    form,
    columns,
    permissions,
    loading,
    tableError,
    current,
    size,
    total,
    reloadTable,
    onSearch,
    onPageChange,
    onSizeChange,
    openCreate,
    openEdit,
    remove,
    submitForm,
  }
}
