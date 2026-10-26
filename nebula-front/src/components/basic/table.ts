import type { TableColumnCtx } from 'element-plus'

/** 表格字段限定为当前行的属性；业务插槽及格式化函数保留相同行类型。 */
export type TableColumn<Row extends object> = Partial<
  Omit<TableColumnCtx<Row>, 'prop' | 'property' | 'label'>
> & {
  label: string
  prop?: Extract<keyof Row, string>
  slot?: string
}
