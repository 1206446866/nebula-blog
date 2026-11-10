/** 下载文件名只保留普通名称，拒绝路径和Windows保留名称。 */
export function markdownFilename(title: string, extension = 'md') {
  let name =
    Array.from(
      title
        .replace(/[\u0000-\u001f\u007f\\/:*?"<>|]/g, '_')
        .trim()
        .replace(/[. ]+$/g, ''),
    )
      .slice(0, 80)
      .join('') || 'nebula-article'
  if (/^(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)/i.test(name)) name = `nebula-${name}`
  return `${name}.${extension === 'zip' ? 'zip' : 'md'}`
}

/** 与服务端导出格式一致，首行标题可在再次导入时单独还原。 */
export function exportMarkdown(title: string, content: string) {
  return `# ${title.replace(/[\r\n]/g, ' ')}\n\n${content}`
}

/** 只提取首行一级标题，不执行HTML或YAML，不自动保存和发布。 */
export function importMarkdown(text: string, filename: string) {
  const normalized = text.replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n')
  if (normalized.length > 200000 || normalized.includes('\0'))
    throw new Error('正文最多20万字，且不能包含二进制内容')
  const heading = /^# ([^\n]+)\n(?:\n)?/.exec(normalized)
  const title = (
    heading?.[1]?.trim() ||
    filename.replace(/\.(md|markdown)$/i, '').trim() ||
    '未命名文章'
  ).slice(0, 255)
  return { title, content: heading ? normalized.slice(heading[0].length) : normalized }
}

/** 下载链接仅在触发时创建，延迟释放对象地址，兼容浏览器保存对话框。 */
export function downloadMarkdownBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob),
    anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.hidden = true
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  setTimeout(() => URL.revokeObjectURL(url), 30000)
}
