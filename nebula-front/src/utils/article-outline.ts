export interface ArticleHeading {
  id: string
  title: string
  number: string
  level: 1 | 2
  element: HTMLHeadingElement
}

/** 读取实际渲染出的标题，避免把代码块或转义后的 Markdown 误当成目录项。 */
export function buildArticleOutline(body: HTMLElement): ArticleHeading[] {
  let section = 0
  let subsection = 0
  let hasParent = false
  return [...body.querySelectorAll<HTMLHeadingElement>('h1, h2')].flatMap((element, index) => {
    const title = element.textContent?.trim()
    if (!title) return []
    // 开头的二级标题没有一级标题父项时，直接作为顶层目录项显示，避免生成“0.1”编号。
    const level = element.tagName === 'H1' || !hasParent ? 1 : 2
    if (element.tagName === 'H1') {
      section++
      subsection = 0
      hasParent = true
    } else if (level === 1) section++
    else subsection++
    const id = `article-section-${index + 1}`
    element.id = id
    return [
      {
        id,
        title,
        number: level === 1 ? String(section) : `${section}.${subsection}`,
        level,
        element,
      } satisfies ArticleHeading,
    ]
  })
}
