// src/types/page.ts

export interface Page<T> {
  records: T[]
  totalRow: number
  pageNumber: number
  pageSize: number
  totalPage: number
}
