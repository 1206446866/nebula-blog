import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'

const markdown = new MarkdownIt({
  html: false,
  highlight(code, language) {
    if (language && hljs.getLanguage(language)) {
      try {
        return hljs.highlight(code, { language, ignoreIllegals: true }).value
      } catch {
        // Let Markdown-it escape code when highlighting cannot handle it.
      }
    }
    return ''
  },
})

// Keep Markdown-it's escaping of fence attributes, including the language name.
const renderFence = markdown.renderer.rules.fence
markdown.renderer.rules.fence = (tokens, index, options, env, renderer) => {
  tokens[index]?.attrJoin('class', 'hljs')
  return renderFence ? renderFence(tokens, index, options, env, renderer) : ''
}

// Preserve Markdown table alignment without allowing arbitrary inline CSS.
for (const rule of ['th_open', 'td_open']) {
  markdown.renderer.rules[rule] = (tokens, index, options, _env, renderer) => {
    const token = tokens[index]
    const alignment = token?.attrGet('style')?.match(/^text-align:(left|center|right)$/)?.[1]
    if (token?.attrs) token.attrs = token.attrs.filter(([name]) => name !== 'style')
    if (alignment) token?.attrSet('align', alignment)
    return renderer.renderToken(tokens, index, options)
  }
}

/** The only HTML renderer used by article bodies and editor previews. */
export function renderMarkdown(source: string | null | undefined): string {
  const content = source ?? ''
  if (!DOMPurify.isSupported) {
    return `<pre>${markdown.utils.escapeHtml(content)}</pre>`
  }
  // Sanitize last; callers must not append or transform HTML after this boundary.
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
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'start', 'align'],
    ALLOW_DATA_ATTR: false,
    ALLOW_ARIA_ATTR: false,
    RETURN_TRUSTED_TYPE: false,
  })
}
