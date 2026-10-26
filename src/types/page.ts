// 分页响应结构。

export interface Page<T> {
  records: T[]
  totalRow: number
  pageNumber: number
  pageSize: number
  totalPage: number
}
