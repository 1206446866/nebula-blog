import type { CreateTagDTO, TagVO, UpdateTagDTO } from '@/api/tag'
import { createTagApi, deleteTagApi, pageTagApi, updateTagApi } from '@/api/tag'
import type { TableColumn } from '@/components/basic/table'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/date'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reactive, ref } from 'vue'

/** 处理标签管理页面的状态与业务交互。 */
export function useTagManager() {
  /**
   * 搜索
   */
  const nameFilter = ref('')

  /**
   * 弹窗
   */
  const dialogVisible = ref(false)
  const dialogTitle = ref('')

  /**
   * 表单
   */
  const tagForm = reactive<Partial<TagVO & CreateTagDTO>>({
    id: undefined,
    name: '',
    description: '',
  })
  const {
    list: tags,
    loading,
    error: tableError,
    current,
    size,
    total,
    reload: reloadTable,
    search: onSearch,
    changePage: onPageChange,
    changeSize: onSizeChange,
  } = useTable<TagVO>(
    ({ current, size, signal }) => pageTagApi(nameFilter.value, current, size, signal),
    { watch: [nameFilter] },
  )

  /**
   * 列定义
   */
  const columns: TableColumn<TagVO>[] = [
    { label: 'ID', prop: 'id', width: 80 },
    { label: '名称', prop: 'name' },
    { label: '描述', prop: 'description' },
    {
      label: '创建时间',
      prop: 'createTime',
      width: 180,
      formatter: (row: TagVO) => formatDateTime(row.createTime) || '—',
    },
    { label: '操作', slot: 'actions', width: 160 },
  ]

  /**
   * 新增
   */
  const openCreate = () => {
    dialogTitle.value = '新增标签'
    Object.assign(tagForm, {
      id: undefined,
      name: '',
      description: '',
    })
    dialogVisible.value = true
  }

  /**
   * 编辑
   */
  const openEdit = (row: TagVO) => {
    dialogTitle.value = '编辑标签'
    Object.assign(tagForm, row)
    dialogVisible.value = true
  }

  /**
   * 保存
   */
  const saveTag = async () => {
    if (!tagForm.name?.trim()) {
      ElMessage.warning('请输入标签名称')
      return
    }

    let res

    if (tagForm.id) {
      const dto: UpdateTagDTO = {
        id: tagForm.id,
        name: tagForm.name!,
        description: tagForm.description,
      }
      res = await updateTagApi(dto)
    } else {
      const dto: CreateTagDTO = {
        name: tagForm.name!,
        description: tagForm.description,
      }
      res = await createTagApi(dto)
    }

    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      reloadTable()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  }

  /**
   * 删除
   */
  const deleteTag = async (row: TagVO) => {
    await ElMessageBox.confirm(`确认删除标签【${row.name}】吗？`, '提示', { type: 'warning' })
    const res = await deleteTagApi(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      reloadTable()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  }

  return {
    nameFilter,
    dialogVisible,
    dialogTitle,
    tagForm,
    tags,
    loading,
    tableError,
    current,
    size,
    total,
    reloadTable,
    onSearch,
    onPageChange,
    onSizeChange,
    columns,
    openCreate,
    openEdit,
    saveTag,
    deleteTag,
  }
}
