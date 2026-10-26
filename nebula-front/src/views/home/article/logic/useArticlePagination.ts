export interface Props {
  currentPage: number
  pageSize: number
  total: number
}

/** 管理文章分页事件的状态与交互。 */
export function useArticlePagination(emit: { (e: 'update:currentPage', value: number): void }) {
  const handleChange = (page: number) => {
    emit('update:currentPage', page)
  }

  return { handleChange }
}
