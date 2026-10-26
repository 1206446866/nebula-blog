export interface MarkdownEdit {
  content: string
  selectionStart: number
  selectionEnd: number
}

/** 按完整行设置标题，替换已有的井号标题标记，避免重复叠加。 */
export function setMarkdownHeading(
  source: string,
  start: number,
  end: number,
  level: 1 | 2,
): MarkdownEdit {
  const lineStart = source.slice(0, start).lastIndexOf('\n') + 1
  const lastPosition = end > start && source[end - 1] === '\n' ? end - 1 : end
  const nextBreak = source.indexOf('\n', lastPosition)
  const lineEnd = nextBreak < 0 ? source.length : nextBreak
  const lines = source.slice(lineStart, lineEnd).split('\n')
  const prefix = '#'.repeat(level) + ' '
  const oldPrefix = /^ {0,3}#{1,6}(?:[\t ]+|$)/
  const placeholder = level === 1 ? '一级标题' : '二级标题'
  const emptyLine = lines.length === 1 && !lines[0]!.trim()
  const replacement = emptyLine
    ? prefix + placeholder
    : lines.map((line) => (line.trim() ? prefix + line.replace(oldPrefix, '') : line)).join('\n')
  const caret =
    lineStart +
    prefix.length +
    Math.max(0, start - lineStart - (lines[0]!.match(oldPrefix)?.[0].length || 0))
  return {
    content: source.slice(0, lineStart) + replacement + source.slice(lineEnd),
    selectionStart: emptyLine || start !== end ? lineStart + prefix.length : caret,
    selectionEnd: emptyLine || start !== end ? lineStart + replacement.length : caret,
  }
}

function longestBacktickRun(source: string): number {
  let longest = 0
  for (const match of source.matchAll(/`+/g)) longest = Math.max(longest, match[0].length)
  return longest
}

/** 选择比代码中连续反引号更长的分隔符，保留 JavaScript 模板和 Markdown 示例。 */
export function insertMarkdownCode(
  source: string,
  start: number,
  end: number,
  block: boolean,
): MarkdownEdit {
  const selection = source.slice(start, end) || (block ? '代码' : '行内代码')
  const fence = '`'.repeat(Math.max(block ? 3 : 1, longestBacktickRun(selection) + 1))
  const padding = !block && /^`|`$/.test(selection) ? ' ' : ''
  const prefix = block ? `\n${fence}\n` : fence + padding
  const suffix = block ? `\n${fence}\n` : padding + fence
  return {
    content: source.slice(0, start) + prefix + selection + suffix + source.slice(end),
    selectionStart: start + prefix.length,
    selectionEnd: start + prefix.length + selection.length,
  }
}
