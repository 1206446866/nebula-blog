import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import { mediaPath } from './resource.ts'

const markdown = new MarkdownIt({
  html: false,
  highlight(code, language) {
    if (language && hljs.getLanguage(language)) {
      try {
        return hljs.highlight(code, { language, ignoreIllegals: true }).value
      } catch {
        // 代码高亮无法处理时，交由 Markdown-it 转义代码。
      }
    }
    return ''
  },
})

const renderImage = markdown.renderer.rules.image
markdown.renderer.rules.image = (tokens, index, options, env, renderer) => {
  const token = tokens[index]
  const path = mediaPath(token?.attrGet('src') || '')
  if (path?.startsWith('upload/article/')) {
    if (token?.attrs) token.attrs = token.attrs.filter(([name]) => name !== 'src')
    token?.attrSet('data-media-path', path)
  }
  return renderImage ? renderImage(tokens, index, options, env, renderer) : ''
}

// 保留 Markdown-it 对代码块属性的转义，包括语言名称。
const renderFence = markdown.renderer.rules.fence
markdown.renderer.rules.fence = (tokens, index, options, env, renderer) => {
  tokens[index]?.attrJoin('class', 'hljs')
  return renderFence ? renderFence(tokens, index, options, env, renderer) : ''
}

// 保留 Markdown 表格对齐方式，同时禁止任意行内样式。
for (const rule of ['th_open', 'td_open']) {
  markdown.renderer.rules[rule] = (tokens, index, options, _env, renderer) => {
    const token = tokens[index]
    const alignment = token?.attrGet('style')?.match(/^text-align:(left|center|right)$/)?.[1]
    if (token?.attrs) token.attrs = token.attrs.filter(([name]) => name !== 'style')
    if (alignment) token?.attrSet('align', alignment)
    return renderer.renderToken(tokens, index, options)
  }
}

/** 文章正文与编辑器预览统一使用此 HTML 渲染入口。 */
export function renderMarkdown(source: string | null | undefined): string {
  const content = source ?? ''
  if (!DOMPurify.isSupported) {
    return `<pre>${markdown.utils.escapeHtml(content)}</pre>`
  }
  // 最后执行安全过滤，调用方不得在此之后拼接或转换 HTML。
  return DOMPurify.sanitize(markdown.render(content), {
    ALLOWED_TAGS: [
      'p',
      'br',
      'hr',
      'h1',
      'h2',
      'h3',
      'h4',
      'h5',
      'h6',
      'strong',
      'em',
      's',
      'del',
      'blockquote',
      'ul',
      'ol',
      'li',
      'table',
      'thead',
      'tbody',
      'tr',
      'th',
      'td',
      'pre',
      'code',
      'span',
      'a',
      'img',
    ],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'start', 'align', 'data-media-path'],
    ALLOW_DATA_ATTR: false,
    ALLOW_ARIA_ATTR: false,
    RETURN_TRUSTED_TYPE: false,
  })
}
