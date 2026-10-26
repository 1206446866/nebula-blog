export function positiveId(value: unknown): value is string {
  return (
    typeof value === 'string' && /^[1-9]\d*$/.test(value) && Number.isSafeInteger(Number(value))
  )
}

export function readArticleQuery(query: Record<string, unknown>) {
  const ids = (...values: unknown[]) => {
    const parts = values
      .flatMap((value) => (value == null ? [] : Array.isArray(value) ? value : [value]))
      .flatMap((value) => (typeof value === 'string' ? value.split(',') : [value]))
    if (parts.length > 100 || parts.some((value) => !positiveId(value)))
      throw new Error('Invalid article filter')
    return [...new Set(parts.map(Number))]
  }
  const rawPage = query.page ?? '1'
  if (!positiveId(rawPage) || Number(rawPage) > 2147483647) throw new Error('Invalid article page')
  return {
    categoryIds: ids(query.categoryId, query.categoryIds),
    tagIds: ids(query.tagId, query.tagIds),
    page: Number(rawPage),
  }
}

export function articleFilterQuery(categoryIds: number[], tagIds: number[]) {
  return {
    categoryId: undefined,
    tagId: undefined,
    categoryIds: categoryIds.length ? categoryIds.join(',') : undefined,
    tagIds: tagIds.length ? tagIds.join(',') : undefined,
    page: undefined,
  }
}
