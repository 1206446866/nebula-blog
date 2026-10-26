import {
  type CategoryVO,
  createCategoryApi,
  type CreateCategoryDTO,
  deleteCategoryApi,
  pageCategoryApi,
  updateCategoryApi,
} from '@/api/category'
import type { TableColumn } from '@/components/basic/table'
import { useTable } from '@/composables/useTable.ts'
import { formatDateTime } from '@/utils/date'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref } from 'vue'

/** 处理分类管理页面的状态与业务交互。 */
export function useCategoryManager() {
  const keyword = ref('')
  const columns: TableColumn<CategoryVO>[] = [
    {
      prop: 'id',
      label: 'ID',
      width: 80,
    },
    {
      prop: 'name',
      label: '分类名称',
    },
    {
      prop: 'description',
      label: '分类描述',
    },
    {
      prop: 'createTime',
      label: '创建时间',
      width: 200,
      formatter: (row: CategoryVO) => formatDateTime(row.createTime) || '—',
    },
    {
      label: '操作',
      width: 120,
      slot: 'action',
    },
  ]
  const dialogVisible = ref(false)
  const createForm = ref<CreateCategoryDTO>({
    name: '',
    description: '',
  })
  const {
    list: categoryList,
    loading,
    error: tableError,
    current,
    size,
    total,
    reload: reloadTable,
    search: handleSearch,
    changePage: handlePageChange,
    changeSize: handleSizeChange,
  } = useTable<CategoryVO>(
    ({ current, size, signal }) =>
      pageCategoryApi({ name: keyword.value, currentPage: current, size }, signal),
    { watch: [keyword] },
  )

  /**
   * 新增分类
   */
  const handleCreate = () => {
    createForm.value = {
      name: '',
      description: '',
    }
    dialogVisible.value = true
  }
  const handleSubmit = async () => {
    const res = await createCategoryApi(createForm.value)
    if (res.code === 200) {
      ElMessage.success('新增成功')
      dialogVisible.value = false
      await reloadTable()
    } else {
      ElMessage.error(res.message)
    }
  }

  /**
   * 编辑分类
   */
  const handleEdit = async (row: CategoryVO) => {
    const res = await updateCategoryApi({
      id: row.id,
      name: row.name,
      description: row.description,
    })
    if (res.code === 200) {
      ElMessage.success('改动成功')
    }
  }
  const handleDelete = async (row: CategoryVO) => {
    try {
      await ElMessageBox.confirm(`确认删除分类【${row.name}】吗？`, '删除确认', {
        type: 'warning',
      })
      const res = await deleteCategoryApi(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        await reloadTable()
      } else {
        ElMessage.error(res.message)
      }
    } catch {
      //
    }
  }

  return {
    keyword,
    columns,
    dialogVisible,
    createForm,
    categoryList,
    loading,
    tableError,
    current,
    size,
    total,
    reloadTable,
    handleSearch,
    handlePageChange,
    handleSizeChange,
    handleCreate,
    handleSubmit,
    handleEdit,
    handleDelete,
  }
}
